package com.u.securekeys.internal;

import java.lang.NullPointerException;

public class Configurations {

    private boolean haltDebuggable;
    private boolean haltAdbOn;
    private boolean haltNotSecure;
    private boolean haltEmulator;

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

    public void writeTo(NativeHeaderBuilder builder) {
        if (aesKey == null || aesVector == null) {
            throw new NullPointerException("Missing aesKey/Vector in configurations. Please set them");
        }

        builder.addDefine("SECUREKEYS_HALT_IF_DEBUGGABLE", String.valueOf(haltDebuggable));
        builder.addDefine("SECUREKEYS_HALT_IF_EMULATOR", String.valueOf(haltEmulator));
        builder.addDefine("SECUREKEYS_HALT_IF_ADB_ON", String.valueOf(haltAdbOn));
        builder.addDefine("SECUREKEYS_HALT_IF_PHONE_NOT_SECURE", String.valueOf(haltNotSecure));

        builder.addDefine("SECUREKEYS_AES_KEY", byteArrayToString(aesKey));
        builder.addDefine("SECUREKEYS_AES_INITIAL_VECTOR", byteArrayToString(aesVector));
    }

}