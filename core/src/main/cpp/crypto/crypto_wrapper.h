//
// Created by Santiago Aguilera on 3/5/17.
//
#ifndef SECUREKEYS_CRYPTO_WRAPPER_H
#define SECUREKEYS_CRYPTO_WRAPPER_H

#include <string>

#define AES_WHOLE_KEY_SIZE 256
#define AES_KEY_SIZE 32
#define AES_IV_SIZE 16

class CryptoWrapper {
private:
    unsigned int aes_whole_key_size;
    unsigned char aes_initial_vector[AES_IV_SIZE];
    unsigned char aes_key[AES_KEY_SIZE];

    bool key_applied;
    bool iv_applied;
public:
    CryptoWrapper();
    void set_aes_key(unsigned char *key);
    void set_aes_iv(unsigned char *iv);
    std::string decode_value(std::string value);
    std::string encode_key(std::string key);
};

#endif //SECUREKEYS_CRYPTO_WRAPPER_H
