package edu.monash.crypto.util

object SecureParam {

  val K_P: Byte = 3

  val K_S: Array[Byte] = Array[Byte](3, 3, 3) // Key for e
  val K_X: Array[Byte] = Array[Byte](4, 4, 4) // Key for xtag
  val K_I: Array[Byte] = Array[Byte](5, 5, 5) // Key for index
  val K_Z: Array[Byte] = Array[Byte](6, 6, 6) // Key for Z
  val K_T: Array[Byte] = Array[Byte](7, 7, 7) // Key for keyword
  val K_H: Array[Byte] = Array[Byte](8, 8, 8) // Key for SHVE
}

object MapUtil {
  def getRind(ind: Array[Byte]): Array[Byte] = ind.map(b => (b ^ SecureParam.K_P).toByte)
}
