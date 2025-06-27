name := "stateful-freshness"
scalaVersion := "2.12.18"
version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.spark" %% "spark-streaming" % "3.5.0"
)

lazy val root = project in file(".")
