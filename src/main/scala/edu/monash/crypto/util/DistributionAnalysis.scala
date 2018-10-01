package edu.monash.crypto.util

import org.apache.spark.{SparkConf, SparkContext}

/**
  * This is a test program that can evaluate
  * the dataset itself. It outputs the distribution
  * of documents with different keywords.
  */
object DistributionAnalysis {
  def main(args: Array[String]): Unit = {
    // spark context init
    val sparkConf = new SparkConf().setAppName("Measure")
    val spark = new SparkContext(sparkConf)
    val s1 = spark.longAccumulator("s1")
    val s10 = spark.longAccumulator("s10")
    val s100 = spark.longAccumulator("s100")
    val s1000 = spark.longAccumulator("s1000")
    val s10000 = spark.longAccumulator("s10000")
    val s100000 = spark.longAccumulator("s100000")
    val s1000000 = spark.longAccumulator("s1000000")
    if(args.length > 0) {
      // Load RDD from File
      val plainDB = spark.textFile(args(0))
      plainDB.flatMap(s => {
        val pair = s.split(" ")
        if (pair.length == 2) {
          pair(1).split(",")
        } else {
          Array[String]()
        }
      })
        .map(word => (word, 1))
        .reduceByKey(_ + _)
        .foreachPartition(vs => {
          vs.foreach(v => {
            if(v._2 >= 1) s1.add(1)
            if(v._2 >= 10) s10.add(1)
            if(v._2 >= 100) s100.add(1)
            if(v._2 >= 1000) s1000.add(1)
            if(v._2 >= 10000) s10000.add(1)
            if(v._2 >= 100000) s100000.add(1)
            if(v._2 >= 1000000) s1000000.add(1)
          })
        })
      println(">=1" + s1.value)
      println(">=10" + s10.value)
      println(">=100" + s100.value)
      println(">=1000" + s1000.value)
      println(">=10000" + s10000.value)
      println(">=100000" + s100000.value)
      println(">=1000000" + s1000000.value)
    }
  }
}
