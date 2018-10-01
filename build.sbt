name := "HXT"

version := "1.0"

scalaVersion := "2.11.12"

resolvers ++= Seq("Cloudera Repository" at "https://repository.cloudera.com/content/repositories/releases/")

resolvers ++= Seq("jitpack.io" at "https://jitpack.io")

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.2"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.2.2"

libraryDependencies += "org.apache.hbase" % "hbase-server" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-client" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-common" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-mapreduce" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-hadoop-compat" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-hadoop2-compat" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-metrics-api" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-metrics" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-zookeeper" % "2.0.2"

libraryDependencies += "org.apache.hbase" % "hbase-spark" % "2.0.0-cdh6.0.0"

libraryDependencies += "com.github.emilianobonassi" % "jpbc" % "v2.0.0"

libraryDependencies += "com.github.alexandrnikitin" %% "bloom-filter" % "latest.release"

libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.58"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}