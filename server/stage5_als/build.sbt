name := "MovieRecommendationALS"
version := "1.0"
scalaVersion := "2.12.17"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.3.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.3.0" % "provided",
  "org.apache.spark" %% "spark-mllib" % "3.3.0",
  "org.apache.hbase" % "hbase-client" % "2.4.12",
  "org.apache.hbase" % "hbase-common" % "2.4.12"
)
