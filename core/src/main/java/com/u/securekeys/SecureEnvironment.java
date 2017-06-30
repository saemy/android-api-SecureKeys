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

    private static final String ENV_LIBRARY_NAME = "secure-keys";

    private static final long NAN_LONG = -1;
    private static final String NAN_STRING = "";

    static {
        System.loadLibrary(ENV_LIBRARY_NAME);
    }

    private SecureEnvironment() throws IllegalAccessException {
        throw new IllegalAccessException("This object cant be instantiated");
    }

    public static void initialize(@NonNull Context context) {
        _init(context.getApplicationContext());
    }

    public static @NonNull String getString(@NonNull String key) {
        if (key.isEmpty()) {
            return NAN_STRING;
        }

        return _getString(key);
    }

    public static long getLong(@NonNull String key) {
        String value = getString(key);

        if (value.isEmpty()) {
            return NAN_LONG;
        }

        return Long.valueOf(value);
    }

    public static double getDouble(@NonNull String key) {
        String value = getString(key);

        if (value.isEmpty()) {
            return NAN_LONG;
        }

        return Double.valueOf(value);
    }

    @Keep
    private static native String _getString(String key);
    @Keep
    private static native void _init(Context context);

}
