package edu.monash.crypto.util;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPowPreProcessing;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class SingleTest {

    public static void main(String[] args) {
        long start, end;

        long n = 10000;

        // warm-up AES here
        AES.decrypt(AES.encrypt("test".getBytes(), SecureParam.K_T()), SecureParam.K_T());
        AES.encode("test".getBytes(), SecureParam.K_T());
        // SYM Encryption/Decryption Time
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            AES.encrypt("test".getBytes(), SecureParam.K_T());
        }
        end = System.nanoTime();
        System.out.println("AES-CBC encryption time:" + (end - start)/n);
        byte[] res = AES.encrypt("test".getBytes(), SecureParam.K_T());
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            AES.decrypt(res, SecureParam.K_T());
        }
        end = System.nanoTime();
        System.out.println("AES-CBC decryption time:" + (end - start)/n);
        // PRF Time
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            AES.encode(res, SecureParam.K_T());
        }
        end = System.nanoTime();
        System.out.println("AES CMAC time:" + (end - start) / n);

        // XOR Time
        byte a;
        start = System.nanoTime();
        for(int i = 0; i < n * 8; i++) {
            a = 0x1 ^ 0x2;
        }
        end = System.nanoTime();
        System.out.println("XOR time:" + (end - start) / n);
        // Pairing time
        Pairing pairing = PairingFactory.getPairing("params/curves/a.properties");
        Element g = pairing.getG1().newElement().setToRandom().getImmutable();
        /*start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            pairing.pairing(g, g);
        }
        end = System.nanoTime();
        System.out.println("pairing time:" + (end - start)/n);

        PairingPreProcessing pairingG = pairing.getPairingPreProcessingFromElement(g);
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            pairingG.pairing(g);
        }
        end = System.nanoTime();
        System.out.println("pairing preprocessed time:" + (end - start)/n);*/

        // G1 exponentiation time
        Element e = pairing.getZr().newRandomElement().getImmutable();
        /*start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            g.powZn(e);
        }
        end = System.nanoTime();
        System.out.println("A curve G1 exponentiation time:" + (end - start)/n);

        start = System.nanoTime();
        ElementPowPreProcessing preG = g.getElementPowPreProcessing();
        end = System.nanoTime();
        System.out.println("A curve G1 preprocessing time:" + (end - start)/n);

        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            preG.powZn(e);
        }
        end = System.nanoTime();
        System.out.println("A curve G1 exponentiation preprocessed time:" + (end - start)/n);*/

        // GT exponentiation time
        Element gt = pairing.getGT().newElement().setToRandom().getImmutable();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            gt.powZn(e);
        }
        end = System.nanoTime();
        System.out.println("A curve GT exponentiation time:" + (end - start)/n);

        start = System.nanoTime();
        ElementPowPreProcessing preGt = gt.getElementPowPreProcessing();
        end = System.nanoTime();
        System.out.println("A curve GT preprocessing time:" + (end - start)/n);

        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            preGt.powZn(e);
        }
        end = System.nanoTime();
        System.out.println("A curve GT exponentiation preprocessed time:" + (end - start)/n);

        // Curve D224
        Pairing pairingD = PairingFactory.getPairing("params/curves/d224.properties");
        Element gD = pairingD.getG1().newElement().setToRandom().getImmutable();
        // G1 exponentiation time
        Element eD = pairing.getZr().newRandomElement().getImmutable();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            gD.powZn(eD);
        }
        end = System.nanoTime();
        System.out.println("D224 curve G1 exponentiation time:" + (end - start)/n);

        start = System.nanoTime();
        ElementPowPreProcessing preGD = gD.getElementPowPreProcessing();
        end = System.nanoTime();
        System.out.println("D224 curve G1 preprocessing time:" + (end - start)/n);

        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            preGD.powZn(eD);
        }
        end = System.nanoTime();
        System.out.println("D224 curve G1 exponentiation preprocessed time:" + (end - start)/n);

        // GT exponentiation time
        Element gtD = pairingD.getGT().newElement().setToRandom().getImmutable();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            gtD.powZn(eD);
        }
        end = System.nanoTime();
        System.out.println("D224 curve GT exponentiation time:" + (end - start)/n);

        start = System.nanoTime();
        ElementPowPreProcessing preGtD = gtD.getElementPowPreProcessing();
        end = System.nanoTime();
        System.out.println("D224 curve GT preprocessing time:" + (end - start)/n);

        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            preGtD.powZn(eD);
        }
        end = System.nanoTime();
        System.out.println("D224 curve GT exponentiation preprocessed time:" + (end - start)/n);

        // multiplication time
        Element m1 = pairing.getZr().newRandomElement().getImmutable();
        Element m2 = pairing.getZr().newRandomElement().getImmutable();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            m1.mul(m2);
        }
        end = System.nanoTime();
        System.out.println("multiplication over Zr time:" + (end - start)/n);

        // division time
        Element d1 = pairing.getZr().newRandomElement().getImmutable();
        Element d2 = pairing.getZr().newRandomElement().getImmutable();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            d1.div(d2);
        }
        end = System.nanoTime();
        System.out.println("divided over Zr time:" + (end - start)/n);

        // subtract time
        Element s1 = pairing.getZr().newRandomElement().getImmutable();
        Element s2 = pairing.getZr().newElement().setToRandom();
        start = System.nanoTime();
        for(int i = 0; i < n; i++) {
            s1.sub(s2);
        }
        end = System.nanoTime();
        System.out.println("subtract over Zr time:" + (end - start)/n);
    }
}
