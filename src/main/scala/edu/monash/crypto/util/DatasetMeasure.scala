package edu.monash.crypto.util

import edu.monash.util.StringAndByte
import org.apache.spark.{SparkConf, SparkContext}

/**
  * This is a test program that can evaluate
  * the dataset itself. It can output the statistical
  * results (such as no. of documents, no. of keywords, etc)
  * for the dataset.
  */
object DatasetMeasure {
  def main(args: Array[String]) {
    // spark context init
    val sparkConf = new SparkConf().setAppName("Measure")
    val spark = new SparkContext(sparkConf)

    if(args.length > 0) {
      // Load RDD from File
      val plainDB = spark.textFile(args(0))
      println(plainDB.count + " documents")

      // count the word frequency
      val counts = plainDB.flatMap(s => {
        val pair = s.split(" ")
        if (pair.length == 2) {
          pair(1).split(",")
        } else {
          Array[String]()
        }
      })
        .map(word => (word, 1))
        .reduceByKey(_ + _)
        .sortBy(_._2, ascending = true)
      counts.saveAsTextFile(args(0) + ".count")

      // Generate inverted Index
      val N = spark.longAccumulator("N")
      val invertedIndex = plainDB.flatMap(s => {
        var invertedIndex = Map[String, String]()
        val pair = s.split(" ")
        if (pair.length == 2) {
          val rind = MapUtil.getRind(pair(0).getBytes)
          val keywords = pair(1).split(",")
          for (keyword <- keywords) {
            invertedIndex +=
              (keyword -> StringAndByte.parseByte2HexStr(rind))
          }
        }
        N.add(invertedIndex.size)
        invertedIndex
      }).reduceByKey(_ + " " + _)
      println(invertedIndex.count + " keywords")
      println(N.value + " pairs")
    }

  }
}