package edu.monash.crypto.core

import java.io.{File, FileInputStream}
import java.math.BigInteger

import bloomfilter.CanGenerateHashFrom.CanGenerateHashFromString
import bloomfilter.mutable.BloomFilter
import edu.monash.crypto.oxt.entity.OXTTSetTuple
import edu.monash.crypto.util._
import edu.monash.util.{HBaseUtils, RandomUtil, StringAndByte}
import it.unisa.dia.gas.jpbc.{Element, Pairing}
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.client.{ConnectionFactory, Get, Scan}
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.{SparkConf, SparkContext}

object QueryHXT {
  def main(args: Array[String]): Unit = {
    // spark context init
    val sparkConf = new SparkConf().setAppName("HXT")
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer.max", "512")
      .registerKryoClasses(Array(classOf[Element], classOf[BloomFilter[String]]))
    sparkConf.set("spark.driver.maxResultSize", "80g")
    val spark = new SparkContext(sparkConf)

    // load Bloom Filter
    val bfFile = new File("bloomfilter_oxt")
    val fReader = new FileInputStream(bfFile)
    val bf = BloomFilter.readFrom[String](fReader)
    // close stream
    fReader.close()

    // elliptical curve init
    val pairing: Pairing = PairingFactory.getPairing("params/curves/a.properties")
    // load curve
    val gFile = new File("elliptical_g")
    val gReader = new FileInputStream(gFile)
    val size = gReader.read
    val gInBytes = new Array[Byte](size)
    gReader.read(gInBytes)
    val g = pairing.getGT.newElementFromBytes(gInBytes).getImmutable
    val prePow = g.getElementPowPreProcessing
    gReader.close()

    if (bf == null) {
      System.out.println("Invalid Bloom filter.")
      System.exit(-1)
    }

    if (prePow == null) {
      System.out.println("Invalid elliptical curve.")
      System.exit(-1)
    }

    val numberOfBits = bf.numberOfBits
    val numberOfHashes = bf.numberOfHashes

    // init HBase Table
    val hbaseConf = HBaseConfiguration.create
    //Add any necessary configuration files (hbase-site.xml, core-site.xml)
    hbaseConf.addResource(new Path(System.getenv("HBASE_CONF_DIR"), "hbase-site.xml"))
    hbaseConf.addResource(new Path(System.getenv("HADOOP_CONF_DIR"), "core-site.xml"))

    val connection = ConnectionFactory.createConnection(hbaseConf)
    val admin = connection.getAdmin

    val tsetTable = connection.getTable(TableName.valueOf(HBaseUtils.TSET_TABLE))
    val hveTable = connection.getTable(TableName.valueOf(HBaseUtils.HVE_TABLE))
    if (HBaseUtils.isAvailable(admin, tsetTable.getName)
      && HBaseUtils.isAvailable(admin, hveTable.getName)) { // check the TSet
      val hbase = new HBaseContext(spark, hbaseConf)

      // counter for time
      var start = 0L
      var end = 0L
      if (args.length > 0) {
        // generate stag
        System.out.println("\nClient is generating stag ... ")
        start = System.nanoTime
        val stag = AES.encode(args(0).getBytes(), SecureParam.K_T)
        end = System.nanoTime
        System.out.println("Client generates stag time " + (end - start) + " ns")

        if (stag != null) {
          // attach id to scan the table
          val startKey = StringAndByte.parseByte2HexStr(stag).concat("~").getBytes
          val endKey = StringAndByte.parseByte2HexStr(stag).concat("~~").getBytes
          val scan = new Scan().withStartRow(startKey).withStopRow(endKey)
          scan.setCaching(1048576)

          // load result and return OXT Tuples
          start = System.nanoTime
          val resultList = hbase.hbaseRDD(TableName.valueOf(HBaseUtils.TSET_TABLE), scan)
            .map(tuple => tuple._2)
            .collect
          end = System.nanoTime

          // parse HBase result to OXT Tuples
          val oxttSetTuples = resultList.toStream
            .par
            .map(result => {
              val oxttSetTuple = new OXTTSetTuple
              oxttSetTuple.e = result.getValue(Bytes.toBytes(HBaseUtils.TSET_CF), Bytes.toBytes("e"))
              oxttSetTuple.y = pairing.getZr.newElement(
                new BigInteger(result.getValue(Bytes.toBytes(HBaseUtils.TSET_CF)
                  , Bytes.toBytes("y"))))
              oxttSetTuple
            }).toArray
          System.out.println("Server searches stag time " + (end - start) + " ns")

          // some results are returned from TSet
          if (oxttSetTuples != null && !oxttSetTuples.isEmpty) {
            // Concat xtoken with OXT Tuple
            var tupleList = Vector[(OXTTSetTuple, Array[Element])]()

            System.out.println("\nClient is generating xtoken ... ")
            start = System.nanoTime
            val xtoken = new Array[Array[Element]](oxttSetTuples.length)
            val xterms = new Array[String](args.length - 1)
            System.arraycopy(args, 1, xterms, 0, xterms.length)
            for (c <- 1 to oxttSetTuples.length) {
              val z = AES.encode(args(0).concat(String.valueOf(c)).getBytes, SecureParam.K_Z)
              val e_z = Hash.HashToZr(pairing, z).getImmutable
              xtoken(c - 1) = xterms.toStream
                .par
                .map(xterm => {
                  val kxw = AES.encode(xterm.getBytes, SecureParam.K_X)
                  val e_kxw = Hash.HashToZr(pairing, kxw)
                  prePow.powZn(e_z.mul(e_kxw)).getImmutable
                }).toArray

              tupleList = tupleList :+ (oxttSetTuples(c - 1), xtoken(c - 1))
            }
            end = System.nanoTime
            System.out.println("Client compute xtoken time " + (end - start) + " ns")

            val pairRDD = spark.parallelize(tupleList)

            System.out.println("\nServer is generating xtag ... ")
            val xtagAccm = spark.longAccumulator
            val hashTupleList = pairRDD.map(pair => {
              // compute all xtag
              var xtags = Set[String]()
              val start = System.nanoTime
              for (c <- 1 to pair._2.length) {
                xtags += pair._2(c - 1).powZn(pair._1.y).getImmutable.toString
              }
              val end = System.nanoTime
              xtagAccm.add(end - start)
              pair._1 -> xtags.toArray
            }).map(xtag => {
              // map xtags to hash values
              var xtagHashes = Set[Long]()
              val start = System.nanoTime
              for (c <- 1 to xtag._2.length) {
                val hash = CanGenerateHashFromString.generateHash(xtag._2(c - 1))
                val hash1 = hash >>> 32
                val hash2 = (hash << 32) >> 32

                var i = 0
                while (i < numberOfHashes) {
                  val computedHash = hash1 + i * hash2
                  xtagHashes += ((computedHash & Long.MaxValue) % numberOfBits)
                  i += 1
                }
              }

              val end = System.nanoTime
              xtagAccm.add(end - start)
              xtag._1 -> xtagHashes.toArray
            }).collect

            System.out.println("Server searches xtag time " + xtagAccm.value + " ns")

            System.out.println("Start to match HVE")

            // the result vector
            var es = Vector[Array[Byte]]()

            start = System.nanoTime
            hashTupleList.toStream.par.foreach(hashTuple => {
              // retrieve HVE ciphertext
              val hveCipher =
                hbase.bulkGet[Long, Array[Byte]](TableName.valueOf(HBaseUtils.HVE_TABLE),
                  2,
                  spark.parallelize(hashTuple._2),
                  record => {
                    new Get(Hash.SHA256(Bytes.toBytes(record)))
                  },
                  result => {
                    result.getValue(Bytes.toBytes(HBaseUtils.HVE_CF), Bytes.toBytes("c"))
                  }).reduce((A, B) => {
                  require(A.length == B.length)
                  val C = new Array[Byte](A.length)
                  for (i <- A.indices) {
                    C(i) = (A(i) ^ B(i)).toByte
                  }
                  C
                })

              // Compute HVE key on client
              // Generate a random mask
              val D0 = new Array[Byte](16)
              for (i <- hashTuple._2.indices) {
                val d = AES.encode(String.valueOf(1)
                  .concat(String.valueOf(hashTuple._2(i)))
                  .getBytes, SecureParam.K_H)
                for (j <- d.indices) {
                  D0(j) = (D0(j) ^ d(j)).toByte
                }
              }

              // Randomly choose a key K
              val K = RandomUtil.getRandom(D0.length * 8 - 1)
              // use K to encrypt "0"
              val D1 = AES.encrypt(BigInteger.valueOf(0).toByteArray, K)
              for (i <- D0.indices) {
                D0(i) = (D0(i) ^ K(i)).toByte
              }

              val k = new Array[Byte](D0.length)
              for (i <- D0.indices) {
                k(i) = (hveCipher(i) ^ D0(i)).toByte
              }
              try {
                AES.decrypt(D1, k)
                es = es :+ hashTuple._1.e
              } catch {
                case e: RuntimeException =>
              }
            })
            end = System.nanoTime
            System.out.println("HVE query time " + (end - start) + " ns")

            // client decrypt e
            System.out.println("Client gets rinds.")
            start = System.nanoTime
            val K_e = AES.encode(args(0).getBytes, SecureParam.K_S)
            val rinds = es.toStream
              .par
              .map(e => AES.decrypt(e, K_e))
              .toArray
            end = System.nanoTime
            System.out.println("Client gets inds time " + (end - start) + " ns")
            System.out.println("Size of result: " + rinds.length)

          } else {
            System.out.println("Empty Result set.")
            System.exit(-1)
          }
        } else {
          System.out.println("Invalid Stag.")
          System.exit(-1)
        }
      }
    } else {
      System.out.println("TSet is unavailable.")
      System.exit(-1)
    }
  }
}
