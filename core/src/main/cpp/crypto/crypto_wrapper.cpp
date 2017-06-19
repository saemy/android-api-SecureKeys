//
// Created by Santiago Aguilera on 3/5/17.
//
#include <stdlib.h>
#include "crypto_wrapper.h"
#include "sha256.h"
#include "base64.h"
#include "aes.h"

unsigned int CryptoWrapper::aes_key_size = 256;
unsigned char CryptoWrapper::aes_initial_vector[16] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                                                        0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
unsigned char CryptoWrapper::aes_key[32] = { 0x60,  0x3d, 0xeb, 0x10, 0x15, 0xca, 0x71,
                                             0xbe, 0x2b, 0x73, 0xae, 0xf0, 0x85, 0x7d, 0x77, 0x81, 0x1f, 0x35, 0x2c,
                                             0x07, 0x3b, 0x61, 0x08, 0xd7, 0x2d, 0x98, 0x10, 0xa3, 0x09, 0x14, 0xdf,
                                             0xf4 };

std::string CryptoWrapper::decode_value(std::string value) {
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
    aes_key_setup(aes_key, key_schedule, aes_key_size);

    // Decrypt
    aes_decrypt_cbc(input, src_len, buff, key_schedule, aes_key_size, aes_initial_vector);

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