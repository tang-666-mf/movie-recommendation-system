package com.realtime

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * 阶段4: 实时热门影视监控 - Spark Structured Streaming
 *
 * 功能: 从Kafka读取点击日志, 使用window算子(10秒窗口,5秒滑动)
 *       实时聚合统计点击次数最多的电影, 写入MySQL/HBase
 *
 * 打包: sbt clean assembly (或 sbt package)
 * 提交: spark-submit --master yarn --deploy-mode client HotMovieStreaming.jar
 */
object HotMovieStreaming {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("HotMovieStreaming")
      .config("spark.sql.shuffle.partitions", "4")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // 1. 定义点击日志的Schema
    val clickSchema = StructType(Array(
      StructField("movieId", StringType, true),
      StructField("timestamp", StringType, true)
    ))

    // 2. 从Kafka读取数据流
    val kafkaStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "movie_logs")
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false")
      .load()

    // 3. 解析JSON/CSV格式的Kafka消息
    //    kafka_click_logs.csv 格式: movieId,timestamp
    import spark.implicits._
    val parsedStream = kafkaStream
      .selectExpr("CAST(value AS STRING) as raw_value")
      .select(
        // 按逗号分割: movieId,timestamp
        when(
          split(col("raw_value"), ",").size >= 2,
          split(col("raw_value"), ",")(0)
        ).as("movieId"),
        when(
          split(col("raw_value"), ",").size >= 2,
          split(col("raw_value"), ",")(1)
        ).as("timestamp_str")
      )
      .filter(col("movieId").isNotNull)

    // 4. 使用window算子统计点击次数 (窗口10秒, 滑动5秒)
    val clickCounts = parsedStream
      .withColumn("click_time", to_timestamp(col("timestamp_str"), "yyyy-MM-dd HH:mm:ss"))
      .withWatermark("click_time", "30 seconds")  // 水位线处理乱序
      .groupBy(
        window(col("click_time"), "10 seconds", "5 seconds"),
        col("movieId")
      )
      .agg(count("*").as("click_count"))
      .select(
        col("window.start").as("window_start"),
        col("window.end").as("window_end"),
        col("movieId"),
        col("click_count")
      )

    // 5. 写入MySQL (方式一)
    val mysqlSink = clickCounts.writeStream
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF, batchId) =>
        batchDF.write
          .mode("append")
          .format("jdbc")
          .option("url", "jdbc:mysql://localhost:3306/movie_analysis?useSSL=false&characterEncoding=utf8")
          .option("driver", "com.mysql.jdbc.Driver")
          .option("user", "root")
          .option("password", "123456")
          .option("dbtable", "hot_movies")
          .save()
      }
      .start()

    // 5b. 可选: 同时写入HBase (方式二)
    val hbaseSink = clickCounts.writeStream
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF, batchId) =>
        batchDF.rdd.foreachPartition { partition =>
          // HBase连接配置 (请根据实际集群修改)
          val hbaseConf = org.apache.hadoop.hbase.HBaseConfiguration.create()
          hbaseConf.set("hbase.zookeeper.quorum", "localhost:2181")
          val conn = org.apache.hadoop.hbase.client.ConnectionFactory.createConnection(hbaseConf)
          val table = conn.getTable(org.apache.hadoop.hbase.TableName.valueOf("movie_reco"))

          partition.foreach { row =>
            val movieId = row.getAs[String]("movieId")
            val clickCount = row.getAs[Long]("click_count")
            val windowStart = row.getAs[java.sql.Timestamp]("window_start").toString

            val put = new org.apache.hadoop.hbase.client.Put(
              org.apache.hadoop.hbase.util.Bytes.toBytes(s"hot_${windowStart}_${movieId}")
            )
            put.addColumn(
              org.apache.hadoop.hbase.util.Bytes.toBytes("info"),
              org.apache.hadoop.hbase.util.Bytes.toBytes("movieId"),
              org.apache.hadoop.hbase.util.Bytes.toBytes(movieId)
            )
            put.addColumn(
              org.apache.hadoop.hbase.util.Bytes.toBytes("info"),
              org.apache.hadoop.hbase.util.Bytes.toBytes("click_count"),
              org.apache.hadoop.hbase.util.Bytes.toBytes(clickCount.toString)
            )
            table.put(put)
          }
          table.close()
          conn.close()
        }
      }
      .start()

    // 6. 同时打印到控制台（调试用）
    val consoleSink = clickCounts.writeStream
      .outputMode("update")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .format("console")
      .option("truncate", "false")
      .start()

    // 7. 等待所有流完成
    consoleSink.awaitTermination()
  }
}
