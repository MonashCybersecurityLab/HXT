package edu.monash.crypto.util;

import edu.monash.util.StringAndByte;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

/**
 * This is an integrated AES package, it contains
 * not only encrypt/decrypt function, but also the functions
 * that can manipulate the string/byte array. All functions
 * are static here, so it can be easily used without
 * create any class instance.
 * </br>
 * Compare with Cong's version, I fix some warnings in old version,
 * and add essential comments for it.
 * </br>
 * Add AES-CMAC PRF function.
 *
 * @author Cong, Shangqi
 * @version 0.3
 */
public class AES {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private static byte[] N = StringAndByte.parseHexStr2Byte("62EC67F9C3A4A407FCB2A8C49031A8B3");

	public AES() {}
	public static byte[] encrypt(byte[] content, byte[] password){
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.setSeed(password);
			keyGenerator.init(128, random);
			SecretKey secretKey = keyGenerator.generateKey();
			byte[] enCodeFormat = secretKey.getEncoded();
			SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(N));
			return cipher.doFinal(content);
		} catch (BadPaddingException
                | IllegalBlockSizeException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchPaddingException
				| NoSuchProviderException e) {
			throw new RuntimeException(e);
        }
	}
	
	public static byte[] decrypt(byte[] content, byte[] password) {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.setSeed(password);
			keyGenerator.init(128, random);
			SecretKey secretKey = keyGenerator.generateKey();
			byte[] enCodeFormat = secretKey.getEncoded();
			SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(N));
			return cipher.doFinal(content);
		} catch (BadPaddingException
				| IllegalBlockSizeException
                | InvalidAlgorithmParameterException
				| InvalidKeyException
				| NoSuchAlgorithmException
				| NoSuchPaddingException
				| NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] encode(byte[] content, byte[] password) {
		try {
			Mac mac = Mac.getInstance("AESCMAC", "BC");
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.setSeed(password);
			keyGenerator.init(128, random);
			SecretKey secretKey = keyGenerator.generateKey();
			mac.init(secretKey);
			mac.update(content, 0, content.length);
			return mac.doFinal();
		} catch (InvalidKeyException
				| NoSuchAlgorithmException
				| NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String args[]){
		byte[] content = {1, 2, 3};
		byte[] password = {3, 2, 1, 6};
		
		byte[] content_encrypt = AES.encrypt(content, password);
		String hex_encrypt = StringAndByte.parseByte2HexStr(content_encrypt);
		System.out.println(hex_encrypt);

		byte[] content_decrypt = AES.decrypt(StringAndByte.parseHexStr2Byte(hex_encrypt), password);
		
		for(int i = 0; i < content.length; i++) {
			if (content_decrypt != null) {
				System.out.println(content_decrypt[i] + " ");
			}

		}
	}
}
