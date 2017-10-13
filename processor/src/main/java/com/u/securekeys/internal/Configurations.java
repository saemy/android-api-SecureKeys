package com.u.securekeys.internal;

import java.lang.NullPointerException;

/**
 * Configurations DTO.
 */
public class Configurations {

    private boolean haltDebuggable;
    private boolean haltAdbOn;
    private boolean haltNotSecure;
    private boolean haltEmulator;
    private String[] installers;
    private String certificate;

    private byte aesKey[];
    private byte aesVector[];

    public Configurations() {
    }

    public void setAesKey(byte[] key) {
        aesKey = key;
    }

    public void setAesVector(byte[] vector) {
        aesVector = vector;
    }

    public void setHaltDebuggable(boolean halt) {
        haltDebuggable = halt;
    }

    public void setHaltAdbOn(boolean halt) {
        haltAdbOn = halt;
    }

    public void setHaltNotSecure(boolean halt) {
        haltNotSecure = halt;
    }

    public void setHaltEmulator(boolean halt) {
        haltEmulator = halt;
    }

    public void setInstallers(String[] arr) {
        installers = arr;
    }

    public void setCertificate(String cert) {
        certificate = cert;
    }

    /**
     * Transforms a byte array into a "developer readable" string,
     * eg: [2Bytes] -> "{ 0xXX, 0xYY }"
     * @param arr to transform
     * @return dev readable byte array
     */
    private String byteArrayToString(byte arr[]) {
        String keyString = "{ ";
        for (int i = 0 ; i < arr.length ; i++) {
            keyString += String.format("0x%02X", arr[i]);
            if (i != arr.length - 1) {
                keyString += ", ";
            }
        }
        keyString += " }";
        return keyString;
    }

    /**
     * Get an array of installers as a string
     * @return an array of installers in a single string { "a", "b", ... }
     */
    private String getInstallers() {
        String result = "{";

        if (installers != null) {
            for (int i = 0 ; i < installers.length ; i++) {
                String installer = installers[i];

                result += (" " + wrap(installer));
                if (i != installers.length - 1) {
                    result += ",";
                }
            }
        }

        result += " }";
        return result;
    }

    private String getCertificate() {
        return certificate == null ? "" : certificate;
    }

    /**
     * Wrap a string in quotes
     * @param str to wrap
     * @return wrapped string
     */
    private String wrap(String str) {
        return ("\"" + str + "\"");
    }

    /**
     * Write the DTO into a native header builder
     * @param builder to write the DTO info to.
     */
    public void writeTo(NativeHeaderBuilder builder) {
        if (aesKey == null || aesVector == null) {
            throw new NullPointerException("Missing aesKey/Vector in configurations. Please set them");
        }

        builder.addDefine("SECUREKEYS_HALT_IF_DEBUGGABLE", String.valueOf(haltDebuggable));
        builder.addDefine("SECUREKEYS_HALT_IF_EMULATOR", String.valueOf(haltEmulator));
        builder.addDefine("SECUREKEYS_HALT_IF_ADB_ON", String.valueOf(haltAdbOn));
        builder.addDefine("SECUREKEYS_HALT_IF_PHONE_NOT_SECURE", String.valueOf(haltNotSecure));

        builder.addDefine("SECUREKEYS_INSTALLERS", getInstallers());
        builder.addDefine("SECUREKEYS_SIGNING_CERTIFICATE", wrap(getCertificate()));

        builder.addDefine("SECUREKEYS_AES_KEY", byteArrayToString(aesKey));
        builder.addDefine("SECUREKEYS_AES_INITIAL_VECTOR", byteArrayToString(aesVector));
    }

}
