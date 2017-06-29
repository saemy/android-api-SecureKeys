//
// Created by Santiago Aguilera on 6/21/17.
//

#ifndef SECUREKEYS_CONFIGURATIONS_H
#define SECUREKEYS_CONFIGURATIONS_H

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
public:
    Configurations();
    unsigned char * get_initial_vector();
    unsigned char * get_key();
    bool is_safe_to_use();
};


#endif //SECUREKEYS_CONFIGURATIONS_H
