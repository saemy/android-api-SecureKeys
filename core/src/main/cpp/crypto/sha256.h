//
// Created by Santiago Aguilera on 6/18/17.
//

#ifndef SECUREKEYS_SHA256_H
#define SECUREKEYS_SHA256_H

#include <string>

#define uchar unsigned char // 8-bit byte
#define uint unsigned int // 32-bit word

class SHA256 {
private:
    uchar data[64];
    uint datalen;
    uint bitlen[2];
    uint state[8];

    int unsigned digest_size;

    void transform(uchar data[]);
    void update(uchar data[], uint len);
    void final(uchar hash[]);
public:
    SHA256();
    std::string hash(std::string what);
};


#endif //SECUREKEYS_SHA256_H
