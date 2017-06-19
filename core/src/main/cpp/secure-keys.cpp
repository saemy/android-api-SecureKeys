#include <jni.h>

#include <string>
#include <map>

#include "crypto/crypto_wrapper.h"

#define _default_response ""

std::map<std::string , std::string> _map;

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
    JNIEXPORT jstring JNICALL Java_com_u_securekeys_SecureEnvironment__1getString(JNIEnv *env, jclass instance, jstring key);
    JNIEXPORT void JNICALL Java_com_u_securekeys_SecureEnvironment__1putEntry(JNIEnv *env, jclass instance, jobject key, jobject value);
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_u_securekeys_SecureEnvironment__1putEntry
        (JNIEnv *env, jclass instance, jobject key, jobject value) {
    const char *raw_key = env->GetStringUTFChars((jstring) key, 0);
    const char *raw_value = env->GetStringUTFChars((jstring) value, 0);

    std::string _key(raw_key);
    std::string _value(raw_value);

    _map[_key] = _value;

    env->ReleaseStringUTFChars((jstring) key, raw_key);
    env->ReleaseStringUTFChars((jstring) value, raw_value);
}

JNIEXPORT jstring JNICALL Java_com_u_securekeys_SecureEnvironment__1getString
        (JNIEnv *env, jclass instance, jstring key) {
    CryptoWrapper crypto;

    // Get the hash of the string param
    const char *raw_key = env->GetStringUTFChars(key, 0);
    std::string _key(raw_key);
    std::string hashed_key = crypto.encode_key(_key);

    // Release allocated stuff
    env->ReleaseStringUTFChars(key, raw_key);
    env->DeleteLocalRef(key);

    // Check if the map contains the key and return it if exists
    std::string crypted_value = _map[hashed_key];
    std::string value(_default_response);
    if (!crypted_value.empty()) {
        value = crypto.decode_value(crypted_value);
    }

    return (env)->NewStringUTF(value.c_str());
}