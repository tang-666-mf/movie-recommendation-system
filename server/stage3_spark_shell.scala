// ============================================================
// 阶段3: Spark Shell 离线数据探索
// 将 movies.csv 上传 HDFS，Spark SQL 查询分类电影
// ============================================================

// ===== 1. 在终端中执行以下命令上传数据到HDFS =====
// hdfs dfs -mkdir -p /user/movie_data
// hdfs dfs -put /path/to/movies.csv /user/movie_data/
// hdfs dfs -put /path/to/ratings.csv /user/movie_data/
// hdfs dfs -ls /user/movie_data/

// ===== 2. 启动 Spark Shell =====
// spark-shell --master yarn --deploy-mode client

// ===== 3. 在 Spark Shell 中执行以下 Scala 代码 =====

// 3.1 加载 movies.csv 到 DataFrame
val moviesDF = spark.read
  .option("header", "true")
  .option("inferSchema", "true")
  .csv("hdfs:///user/movie_data/movies.csv")

println("=== 原始数据预览 ===")
moviesDF.show()

// 3.2 加载 ratings.csv
val ratingsDF = spark.read
  .option("header", "true")
  .option("inferSchema", "true")
  .csv("hdfs:///user/movie_data/ratings.csv")

ratingsDF.show()

// 3.3 注册临时视图
moviesDF.createOrReplaceTempView("movies")
ratingsDF.createOrReplaceTempView("ratings")

// 3.4 查询各分类电影
println("\n=== SQL: 查询科幻类电影 ===")
spark.sql("""
  SELECT movieId, title, category
  FROM movies
  WHERE category = 'Sci-Fi'
""").show()

println("\n=== SQL: 查询剧情类电影 ===")
spark.sql("""
  SELECT movieId, title, category
  FROM movies
  WHERE category = 'Drama'
""").show()

println("\n=== SQL: 电影分类统计 ===")
spark.sql("""
  SELECT category, COUNT(*) as count
  FROM movies
  GROUP BY category
  ORDER BY count DESC
""").show()

// 3.5 电影评分聚合分析
println("\n=== SQL: 各电影平均评分 ===")
spark.sql("""
  SELECT m.title, m.category,
         ROUND(AVG(r.rating), 2) as avg_rating,
         COUNT(r.rating) as rating_count
  FROM movies m
  JOIN ratings r ON m.movieId = r.movieId
  GROUP BY m.title, m.category
  ORDER BY avg_rating DESC
""").show()

println("\n=== SQL: 各分类平均评分 ===")
spark.sql("""
  SELECT m.category,
         ROUND(AVG(r.rating), 2) as avg_rating,
         COUNT(r.rating) as total_ratings
  FROM movies m
  JOIN ratings r ON m.movieId = r.movieId
  GROUP BY m.category
  ORDER BY avg_rating DESC
""").show()

// 3.6 用户评分活跃度
println("\n=== SQL: 评分最多的用户 ===")
spark.sql("""
  SELECT userId, COUNT(*) as rating_count,
         ROUND(AVG(rating), 2) as avg_rating
  FROM ratings
  GROUP BY userId
  ORDER BY rating_count DESC
""").show()

// 退出 Spark Shell
// :q

// ===== 验收截图: show() 输出的ASCII表格查询结果 =====
