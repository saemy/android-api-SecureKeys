//
// Created by Santiago Aguilera on 6/21/17.
//

#include "configurations.h"
#include "extern_consts.h"
#include <sys/system_properties.h>
#include <unistd.h>

unsigned char Configurations::aes_iv[AES_IV_SIZE] = SECUREKEYS_AES_INITIAL_VECTOR;
unsigned char Configurations::aes_key[AES_KEY_SIZE] = SECUREKEYS_AES_KEY;

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
Configurations::Configurations() : safe(true) {
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

void Configurations::check_adb() {
    if (SECUREKEYS_HALT_IF_ADB_ON) {
        if (validate_property_contains("sys.usb.state", "adb") ||
            validate_property_contains("sys.usb.config", "adb") ||
            validate_property_contains("ro.adb.secure", "1") ||
            validate_property_contains("qemu.adb.secure", "0") ||
            validate_property_contains("persist.adb.notify", "1") ||
            validate_property_contains("persist.sys.usb.config", "adb")) {
            safe = false;
        }
    }
}

void Configurations::check_secure_environment() {
    if (SECUREKEYS_HALT_IF_PHONE_NOT_SECURE) {
        if (validate_property_contains("ro.secure", "0") ||
            validate_property_contains("persist.service.adb.enable", "1")) {
            safe = false;
        }
    }
}
