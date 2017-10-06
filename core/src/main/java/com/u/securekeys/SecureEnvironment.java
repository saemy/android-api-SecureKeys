package com.u.securekeys;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

/**
 * Bridge between native and java for accessing secure keys.
 *
 * Created by saguilera on 3/3/17.
 */
public final class SecureEnvironment {

    /**
     * Native library name to initialize
     */
    private static final String ENV_LIBRARY_NAME = "secure-keys";

    /**
     * Default not-found variables
     */
    private static final long NAN_LONG = -1;
    private static final String NAN_STRING = "";

    /**
     * Initialize the system library
     */
    static {
        System.loadLibrary(ENV_LIBRARY_NAME);
    }

    /**
     * Private constructor, avoid instantiating this class, else it will throw an IllegalAccessException
     * @throws IllegalAccessException if instantiation is tried
     */
    private SecureEnvironment() throws IllegalAccessException {
        throw new IllegalAccessException("This object cant be instantiated");
    }

    /**
     * Mandatory method to initialize the library environment. This will check the given configurations
     * and initialize the native environment with it
     *
     * <b>Make sure you only initialize the environment once</b>
     *
     * @param context of android, either activity or application
     */
    public static void initialize(@NonNull Context context) {
        _init(context.getApplicationContext());
    }

    /**
     * Get a value from the native environment as a String
     * @param key which was used for referencing the value
     * @return the value which was stored with the given key, else an empty string if theres none or the environment isnt secure
     */
    public static @NonNull String getString(@NonNull String key) {
        if (key.isEmpty()) {
            return NAN_STRING;
        }

        return _getString(key);
    }

    /**
     * Get a value from the native environment as a Long
     * @param key which was used for referencing the value
     * @return the value which was stored with the given key, else -1 if theres none or the environment isnt secure
     */
    public static long getLong(@NonNull String key) {
        String value = getString(key);

        if (value.isEmpty()) {
            return NAN_LONG;
        }

        return Long.valueOf(value);
    }

    /**
     * Get a value from the native environment as a Double
     * @param key which was used for referencing the value
     * @return the value which was stored with the given key, else -1 if theres none or the environment isnt secure
     */
    public static double getDouble(@NonNull String key) {
        String value = getString(key);

        if (value.isEmpty()) {
            return NAN_LONG;
        }

        return Double.valueOf(value);
    }

    /**
     * Native method for obtaining a String value from a key
     * @param key to reference the value
     * @return Empty string if not found or the environment is unsecure, else the value stored for that key
     */
    @Keep
    private static native String _getString(String key);

    /**
     * Initialize the native environment with the given configurations
     * @param context of an android Application
     */
    @Keep
    private static native void _init(Context context);

}
