package com.u.securekeys.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring the plugin.
 *
 * Only one annotation is allowed, more than once will throw an exception in the apt :)
 *
 * Created by saguilera on 3/3/17.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface SecureConfigurations {

    String CLASSPATH = "com.u.securekeys.annotation.SecureConfigurations";

    /**
     * 16 bytes length array for the initial vector. If this is specified and has different size, exception will be thrown
     * and the processor will halt (no keys will be processed)
     * @return Initial vector used for aes with 16 bytes length
     */
    byte[] aesInitialVector() default { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                                        0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d,
                                        0x0e, 0x0f };

    /**
     * 32 bytes length array for the aes key. If this is specified and has different size, exception will be thrown
     * and the processor will halt (no keys will be processed)
     *
     * @return aes key used for aes with 32 bytes length
     */
    byte[] aesKey() default   { 0x60, 0x3d, (byte) 0xeb, 0x10, 0x15, (byte) 0xca, 0x71,
                                (byte) 0xbe, 0x2b, 0x73, (byte) 0xae, (byte) 0xf0, (byte) 0x85, 0x7d,
                                0x77, (byte) 0x81, 0x1f, 0x35, 0x2c, 0x07, 0x3b,
                                0x61, 0x08, (byte) 0xd7, 0x2d, (byte) 0x98, 0x10, (byte) 0xa3,
                                0x09, 0x14, (byte) 0xdf, (byte) 0xf4 };

    /**
     * Generate a random aes key and initial vector depending of the build. This will make on every build unique
     * hyperparameters for the aes cipher
     *
     * <b>Note:</b> If this is enabled, aesKey and aesInitialVector will be discarded (even if they are modified to something
     * of your own)
     *
     * @return boolean if should use a random aes key/iv or not.
     */
    boolean useAesRandomly() default false;

    /**
     * Makes the JNI module return empty strings if the <b>phone</b> is in debug mode
     * @return true if should return empty strings if the <b>phone</b> is in debugging mode
     */
    boolean blockIfDebugging() default false;

    /**
     * Makes the JNI module return empty strings if the APK is in a emulated environment
     * @return true if should return empty strings if the APK is in a emulated environment
     */
    boolean blockIfEmulator() default false;

    /**
     * Makes the JNI module return empty strings if the phone is connected to ADB
     * @return true if should return empty strings if the phone is connected to ADB
     */
    boolean blockIfADB() default false;

    /**
     * Makes the JNI module return empty strings if the environment is not secure
     *
     * Most of times secure=="root", but please note that this contemplates any case
     * which turns off the system property ro.secure
     *
     * @return true if should return empty strings if the environment is not secure
     */
    boolean blockIfPhoneNotSecure() default false;

    /**
     * Makes the JNI module return empty strings if the APK wasnt installed via
     * one of the allowed installers mentioned.
     *
     * For example if we add:
     * allowedInstallers = { "com.android.vending" }
     * Then the SecureEnvironment will only work if the app was installed from the playstore.
     *
     * By default (empty array), this is not validated and 
     * it accepts to be installed from anywhere
     */
    String[] allowedInstallers() default {};

    
    /**
     * Makes the JNI module return empty strings if the APk wasnt signed
     * with this signature
     *
     * The certificate signature here should be obtained like this (else this is very 
     * ambiguous)
     *
     * PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
     * Log.w("DeleteMePlease", String.valueOf(packageInfo.signatures[0].hashCode()));
     *
     * Note that this field is a String, so the outputted hashCode should be wrapped in a String.
     *
     * By default it wont check against the signing certificate
     */
    String certificateSignature default null;

}
