//
// Created by Santiago Aguilera on 6/21/17.
//

#ifndef SECUREKEYS_CONFIGURATIONS_H
#define SECUREKEYS_CONFIGURATIONS_H

#include <jni.h>
#include <map>
#include <string>
#include "crypto/crypto_wrapper.h"

/**
 * Class that holds information about the library configurations
 */
class Configurations {
private:
    // AES information, if the system is flagged as unsafe they wont exist
    static unsigned char aes_key[AES_KEY_SIZE];
    static unsigned char aes_iv[AES_IV_SIZE];

    // If the system is safe or not
    bool safe;

    // Configures aes values
    void configure_aes();

    // Checks system information for detecting the safeness of the system
    void check_debug();
    void check_emulator();
    void check_adb();
    void check_secure_environment();
    void check_installer(JNIEnv *env, jobject &object_context);
    void check_certificate(JNIEnv *env, jobject &object_context);
public:
    // Constructor, requires an Android Application context
    Configurations(JNIEnv *env, jobject &object_context);

    // Getters for aes information
    unsigned char * get_initial_vector();
    unsigned char * get_key();

    // Getter for the safe boolean
    bool is_safe_to_use();
};


#endif //SECUREKEYS_CONFIGURATIONS_H
