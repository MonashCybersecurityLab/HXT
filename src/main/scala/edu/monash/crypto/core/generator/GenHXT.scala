package edu.monash.crypto.core.generator

import java.io.{File, FileInputStream}

import bloomfilter.mutable.{BloomFilter, UnsafeBitArray}
import edu.monash.crypto.util.{AES, Hash, SecureParam}
import edu.monash.util.HBaseUtils
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.{SparkConf, SparkContext}

object GenHXT {
  def main(args: Array[String]) {

    // init HBase Table
    val hbaseConf = HBaseConfiguration.create
    hbaseConf.addResource("/etc/hbase/conf/hbase-site.xml")
    hbaseConf.addResource("/etc/hadoop/conf/core-site.xml")
    HBaseUtils.createSchemaTables(hbaseConf, HBaseUtils.HVE_TABLE, HBaseUtils.HVE_CF)

    // spark context init
    val sparkConf = new SparkConf().setAppName("HXT")
    sparkConf.set("spark.driver.maxResultSize", "30g")
    val spark = new SparkContext(sparkConf)

    // init the HBase context
    val hbaseContext = new HBaseContext(spark, hbaseConf)

    // load Bloom Filter
    val bfFile = new File("bloomfilter_oxt")
    val fReader = new FileInputStream(bfFile)
    val bf = BloomFilter.readFrom[String](fReader)

    // use reflection to retrieve bit array
    val bitField = bf.getClass.getDeclaredField("bits")
    bitField.setAccessible(true)
    val bits = bitField.get(bf).asInstanceOf[UnsafeBitArray]

    // To prevent memory leakage, split the Bloom Filter into several iterations
    val bitSize = bits.numberOfBits
    val iter = bitSize / 100000000L
    for (i <- 0L until iter) { // split hve vector and put it into RDD
      val indies = spark.parallelize((i * 100000000L until (i + 1) * 100000000L).map(i => (i, bits.get(i))))
      val hve = indies.map(index => {
        if (index._2) {
          (Hash.SHA256(Bytes.toBytes(index._1)), AES.encode(String.valueOf(1).concat(String.valueOf(index._1)).getBytes, SecureParam.K_H))
        } else {
          (Hash.SHA256(Bytes.toBytes(index._1)), AES.encode(String.valueOf(0).concat(String.valueOf(index._1)).getBytes, SecureParam.K_H))
        }
      })

      // Put the tuple into HBase
      hbaseContext.bulkPut[(Array[Byte], Array[Byte])](hve,
        TableName.valueOf(HBaseUtils.HVE_TABLE),
        putRecord => {
          val put = new Put(putRecord._1)
          put.addColumn(Bytes.toBytes(HBaseUtils.HVE_CF), Bytes.toBytes("c")
            , putRecord._2)
          put
        })
    }
    if (bitSize - iter * 100000000L > 0L) {
      val residue = bitSize - iter * 100000000L
      val indies = spark.parallelize((iter * 100000000L until iter * 100000000L + residue).map(i => (i, bits.get(i))))
      val hve = indies.map(index => {
        if (index._2) {
          (Hash.SHA256(Bytes.toBytes(index._1)), AES.encode(String.valueOf(1).concat(String.valueOf(index._1)).getBytes, SecureParam.K_H))
        } else {
          (Hash.SHA256(Bytes.toBytes(index._1)), AES.encode(String.valueOf(0).concat(String.valueOf(index._1)).getBytes, SecureParam.K_H))
        }
      })

      // Put the tuple into HBase
      hbaseContext.bulkPut[(Array[Byte], Array[Byte])](hve,
        TableName.valueOf(HBaseUtils.HVE_TABLE),
        putRecord => {
          val put = new Put(putRecord._1)
          put.addColumn(Bytes.toBytes(HBaseUtils.HVE_CF), Bytes.toBytes("c")
            , putRecord._2)
          put
        })
    }
    bf.dispose

    // Task Finished
    spark.stop
  }
}
