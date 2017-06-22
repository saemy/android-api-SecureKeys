package com.u.securekeys.internal;

/**
 * Created by saguilera on 6/20/17.
 */
public final class Protocol {

   /**
    * THIS FILE HAS TO BE IDENTICAL TO THE protocol.h IN CORE-JNI MODULE.
    */

   public static final String AES_RANDOM_SEED = "_securekeys_using_aes_random_key_";
   public static final String AES_KEY = "_securekeys_using_aes_key_";
   public static final String AES_INITIAL_VECTOR = "_securekeys_using_aes_initial_vector_";

   public static final String HALT_IF_DEBUGGABLE = "_securekeys_halt_if_debuggable_";
   public static final String HALT_IF_EMULATOR = "_securekeys_halt_if_emulator_exists_";
   public static final String HALT_IF_ADB_ON = "_securekeys_halt_if_adb_is_on_";
   public static final String HALT_IF_PHONE_NOT_SECURE = "_securekeys_halt_if_phone_is_not_secure_";


}
