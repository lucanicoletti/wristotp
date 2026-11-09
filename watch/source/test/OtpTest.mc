import Toybox.Cryptography;
import Toybox.Lang;
import Toybox.StringUtil;
import Toybox.Test;

(:test)
function base32DecodesRfcSeed(logger as Logger) as Boolean {
    var decoded = Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
    return decoded != null && decoded.equals(OtpTestSupport.bytesOf("12345678901234567890"));
}

(:test)
function base32IgnoresCaseSpacesAndPadding(logger as Logger) as Boolean {
    var decoded = Base32.decode("my====");
    var spaced = Base32.decode("gezd gnbv gy3t qojq gezd gnbv gy3t qojq");
    return decoded != null && decoded.equals(OtpTestSupport.bytesOf("f"))
        && spaced != null && spaced.equals(OtpTestSupport.bytesOf("12345678901234567890"));
}

(:test)
function base32RejectsInvalidCharacters(logger as Logger) as Boolean {
    return Base32.decode("GEZD1NBV") == null;
}

(:test)
function hotpMatchesRfc4226(logger as Logger) as Boolean {
    var expected = ["755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489"];
    var secret = OtpTestSupport.bytesOf("12345678901234567890");
    var passed = true;
    for (var i = 0; i < expected.size(); i++) {
        var code = Totp.hotp(Cryptography.HASH_SHA1, secret, i.toLong(), 6);
        if (!code.equals(expected[i])) {
            logger.error("counter " + i + ": expected " + expected[i] + ", got " + code);
            passed = false;
        }
    }
    return passed;
}

(:test)
function totpMatchesRfc6238Sha1(logger as Logger) as Boolean {
    return OtpTestSupport.checkRfc6238(logger, Cryptography.HASH_SHA1, "12345678901234567890",
        ["94287082", "07081804", "14050471", "89005924", "69279037", "65353130"]);
}

(:test)
function totpMatchesRfc6238Sha256(logger as Logger) as Boolean {
    return OtpTestSupport.checkRfc6238(logger, Cryptography.HASH_SHA256, "12345678901234567890123456789012",
        ["46119246", "68084774", "67062674", "91819424", "90698825", "77737706"]);
}

(:test)
function totpSecondsRemainingCountsDown(logger as Logger) as Boolean {
    return Totp.secondsRemaining(59l, 30) == 1
        && Totp.secondsRemaining(60l, 30) == 30
        && Totp.secondsRemaining(1111111109l, 30) == 1;
}

(:test)
module OtpTestSupport {

    function checkRfc6238(logger as Logger, algorithm as Cryptography.HashAlgorithm, seed as String, expected as Array<String>) as Boolean {
        var times = [59l, 1111111109l, 1111111111l, 1234567890l, 2000000000l, 20000000000l];
        var secret = bytesOf(seed);
        var passed = true;
        for (var i = 0; i < times.size(); i++) {
            var code = Totp.generate(algorithm, secret, times[i], 30, 8);
            if (!code.equals(expected[i])) {
                logger.error("time " + times[i] + ": expected " + expected[i] + ", got " + code);
                passed = false;
            }
        }
        return passed;
    }

    function bytesOf(text as String) as ByteArray {
        return StringUtil.convertEncodedString(text, {
            :fromRepresentation => StringUtil.REPRESENTATION_STRING_PLAIN_TEXT,
            :toRepresentation => StringUtil.REPRESENTATION_BYTE_ARRAY
        }) as ByteArray;
    }
}
