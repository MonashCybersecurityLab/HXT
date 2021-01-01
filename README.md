# HXT

HXT is a new searchable encryption scheme which supports efficient conjunctive queries. The proposed new scheme is based on the OXT [2]. Compared with OXT, it retains the *sub-linear* asymptotic search complexity for conjunctive queries but reaches better performance regarding its security. HXT is a part of our ACM CCS'18 paper [1]. 

The repository contains the implementation of the HXT, which consists of two parts. The first part is an implementation of OXT. We further combine it with our [SHVE scheme](https://github.com/MonashCybersecurityLab/SHVE) to implement the HXT. In addition, we use the implementation of OXT as baseline to evaluate the proposed scheme. For the detailed performance comparison, please refer to the paper.

## Requirements

- Git
- Java 8
- Scala (tested in version 2.11.12)
- sbt 0.13.6+

## Installation

```bash
git clone https://github.com/MonashCybersecurityLab/HXT.git
cd HXT
sbt
# once the sbt shell is ready, type the following command
> assembly
```

The [sbt-assembly](https://github.com/sbt/sbt-assembly) plugin is used to build a **FAT** JAR, which contains all the necessary dependencies for the prototype.

However, `package`command could be used to build a smaller JAR. In this case, you will be expected to provide the dependencies by yourselves. Below are dependencies added in this prototype:

* Bouncy Castle Crypto APIs: https://www.bouncycastle.org/
* Java Pairing-Based Cryptography (JPBC): http://gas.dia.unisa.it/projects/jpbc/
* Bloom Filter Scala: https://github.com/alexandrnikitin/bloom-filter-scala
* Spark 2.2.2: https://spark.apache.org/docs/2.2.0/
* HBase-Spark Connector 2.0.0-cdh6.0.0: https://mvnrepository.com/artifact/org.apache.hbase/hbase-spark/2.0.0-cdh6.0.0
* HBase 2.0.2 is used to test the prototype, you may experience incompatible issues if you choose to use the other versions.

## Usage

* Preparation

  The document should follow the structure below:

  ```
  id1 keyword1,keyword2,...
  id2 keyword3,keyword4,...
  ```

  and store in HDFS.

* To generate the OXT index

  run `spark-submit --class edu.monash.crypto.core.generator.GenOXT --master yarn --deploy-mode client --executor-memory 4g --num-executors 20 HXT-assembly-1.0.jar <document_name>`

* To generate the HXT index, ensure that the OXT index is generated

  run `spark-submit --class edu.monash.crypto.core.generator.GenHXT --master yarn --deploy-mode client --executor-memory 4g --num-executors 20 HXT-assembly-1.0.jar` 

* To test the OXT protocol

  run `spark-submit --class edu.monash.crypto.core.QueryOXT --master yarn --deploy-mode client --executor-memory 4g --num-executors 20 ../HXT-assembly-1.0.jar <keyword1> <keyword2> ...`

* To test the HXT protocol

  run `spark-submit --class edu.monash.crypto.core.QueryHXT --master yarn --deploy-mode client --executor-memory 4g --num-executors 20 ../HXT-assembly-1.0.jar <keyword1> <keyword2> ...`

In the above examples, the spark parameters are not optimised. You can get better performance after the parameter tuning based on your environment.

You can refer to some technical blogs to learn how to tune the performance of Spark job.

* [How-to: Tune Your Apache Spark Jobs (Part 1)](http://blog.cloudera.com/blog/2015/03/how-to-tune-your-apache-spark-jobs-part-1/)
* [How-to: Tune Your Apache Spark Jobs (Part 2)](http://blog.cloudera.com/blog/2015/03/how-to-tune-your-apache-spark-jobs-part-2/)

## Feedback

- [Submit an issue](https://github.com/MonashCybersecurityLab/HXT/issues/new)
- Email the authors: shangqi.lai@monash.edu, joseph.liu@monash.edu

## Reference

[1] Shangqi Lai, Sikhar Patranabis, Amin Sakzad, Joseph K. Liu, Debdeep Mukhopadhyay, Ron Steinfeld, Shi-Feng Sun, Dongxi Liu, and Cong Zuo. 2018. Result Pattern Hiding Searchable Encryption for Conjunctive Queries. In 2018 ACM SIGSAC Conference on Computer and Communications Security (CCS ’18), October 15–19, 2018, Toronto, ON, Canada. ACM, New York, NY, USA, 18 pages. https://doi.org/10.1145/3243734.3243753

[2] David Cash, Stanislaw Jarecki, Charanjit S. Jutla, Hugo Krawczyk, Marcel-Catalin Rosu, and Michael Steiner. 2013. Highly-Scalable Searchable Symmetric Encryption with Support for Boolean Queries. In Proceedings of the International Cryptology Conference (CRYPTO’13) (LNCS), Ran Canetti and Juan A. Garay (Eds.), Vol. 8042. Springer, 353--373.  https://dx.doi.org/10.1007/978-3-642-40041-4_20
