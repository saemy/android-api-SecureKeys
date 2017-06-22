package com.u.securekeys.internal;

import com.u.securekeys.annotation.SecureConfigurations;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by saguilera on 6/18/17.
 */

public class EncoderTest {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Test
    public void test_SHA256() {
        String message = "test_message";

        String hash = Encoder.hash(message);

        Assert.assertEquals("3b7491dc016ac1a0b2e02372402c861fafa459294087e7cbe09f704d582d931f", hash);
    }

    @Test
    public void test_EncodeAES() {
        // Test array of bytes to crypt
        byte[] arr = {0x01, 0x02, 0x03};

        Encoder encoder = defaultEncoder();
        byte[] aesarr = encoder.aes(arr);

        Assert.assertEquals("F81B0C071652901E54BA6781CED9589D", bytesToHex(aesarr));
    }

    @Test
    public void test_FullCrypt() {
        // Test array of bytes to crypt
        String value = "test_message";

        Encoder encoder = defaultEncoder();
        String encodedValue = encoder.encode(value);

        Assert.assertEquals("MPRSyn8EtW48RrsKM8WFwg==", encodedValue);
    }

    private Encoder defaultEncoder() {
        try {
            byte[] iv = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesInitialVector").getDefaultValue();
            byte[] key = (byte[]) SecureConfigurations.class.getDeclaredMethod("aesKey").getDefaultValue();
            return new Encoder(iv, key);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
