//
// Created by Santiago Aguilera on 6/21/17.
//

#ifndef SECUREKEYS_CONFIGURATIONS_H
#define SECUREKEYS_CONFIGURATIONS_H

#include <jni.h>
#include <map>
#include <string>
#include "crypto/crypto_wrapper.h"

class Configurations {
private:
    static unsigned char aes_key[AES_KEY_SIZE];
    static unsigned char aes_iv[AES_IV_SIZE];
    bool safe;

    void configure_aes();
    void check_debug();
    void check_emulator();
    void check_adb();
    void check_secure_environment();
    void check_installer(JNIEnv *env, jobject &object_context);
    void check_certificate(JNIEnv *env, jobject &object_context);
public:
    Configurations(JNIEnv *env, jobject &object_context);
    unsigned char * get_initial_vector();
    unsigned char * get_key();
    bool is_safe_to_use();
};


#endif //SECUREKEYS_CONFIGURATIONS_H
