//
// Created by Santiago Aguilera on 3/5/17.
//
#include <stdlib.h>
#include "crypto_wrapper.h"
#include "sha256.h"
#include "base64.h"
#include "aes.h"

std::string CryptoWrapper::decode_value(std::string value) {
    if (!key_applied || !iv_applied) {
        throw "Missing aes_key || aes_iv";
    }

    std::string base64_decode_string = base64_decode(value);

    // Prepare data for aes
    unsigned int len = base64_decode_string.length();
    unsigned int src_len = len;

    // Copy data input to ashmem buffer
    char *data = &base64_decode_string[0];
    unsigned char *input = (unsigned char *) malloc(src_len);
    memset(input, 0, src_len);
    memcpy(input, data, len);

    unsigned char * buff = (unsigned char*) malloc(src_len);
    if (!buff) {
        free(input);
        throw "Couldnt assign memmory for buffer inside decode";
    }
    memset(buff, src_len, 0);

    // Set key and iv
    unsigned int key_schedule[AES_BLOCK_SIZE * 4] = { 0 };
    aes_key_setup(aes_key, key_schedule, aes_whole_key_size);

    // Decrypt
    aes_decrypt_cbc(input, src_len, buff, key_schedule, aes_whole_key_size, aes_initial_vector);

    // Read padding assigned from last byte, remove it if exist.
    unsigned char *ptr = buff;
    ptr += (src_len - 1);
    unsigned int padding_len = (unsigned int) *ptr;
    if (padding_len > 0 && padding_len <= AES_BLOCK_SIZE) {
        src_len -= padding_len;
    }

    // Interpret it as a string
    std::string result(reinterpret_cast<char const*>(buff), src_len);

    // Release ashmem
    free(input);
    free(buff);

    return result;
}

std::string CryptoWrapper::encode_key(std::string key) {
    SHA256 hasher;
    return hasher.hash(key);
}

CryptoWrapper::CryptoWrapper() :
    aes_whole_key_size(AES_WHOLE_KEY_SIZE), key_applied(false), iv_applied(false) {
}

void CryptoWrapper::set_aes_key(unsigned char *key) {
    memcpy(aes_key, key, sizeof(unsigned char) * AES_KEY_SIZE);
    key_applied = true;
}

void CryptoWrapper::set_aes_iv(unsigned char *iv) {
    memcpy(aes_initial_vector, iv, sizeof(unsigned char) * AES_IV_SIZE);
    iv_applied = true;
}