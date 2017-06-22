#include <jni.h>

#include <string>
#include <map>

#include "crypto/crypto_wrapper.h"
#include "configurations.h"

#define _default_response ""

std::map<std::string , std::string> _map;
CryptoWrapper crypto_wrapper;

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
    JNIEXPORT jstring JNICALL Java_com_u_securekeys_SecureEnvironment__1getString(JNIEnv *env, jclass instance, jstring key);
};

void put_entry(JNIEnv *env, jstring &key, jstring &value) {
    const char *raw_key = env->GetStringUTFChars(key, 0);
    const char *raw_value = env->GetStringUTFChars(value, 0);

    std::string _key(raw_key);
    std::string _value(raw_value);

    _map[_key] = _value;

    env->ReleaseStringUTFChars((jstring) key, raw_key);
    env->ReleaseStringUTFChars((jstring) value, raw_value);
}

void load_map(JNIEnv *env) {
    jclass class_ProcessedMap = (env)->FindClass("android/util/SCCache");

    if (class_ProcessedMap != NULL) {
        jclass class_HashMap = (env)->FindClass("java/util/HashMap");
        jclass class_Set = (env)->FindClass("java/util/Set");
        jclass class_MapEntry = (env)->FindClass("java/util/Map$Entry");

        jobject object_ProcessedValuesHashMap;
        jobject object_ProcessedValuesEntrySet;
        jobjectArray object_ProcessedValuesArray;

        // Get the processed map elements from the class
        jmethodID method_Retrieve = env->GetStaticMethodID(class_ProcessedMap, "getElements", "()Ljava/util/HashMap;");
        object_ProcessedValuesHashMap = env->CallStaticObjectMethod(class_ProcessedMap, method_Retrieve);

        // Get the set with all the elements in the hashmap
        jmethodID method_EntrySet = env->GetMethodID(class_HashMap, "entrySet", "()Ljava/util/Set;");
        object_ProcessedValuesEntrySet = env->CallObjectMethod(object_ProcessedValuesHashMap, method_EntrySet);

        // Get the set as an array
        jmethodID method_EntrySetToArray = env->GetMethodID(class_Set, "toArray", "()[Ljava/lang/Object;");
        object_ProcessedValuesArray = (jobjectArray) env->CallObjectMethod(object_ProcessedValuesEntrySet,
                                                                           method_EntrySetToArray);

        // Get size of array
        jsize size = env->GetArrayLength(object_ProcessedValuesArray);

        // Clean local table in case we can overflow it
        env->DeleteLocalRef(object_ProcessedValuesHashMap);
        env->DeleteLocalRef(object_ProcessedValuesEntrySet);

        // Start iterating inside it
        for (int i = 0; i < size; ++i) {
            // Get Map.Entry object from it
            jobject object_MapEntryElement = env->GetObjectArrayElement(object_ProcessedValuesArray, i);

            // Get KEY field
            jmethodID method_GetKey = env->GetMethodID(class_MapEntry, "getKey", "()Ljava/lang/Object;");
            jstring object_KeyString = (jstring) env->CallObjectMethod(object_MapEntryElement, method_GetKey);

            // Get VALUE field
            jmethodID method_GetValue = env->GetMethodID(class_MapEntry, "getValue", "()Ljava/lang/Object;");
            jstring object_ValueString = (jstring) env->CallObjectMethod(object_MapEntryElement, method_GetValue);

            // Put entries in the map
            put_entry(env, object_KeyString, object_ValueString);

            // Clean table so we dont overflow it
            env->DeleteLocalRef(object_KeyString);
            env->DeleteLocalRef(object_ValueString);
            env->DeleteLocalRef(object_MapEntryElement);
        }
    } else {
        env->ExceptionClear();
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    load_map(env);

    Configurations configs(_map);

    if (configs.is_safe_to_use()) {
        crypto_wrapper.set_aes_key(configs.get_key());
        crypto_wrapper.set_aes_iv(configs.get_initial_vector());
    } else {
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