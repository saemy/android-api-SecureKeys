//
// Created by Santiago Aguilera on 3/5/17.
//
#ifndef SECUREKEYS_CRYPTO_WRAPPER_H
#define SECUREKEYS_CRYPTO_WRAPPER_H

#include "jni.h"
#include <string>

class CryptoWrapper {
private:
    static unsigned int aes_key_size;
    static unsigned char aes_initial_vector[16];
    static unsigned char aes_key[32];

public:
    std::string decode_value(std::string value);
    std::string encode_key(std::string key);
};

#endif //SECUREKEYS_CRYPTO_WRAPPER_H
