package edu.monash.util;

/**
 * This is a useful utility to translate byte to
 * HEX String
 *
 * @author Shangqi
 * @version 0.1
 */
public class StringAndByte {
    /**
     * Translates byte array to a HEX String.
     *
     * @param buf of byte
     * @return a HEX Integer String
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * Translates a HexStr to a byte array.
     *
     * @param hexStr of a HEX Integer
     * @return a byte array represents the String
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length()/2];
        for (int i = 0;i < hexStr.length()/2; i++) {
            int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16);
            int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }
}
