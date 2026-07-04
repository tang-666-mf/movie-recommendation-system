package com.recommend.als

import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.hadoop.hbase.client.{ConnectionFactory, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

/**
 * 阶段5: 个性化电影推荐系统
 * 使用Spark MLlib ALS算法训练协同过滤模型
 * 为所有用户预测最喜欢的3部电影，写入HBase
 *
 * 提交命令:
 * spark-submit --master yarn --deploy-mode client \
 *   --class com.recommend.als.MovieRecommendationALS \
 *   MovieRecommendationALS.jar
 */
object MovieRecommendationALS {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("MovieRecommendationALS")
      .config("spark.sql.shuffle.partitions", "4")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    // 1. 加载评分数据
    println("=" * 60)
    println("阶段5: 个性化电影推荐系统 - ALS协同过滤")
    println("=" * 60)

    val ratingsDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("hdfs:///user/movie_data/ratings.csv")

    // 加载电影数据用于结果映射
    val moviesDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("hdfs:///user/movie_data/movies.csv")

    println("\n>>> 评分数据预览:")
    ratingsDF.show(10)

    println("\n>>> 电影数据预览:")
    moviesDF.show(10)

    println(s"\n>>> 评分总数: ${ratingsDF.count()}")
    println(s">>> 用户总数: ${ratingsDF.select("userId").distinct().count()}")
    println(s">>> 电影总数: ${ratingsDF.select("movieId").distinct().count()}")

    // 2. 划分训练集和测试集
    val Array(training, test) = ratingsDF.randomSplit(Array(0.8, 0.2), seed = 42L)

    println(s"\n>>> 训练集: ${training.count()} 条")
    println(s">>> 测试集: ${test.count()} 条")

    // 3. 训练ALS模型
    //    参数配置: maxIter=10, rank=10, regParam=0.1
    println("\n>>> 开始训练ALS模型...")
    val als = new ALS()
      .setMaxIter(10)
      .setRank(10)
      .setRegParam(0.1)
      .setUserCol("userId")
      .setItemCol("movieId")
      .setRatingCol("rating")
      .setColdStartStrategy("drop")

    val model = als.fit(training)

    // 4. 评估模型 (RMSE)
    val predictions = model.transform(test)
    val evaluator = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")

    val rmse = evaluator.evaluate(predictions)
    println(s"\n>>> 模型评估 - RMSE: $rmse")

    // 5. 为所有用户生成推荐 (Top 3)
    println("\n>>> 为所有用户生成推荐...")
    val userRecs = model.recommendForAllUsers(3)

    println("\n>>> 推荐结果预览:")
    userRecs.show(10, false)

    // 6. 展开推荐结果: 每个用户-推荐电影-评分
    val explodedRecs = userRecs
      .select(
        col("userId"),
        explode(col("recommendations")).as("rec")
      )
      .select(
        col("userId"),
        col("rec.movieId").as("rec_movieId"),
        col("rec.rating").as("pred_rating")
      )

    println("\n>>> 展开后的推荐结果:")
    explodedRecs.show(20, false)

    // 7. 关联电影标题
    val recWithTitle = explodedRecs
      .join(moviesDF, explodedRecs("rec_movieId") === moviesDF("movieId"), "left")
      .select(
        col("userId"),
        col("rec_movieId"),
        col("title"),
        col("category"),
        col("pred_rating")
      )
      .orderBy("userId", "pred_rating")

    println("\n>>> 带电影标题的推荐结果:")
    recWithTitle.show(30, false)

    // 8. 写入HBase (movie_reco表)
    println("\n>>> 写入HBase...")
    writeToHBase(recWithTitle)

    // 9. 控制台输出汇总
    println("\n" + "=" * 60)
    println("推荐完成! 结果已写入 HBase 表: movie_reco")
    println("=" * 60)
    println("""HBase RowKey格式: userId
              |列族: info
              |列: recommendations (JSON格式: [{"movieId":101,"title":"流浪地球","rating":4.8},...])
              |""".stripMargin)

    spark.stop()
  }

  /**
   * 将推荐结果写入HBase
   */
  def writeToHBase(recDF: org.apache.spark.sql.DataFrame): Unit = {
    val hbaseConf = HBaseConfiguration.create()
    hbaseConf.set("hbase.zookeeper.quorum", "localhost:2181")
    // 请根据实际HBase集群修改zookeeper地址

    recDF.rdd.foreachPartition { partition =>
      val conn = ConnectionFactory.createConnection(hbaseConf)
      val table = conn.getTable(TableName.valueOf("movie_reco"))

      partition.foreach { row =>
        val userId = row.getAs[Int]("userId").toString
        val recMovieId = row.getAs[Int]("rec_movieId").toString
        val title = row.getAs[String]("title")
        val predRating = row.getAs[Float]("pred_rating")

        // 以 userID 为RowKey
        val put = new Put(Bytes.toBytes(userId))
        put.addColumn(
          Bytes.toBytes("info"),
          Bytes.toBytes(s"rec_movieId_${recMovieId}"),
          Bytes.toBytes(recMovieId)
        )
        put.addColumn(
          Bytes.toBytes("info"),
          Bytes.toBytes(s"title_${recMovieId}"),
          Bytes.toBytes(title)
        )
        put.addColumn(
          Bytes.toBytes("info"),
          Bytes.toBytes(s"rating_${recMovieId}"),
          Bytes.toBytes(predRating.toString)
        )

        // 同时保存JSON格式的推荐列表
        val jsonRec = s"""{"movieId":${recMovieId},"title":"${title}","rating":${predRating}}"""
        put.addColumn(
          Bytes.toBytes("info"),
          Bytes.toBytes("recommendations"),
          Bytes.toBytes(jsonRec)
        )

        table.put(put)
      }

      table.close()
      conn.close()
    }
  }
}
