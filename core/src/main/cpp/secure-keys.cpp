#include <jni.h>

#include <string>
#include <map>

#include "crypto/crypto_wrapper.h"
#include "configurations.h"
#include "extern_consts.h"

#define _default_response ""

std::map<std::string , std::string> _map;
CryptoWrapper crypto_wrapper;

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
    JNIEXPORT jstring JNICALL Java_com_u_securekeys_SecureEnvironment__1getString(JNIEnv *env, jclass instance, jstring key);
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    Configurations configs;

    if (configs.is_safe_to_use()) {
        SECUREKEYS_LOAD_MAP(_map)
        crypto_wrapper.set_aes_key(configs.get_key());
        crypto_wrapper.set_aes_iv(configs.get_initial_vector());
    } else {
        // It shouldnt have anything, but still
        // Remove everything from memory, since its not a safe environment
        _map.clear();
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL Java_com_u_securekeys_SecureEnvironment__1getString
        (JNIEnv *env, jclass instance, jstring key) {
    // Get the hash of the string param
    const char *raw_key = env->GetStringUTFChars(key, 0);
    std::string _key(raw_key);
    std::string hashed_key = crypto_wrapper.encode_key(_key);

    // Release allocated stuff
    env->ReleaseStringUTFChars(key, raw_key);
    env->DeleteLocalRef(key);

    // Check if the map contains the key and return it if exists
    std::string crypted_value = _map[hashed_key];
    std::string value(_default_response);
    if (!crypted_value.empty()) {
        value = crypto_wrapper.decode_value(crypted_value);
    }

    return (env)->NewStringUTF(value.c_str());
}