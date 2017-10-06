//
// Created by Santiago Aguilera on 6/21/17.
//

#include "configurations.h"
#include "extern_consts.h"
#include <sys/system_properties.h>
#include <unistd.h>
#include <sstream>

unsigned char Configurations::aes_iv[AES_IV_SIZE] = SECUREKEYS_AES_INITIAL_VECTOR;
unsigned char Configurations::aes_key[AES_KEY_SIZE] = SECUREKEYS_AES_KEY;

template < typename T > std::string to_string( const T& n ) {
    std::ostringstream stm;
    stm << n ;
    return stm.str() ;
}

/**
 * True if the property is the expected
 * False if the property is not what was expected
 * False if the property doesnt exist
 * @param property_name
 * @param expected
 */
bool validate_property_contains(std::string property_name, std::string expected) {
    char _response[PROP_VALUE_MAX];
    memset(_response, 0, sizeof(_response));

    int length = __system_property_get(property_name.c_str(), _response);

    if (length == 0) {
        return false;
    }

    std::string _str_response(_response, (unsigned long) length);
    return _str_response.find(expected) != std::string::npos;
}

/**
 * cmdline is a file inside the /proc/<pid>/ folder which has the package name + abi
 * Eg for this testapp: com.u.testappgeneric_x86_64 (im in a Google Pixel with x86_64)
 * @return true if has debug, else false.
 */
bool check_cmdline_has_debug() {
    pid_t pid = getpid();
    char path[64];
    memset(path, 0, sizeof(path));
    sprintf(path, "/proc/%d/cmdline", pid);
    FILE *cmdline = fopen(path, "r");
    if (cmdline) {
        char application_id[64] = { 0 };
        fread(application_id, sizeof(application_id), 1, cmdline);
        fclose(cmdline);

        if (std::string(application_id).find("debug") != std::string::npos) {
            return true;
        }
    }

    return false;
}

/**
 * Initialize the configurations. For more information see the class "SecureConfigurations" in the "annotation"
 * module
 */
Configurations::Configurations(JNIEnv *env, jobject &object_context) : safe(true) {
    check_installer(env, object_context);
    check_certificate(env, object_context);
    check_debug();
    check_adb();
    check_emulator();
    check_secure_environment();

    if (!is_safe_to_use()) {
        memset(aes_iv, 0, sizeof(aes_iv));
        memset(aes_key, 0, sizeof(aes_key));
    }
}

/**
 * For detecting a valid signature we are using hashcodes.
 */
void Configurations::check_certificate(JNIEnv *env, jobject &object_context) {
    std::string certificate(SECUREKEYS_SIGNING_CERTIFICATE);
    if (!certificate.empty()) {
        // Classes we will use
        jclass class_context = env->FindClass("android/content/Context");
        jclass class_package_manager = env->FindClass("android/content/pm/PackageManager");
        jclass class_package_info = env->FindClass("android/content/pm/PackageInfo");
        jclass class_signature = env->FindClass("android/content/pm/Signature");

        // Get package manager
        jmethodID method_get_package_manager = env->GetMethodID(class_context, "getPackageManager", "()Landroid/content/pm/PackageManager;");
        jobject object_package_manager = env->CallObjectMethod(object_context, method_get_package_manager);

        // Get packageName and GET_SIGNATURES params
        jmethodID method_get_package_name = env->GetMethodID(class_context, "getPackageName", "()Ljava/lang/String;");
        jobject object_package_name = env->CallObjectMethod(object_context, method_get_package_name);
        jfieldID field_get_signatures = env->GetStaticFieldID(class_package_manager, "GET_SIGNATURES", "I");
        jint object_get_signatures = env->GetStaticIntField(class_package_manager, field_get_signatures);

        // Get package info with above params
        jmethodID method_get_package_info = env->GetMethodID(class_package_manager, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        jobject object_package_info = env->CallObjectMethod(object_package_manager, method_get_package_info, object_package_name, object_get_signatures);
        
        // Get signatures field (Signature[])
        jfieldID field_signatures = env->GetFieldID(class_package_info, "signatures", "[Landroid/content/pm/Signature;");
        jobjectArray object_array_signatures = (jobjectArray) env->GetObjectField(object_package_info, field_signatures);
        
        // Clean the stack frame
        env->DeleteLocalRef(object_package_manager);
        env->DeleteLocalRef(object_package_name);
        env->DeleteLocalRef(object_package_info);

        bool aux_safe = false;
        int signaturesLength = env->GetArrayLength(object_array_signatures);
        for (int i = 0 ; i < signaturesLength ; i++) {
            jobject object_signature = env->GetObjectArrayElement(object_array_signatures, i);

            // Get hashcode method
            jmethodID method_hash_code = env->GetMethodID(class_signature, "hashCode", "()I");
            jint object_hash_code = env->CallIntMethod(object_signature, method_hash_code);
            int hash_code = (int) object_hash_code;

            if (to_string(hash_code) == certificate) {
                aux_safe = true;
            }

            env->DeleteLocalRef(object_signature);
        }

        env->DeleteLocalRef(object_array_signatures);

        if (!aux_safe) {
            safe = false;
        }
    }
}

/**
 * Checks that the installee is not a snitch
 */
void Configurations::check_installer(JNIEnv *env, jobject &object_context) {
    std::string installers[] = SECUREKEYS_INSTALLERS;

    if (sizeof(installers)) {
        // Find jclass we will interact with
        jclass class_context = env->FindClass("android/content/Context");
        jclass class_package_manager = env->FindClass("android/content/pm/PackageManager");

        // Get the package manager jobject
        jmethodID method_get_package_manager = env->GetMethodID(class_context, "getPackageManager", "()Landroid/content/pm/PackageManager;");
        jobject object_package_manager = env->CallObjectMethod(object_context, method_get_package_manager);

        // Get the methods for getting my package name and the installer package name
        jmethodID method_get_installer_package_name = env->GetMethodID(class_package_manager, "getInstallerPackageName", "(Ljava/lang/String;)Ljava/lang/String;");
        jmethodID method_get_package_name = env->GetMethodID(class_context, "getPackageName", "()Ljava/lang/String;");

        // Obtain my package name
        jobject object_package_name = env->CallObjectMethod(object_context, method_get_package_name);
        // Obtain the installer package name
        jobject object_installer_package_name = (jstring) env->CallObjectMethod(object_package_manager, method_get_installer_package_name, object_package_name);

        // Delete used local references
        env->DeleteLocalRef(object_package_manager);
        env->DeleteLocalRef(object_package_name);

        bool aux_safe = false;

        if (object_installer_package_name) {
            const char *raw_installer_package_name = env->GetStringUTFChars((jstring) object_installer_package_name, 0);
            std::string installer_package_name(raw_installer_package_name);

            for (int i = 0 ; i < sizeof(installers) / sizeof(installers[0]) ; ++i) {
                std::string installer = installers[i];
                if (installer_package_name.size() >= installer.size() && installer_package_name.substr(0, installer.size()) == installer) {
                    aux_safe = true;
                }
            }

            env->ReleaseStringUTFChars((jstring) object_installer_package_name, 0);
            env->DeleteLocalRef(object_installer_package_name);
        }

        if (!aux_safe) {
            safe = false;
        }
    }
}

/**
 * Get AES IV
 */
unsigned char * Configurations::get_initial_vector() {
    return aes_iv;
}

/**
 * Get AES key
 */
unsigned char * Configurations::get_key() {
    return aes_key;
}

bool Configurations::is_safe_to_use() {
    return safe;
}

/**
 * Validates system properties checking if the debug ones are present.
 * Relevant notes:
 * - https://android.googlesource.com/platform/system/core/+/android-4.1.2_r1/init/readme.txt
 * - getprop in adb shell
 * - https://github.com/jacobsoo/AndroidSlides/blob/master/CanSecWest-2013/An%20Android%20Hacker's%20Journey-%20Challenges%20in%20Android%20Security%20Research.pptx
 * - Get inside adb shell and see files and props
 */
void Configurations::check_debug() {
    if (SECUREKEYS_HALT_IF_DEBUGGABLE) {
        if (validate_property_contains("ro.debuggable", "1") ||
            validate_property_contains("ro.kernel.android.checkjni", "1") ||
            validate_property_contains("ro.build.fingerprint", "debug") ||
            validate_property_contains("ro.build.product", "generic") ||
            validate_property_contains("ro.product.device", "generic") ||
            check_cmdline_has_debug()) {
            safe = false;
        }
    }
}

/**
 * Check if the system is an emulator or not
 */
void Configurations::check_emulator() {
    if (SECUREKEYS_HALT_IF_EMULATOR) {
        if (validate_property_contains("ro.kernel.qemu", "1") ||
            validate_property_contains("ro.hardware", "goldfish") ||
            validate_property_contains("ro.hardware", "ranchu") ||
            validate_property_contains("ro.setupwizard.mode", "EMULATOR") ||
            validate_property_contains("ro.build.characteristics", "emulator") ||
            validate_property_contains("qemu.sf.fake_camera", "both") ||
            validate_property_contains("qemu.sf.fake_camera", "back") ||
            validate_property_contains("ro.hardware.audio.primary", "goldfish")) {
            safe = false;
        }
    }
}

/**
 * Check if the phone is connected to adb or not
 */
void Configurations::check_adb() {
    if (SECUREKEYS_HALT_IF_ADB_ON) {
        if (validate_property_contains("sys.usb.state", "adb") ||
            validate_property_contains("sys.usb.config", "adb") ||
            validate_property_contains("qemu.adb.secure", "0") ||
            validate_property_contains("persist.adb.notify", "1") ||
            validate_property_contains("persist.sys.usb.config", "adb")) {
            safe = false;
        }
    }
}

/**
 * Check that the phone is not rooted
 */
void Configurations::check_secure_environment() {
    if (SECUREKEYS_HALT_IF_PHONE_NOT_SECURE) {
        if (validate_property_contains("ro.secure", "0") ||
            validate_property_contains("persist.service.adb.enable", "1")) {
            safe = false;
        }
    }
}
