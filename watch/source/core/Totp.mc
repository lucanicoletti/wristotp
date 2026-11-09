import Toybox.Cryptography;
import Toybox.Lang;

(:glance)
module Totp {

    function generate(algorithm as Cryptography.HashAlgorithm, secret as ByteArray, timeSeconds as Long, period as Number, digits as Number) as String {
        return hotp(algorithm, secret, timeSeconds / period, digits);
    }

    function secondsRemaining(timeSeconds as Long, period as Number) as Number {
        return period - (timeSeconds % period).toNumber();
    }

    function hotp(algorithm as Cryptography.HashAlgorithm, secret as ByteArray, counter as Long, digits as Number) as String {
        var message = new [8]b;
        for (var i = 7; i >= 0; i--) {
            message[i] = (counter & 0xFF).toNumber();
            counter = counter >> 8;
        }
        var mac = Hmac.compute(algorithm, secret, message);
        var offset = mac[mac.size() - 1] & 0x0F;
        var binary = ((mac[offset] & 0x7F) << 24)
            | ((mac[offset + 1] & 0xFF) << 16)
            | ((mac[offset + 2] & 0xFF) << 8)
            | (mac[offset + 3] & 0xFF);
        var modulus = 1;
        for (var i = 0; i < digits; i++) {
            modulus *= 10;
        }
        return (binary % modulus).format("%0" + digits + "d");
    }
}
