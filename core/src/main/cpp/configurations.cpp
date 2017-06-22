//
// Created by Santiago Aguilera on 6/21/17.
//

#include "configurations.h"
#include "protocol.h"
#include "crypto/base64.h"
#include <sys/system_properties.h>
#include <unistd.h>

#define FIND(WHAT) find(crypto_wrapper.encode_key(WHAT), map)

unsigned char Configurations::aes_iv[AES_IV_SIZE] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                                                        0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
unsigned char Configurations::aes_key[AES_KEY_SIZE] = { 0x60, 0x3d, 0xeb, 0x10, 0x15, 0xca, 0x71,
                                             0xbe, 0x2b, 0x73, 0xae, 0xf0, 0x85, 0x7d, 0x77, 0x81, 0x1f, 0x35, 0x2c,
                                             0x07, 0x3b, 0x61, 0x08, 0xd7, 0x2d, 0x98, 0x10, 0xa3, 0x09, 0x14, 0xdf,
                                             0xf4 };

/**
 * Find something in a map
 * @param what to find
 * @param where to look
 * @return string with the value found, or empty of nothing.
 */
std::string find(std::string what, std::map<std::string, std::string> &where) {
    return where[what];
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
 * @param map
 */
Configurations::Configurations(std::map<std::string, std::string> &map) : safe(true) {
    configure_aes(map);
    check_debug(map);
    check_adb(map);
    check_emulator(map);
    check_secure_environment(map);
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

void Configurations::configure_aes(std::map<std::string, std::string> &map) {
    // Get aes values
    std::string map_aes_rnd_key = FIND(SECUREKEYS_AES_RANDOM_SEED);

    if (!map_aes_rnd_key.empty()) {
        // Create from random seed
        unsigned char map_aes_seed[map_aes_rnd_key.length()];
        memcpy((char*) map_aes_seed, map_aes_rnd_key.c_str(), sizeof(map_aes_seed));

        for (int i = 0 ; i < AES_KEY_SIZE; ++i) {
            if (i < AES_IV_SIZE) {
                aes_iv[i] = map_aes_seed[i];
            }
            aes_key[i] = map_aes_seed[map_aes_rnd_key.length() - i - 1];
        }
    } else {
        // Check if unique values are present, else use defaults
        std::string map_aes_key = FIND(SECUREKEYS_AES_KEY);
        std::string map_aes_iv = FIND(SECUREKEYS_AES_INITIAL_VECTOR);

        if (!map_aes_key.empty()) {
            std::string map_aes_key_b64 = base64_decode(map_aes_key);
            memcpy((char*) aes_key, map_aes_key_b64.c_str(), sizeof(aes_key));
        }
        if (!map_aes_iv.empty()) {
            std::string map_aes_iv_b64 = base64_decode(map_aes_iv);
            memcpy((char*) aes_iv, map_aes_iv_b64.c_str(), sizeof(aes_iv));
        }
    }
}

/**
 * Validates system properties checking if the debug ones are present.
 * Relevant notes:
 * - https://android.googlesource.com/platform/system/core/+/android-4.1.2_r1/init/readme.txt
 * - getprop in adb shell
 * - https://github.com/jacobsoo/AndroidSlides/blob/master/CanSecWest-2013/An%20Android%20Hacker's%20Journey-%20Challenges%20in%20Android%20Security%20Research.pptx
 * - Get inside adb shell and see files and props
 *
 * @param map
 */
void Configurations::check_debug(std::map<std::string, std::string> &map) {
    if (!FIND(SECUREKEYS_HALT_IF_DEBUGGABLE).empty()) {
        if (validate_property_contains("ro.debuggable", "1") ||
            validate_property_contains("ro.kernel.android.checkjni", "1") ||
            validate_property_contains("ro.build.fingerprint", "debug") ||
            validate_property_contains("ro.build.product", "generic") ||
            validate_property_contains("init.svc.debuggerd", "running") ||
            validate_property_contains("ro.product.device", "generic") ||
            check_cmdline_has_debug()) {
            safe = false;
        }
    }
}

void Configurations::check_emulator(std::map<std::string, std::string> &map) {
    if (!FIND(SECUREKEYS_HALT_IF_EMULATOR).empty()) {
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

void Configurations::check_adb(std::map<std::string, std::string> &map) {
    if (!FIND(SECUREKEYS_HALT_IF_ADB_ON).empty()) {
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

void Configurations::check_secure_environment(std::map<std::string, std::string> &map) {
    if (!FIND(SECUREKEYS_HALT_IF_PHONE_NOT_SECURE).empty()) {
        if (validate_property_contains("ro.secure", "0") ||
            validate_property_contains("persist.service.adb.enable", "1")) {
            safe = false;
        }
    }
}
