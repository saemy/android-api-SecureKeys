//
// Created by Santiago Aguilera on 6/21/17.
//

#ifndef SECUREKEYS_EXTERN_CONSTS_H
#define SECUREKEYS_EXTERN_CONSTS_H

#include <map>
#include <string>

#define SECUREKEYS_LOAD_MAP(_map) \
    _map["fdce8e4a65b70d186bd77cba2e0c580dcf1c6497da9f1b70eed849497e1f8ba2"] = "jUvAlWYtbJJXOB5PWy1NMsgtAjOcBYdZpSgWcvBjnfwXtmyCsMFnPHeM4CrLdYPO2xmk2IAnOGhlsVn55eV6wA=="; \
    _map["ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"] = "dtqf2xd4THshib3s3WONaw=="; \
    _map["3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d"] = "qFsTXgKSFsR0ZVFTmMF3Nw=="; \
    _map["2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6"] = "ZhGyX8xxGHmjZWQZoJYo8g=="; \
    _map["18ac3e7343f016890c510e93f935261169d9e3f565436429830faf0934f4f8e4"] = "wb0QngK3kUM695MTn3tIHA=="; \
    _map["909106f80c2138a546a0bca1f81cc495014d090fd1d748063b720eb7247cc400"] = "KgnNKwQHcHh2SxdsovS/DQ=="; \
    _map["c116a63b963406228c5364568016e230da806aa9554265811bd285bdb287657b"] = "Zt27YkzEBI7vp8JnLXUZ1A==";

#define SECUREKEYS_AES_INITIAL_VECTOR { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
#define SECUREKEYS_AES_KEY { 0x60, 0x3d, 0xeb, 0x10, 0x15, 0xca, 0x71, 0xbe, 0x2b, 0x73, 0xae, 0xf0, 0x85, 0x7d, 0x77, 0x81, 0x1f, 0x35, 0x2c, 0x07, 0x3b, 0x61, 0x08, 0xd7, 0x2d, 0x98, 0x10, 0xa3, 0x09, 0x14, 0xdf, 0xf4 }

#define SECUREKEYS_HALT_IF_DEBUGGABLE false
#define SECUREKEYS_HALT_IF_EMULATOR false
#define SECUREKEYS_HALT_IF_ADB_ON true
#define SECUREKEYS_HALT_IF_PHONE_NOT_SECURE false

#endif //SECUREKEYS_EXTERN_CONSTS_H
