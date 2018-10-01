package edu.monash.crypto.util;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

import java.security.MessageDigest;
import java.util.Formatter;

public class Hash {
    public static Element HashToZr(Pairing pairing, byte[] m){
        return pairing.getZr().newElementFromHash(m, 0, m.length);
    }

    public static Element HashToG1(Pairing pairing, byte[] m){
        return pairing.getG1().newElementFromHash(m, 0, m.length);
    }

    public static byte[] SHA256(byte[] message) {
        try{
            return MessageDigest.getInstance("SHA-256").digest(message);
        } catch (Exception e) {
            System.out.println("No such algorithm.");
        }
        return null;
    }

    private static String byteToHex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
