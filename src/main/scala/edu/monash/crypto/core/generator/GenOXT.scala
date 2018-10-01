package edu.monash.crypto.core.generator

import java.io.FileOutputStream

import bloomfilter.CanGenerateHashFrom.CanGenerateHashFromString
import bloomfilter.mutable.{BloomFilter, UnsafeBitArray}
import edu.monash.crypto.util._
import edu.monash.util.{HBaseUtils, StringAndByte}
import it.unisa.dia.gas.jpbc.{Element, Pairing}
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.{SparkConf, SparkContext}

object GenOXT {
  def main(args: Array[String]) {
    // init HBase Table
    val hbaseConf = HBaseConfiguration.create
    //Add any necessary configuration files (hbase-site.xml, core-site.xml)
    hbaseConf.addResource(new Path(System.getenv("HBASE_CONF_DIR"), "hbase-site.xml"))
    hbaseConf.addResource(new Path(System.getenv("HADOOP_CONF_DIR"), "core-site.xml"))
    HBaseUtils.createSchemaTables(hbaseConf, HBaseUtils.TSET_TABLE, HBaseUtils.TSET_CF)

    // spark context init
    val sparkConf = new SparkConf().setAppName("OXT")
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer.max", "512")
      .registerKryoClasses(Array(classOf[Element], classOf[BloomFilter[String]]))
    sparkConf.set("spark.driver.maxResultSize", "80g")
    val spark = new SparkContext(sparkConf)

    // elliptical curve init
    val pairing: Pairing = PairingFactory.getPairing("params/curves/a.properties")

    // get a fixed g for this database
    val g : Element = pairing.getGT
      .newRandomElement
      .getImmutable

    // save the elliptical curve
    val fos = new FileOutputStream("elliptical_g")
    fos.write(g.getLengthInBytes)
    fos.write(g.toBytes)
    fos.close()
    val prePow = g.getElementPowPreProcessing

    // create broadcast variables
    val broadcastPairing = spark.broadcast(pairing)
    val broadcastPow = spark.broadcast(prePow)

    // if it has a valid parameter
    if(args.length > 0) {
      // Load RDD from File
      val plainDB = spark.textFile(args(0))
      // Generate inverted Index
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
        invertedIndex
      }).repartition(900)
      invertedIndex.cache

      // generate xtags
      val edb2 = invertedIndex.randomSplit(Array.fill[Double](20) {
        1.0
      }, 20) // randomly partition the inverted index into 20 segments
        .map(rdd => { // map each rdd in array to xtag
          rdd.flatMap(entry => {
            val indies = entry._2.split(" ")
            var xtags = Set[String]()
            val kxw = AES.encode(entry._1.getBytes, SecureParam.K_X)
            for(index <- indies) {
              val xind = AES.encode(StringAndByte.parseHexStr2Byte(index), SecureParam.K_I)
              val xtag = broadcastPow.value.powZn(Hash.HashToZr(broadcastPairing.value, kxw)
                .mul(Hash.HashToZr(broadcastPairing.value, xind)))
              xtags += xtag.toString
            }
            xtags
          })
        })

      // Create a Bloom filter
      val expectedElements = invertedIndex.count
      val falsePositiveRate = 0.000001
      val bf = BloomFilter[String](expectedElements, falsePositiveRate)
      println(expectedElements + " generated.")

      // calculate hash in workers
      val broadcastNumBit = spark.broadcast(bf.numberOfBits)
      val broadcastNumHash = spark.broadcast(bf.numberOfHashes)

      // use reflection to retrieve bit array
      val bitField = bf.getClass.getDeclaredField("bits")
      bitField.setAccessible(true)
      val bits = bitField.get(bf).asInstanceOf[UnsafeBitArray]

      // compute the positions of each xtag and collect it back
      edb2.foreach(rdd => {
          rdd.mapPartitions(xtags => {
            var xtagHashs = Set[Long]()
            xtags.foreach(xtag => {
              val hash = CanGenerateHashFromString.generateHash(xtag)
              val hash1 = hash >>> 32
              val hash2 = (hash << 32) >> 32

              var i = 0
              while (i < broadcastNumHash.value) {
                val computedHash = hash1 + i * hash2
                xtagHashs += ((computedHash & Long.MaxValue) % broadcastNumBit.value)
                i += 1
              }
            })
            xtagHashs.iterator
          }).collect.foreach(xtagHashs => bits.set(xtagHashs))
        })

      // Remove EDB2
      edb2.foreach(rdd => rdd.unpersist())

      // Init Bloom Filter on disk
      bf.writeTo(new FileOutputStream("bloomfilter_oxt"))
      bf.dispose

      // generate encrypted database
      val edb1 = invertedIndex.reduceByKey(_ + " " + _)
        .flatMap(entry => {
          var edbTuple = List[(Array[Byte], Array[Byte], Array[Byte])]()
          val indies = entry._2.split(" ")
          val K_e = AES.encode(entry._1.getBytes, SecureParam.K_S)
          var c = 1
          indies.foreach(index => {
            val xind = AES.encode(StringAndByte.parseHexStr2Byte(index), SecureParam.K_I)
            val z = AES.encode(entry._1.concat(String valueOf c).getBytes, SecureParam.K_Z)
            val e = AES.encrypt(StringAndByte.parseHexStr2Byte(index), K_e)
            val y = Hash.HashToZr(broadcastPairing.value, xind)
              .div(Hash.HashToZr(broadcastPairing.value, z)).getImmutable
            val key = StringAndByte.parseByte2HexStr(AES.encode(entry._1.getBytes, SecureParam.K_T))
              .concat("~").concat(String valueOf c)
            // Generate Tuple of (key~i, e, y)
            edbTuple = edbTuple:+(key.getBytes, e, y.toBytes)
            c += 1
          })
          edbTuple
        })

      // Put the tuple into HBase
      val hbaseContext = new HBaseContext(spark, hbaseConf)
      hbaseContext.bulkPut[(Array[Byte], Array[Byte], Array[Byte])](edb1,
        TableName.valueOf(HBaseUtils.TSET_TABLE),
        putRecord => {
          val put = new Put(putRecord._1)
          put.addColumn(Bytes.toBytes(HBaseUtils.TSET_CF), Bytes.toBytes("e")
            , putRecord._2)
          put.addColumn(Bytes.toBytes(HBaseUtils.TSET_CF)
            , Bytes.toBytes("y")
            , putRecord._3)
          put
        })

      // Task Finished
      invertedIndex.unpersist()
      spark.stop

    }
    else
      Console.err.println("Please enter filename")
  }
}
