package com.u.securekeys.internal;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * Class to encode strings with a given key
 * Created by saguilera on 3/3/17.
 */
public class Encoder {

    private static String hash = "SHA-256";

    //Charset
    private static String utf8 = "UTF-8";

    //Radix for the hash
    private static final int STRING_RADIX_REPRESENTATION = 16;

    // Initial vector for AES cipher
    private final byte[] initialVectorBytes;

    // Key used for AES cypher
    private final byte[] keyBytes;

    public Encoder(byte[] initialVectorBytes, byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalStateException("Key bytes length should be 32 and its: " + initialVectorBytes.length);
        }

        if (initialVectorBytes.length != 16) {
            throw new IllegalStateException("Initial Vector bytes length should be 16 and its: " + initialVectorBytes.length);
        }

        this.initialVectorBytes = initialVectorBytes;
        this.keyBytes = keyBytes;
    }

    /**
     * hash a string using hash mode.
     * @param name string to hash
     * @return string with the hashed name
     */
    public static String hash(String name) {
        try {
            MessageDigest m = MessageDigest.getInstance(hash);
            m.update(name.getBytes(Charset.forName(utf8)));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(STRING_RADIX_REPRESENTATION);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64(byte[] bytes) {
        return DatatypeConverter.printBase64Binary(bytes);
    }

    public String encode(String what) {
        try {
            return base64(aes(what.getBytes(Charset.forName("UTF-8"))));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Couldnt encode value: " + what, e);
        }
    }

    byte[] aes(byte[] content) {
        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final IvParameterSpec iv = new IvParameterSpec(initialVectorBytes);

            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(content);
        } catch (InvalidKeyException e) {
            System.out.println("Please install JCE's Unlimited Strength Policies for next compilation");
            if (Restrictions.remove())
                return aes(content);
            else throw new RuntimeException("No JCE's policies installed + couldnt bypass them", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unknown exception while trying to encript with aes", e);
        }
    }

}
