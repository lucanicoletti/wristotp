import Toybox.Cryptography;
import Toybox.Lang;

(:glance)
module Hmac {
    const BLOCK_SIZE = 64;

    function compute(algorithm as Cryptography.HashAlgorithm, key as ByteArray, message as ByteArray) as ByteArray {
        if (key.size() > BLOCK_SIZE) {
            key = hash(algorithm, key);
        }
        var innerPad = new [BLOCK_SIZE]b;
        var outerPad = new [BLOCK_SIZE]b;
        for (var i = 0; i < BLOCK_SIZE; i++) {
            var k = i < key.size() ? key[i] : 0;
            innerPad[i] = k ^ 0x36;
            outerPad[i] = k ^ 0x5C;
        }
        return hash(algorithm, outerPad.addAll(hash(algorithm, innerPad.addAll(message))));
    }

    function hash(algorithm as Cryptography.HashAlgorithm, data as ByteArray) as ByteArray {
        var hasher = new Cryptography.Hash({:algorithm => algorithm});
        hasher.update(data);
        return hasher.digest();
    }
}
