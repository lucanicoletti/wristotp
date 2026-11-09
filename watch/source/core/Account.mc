import Toybox.Cryptography;
import Toybox.Lang;

(:glance)
class Account {
    public var issuer as String;
    public var name as String;
    public var digits as Number;
    public var period as Number;
    private var _secret as ByteArray;
    private var _algorithm as Cryptography.HashAlgorithm;
    private var _cachedCounter as Long = -1l;
    private var _cachedCode as String = "";

    public function initialize(record as Dictionary) {
        issuer = valueOr(record["i"], "") as String;
        name = valueOr(record["n"], "") as String;
        digits = valueOr(record["d"], 6) as Number;
        period = valueOr(record["p"], 30) as Number;
        _secret = record["s"] as ByteArray;
        _algorithm = record["a"] == 2 ? Cryptography.HASH_SHA256 : Cryptography.HASH_SHA1;
    }

    public function codeAt(timeSeconds as Long) as String {
        var counter = timeSeconds / period;
        if (counter != _cachedCounter) {
            _cachedCode = Totp.hotp(_algorithm, _secret, counter, digits);
            _cachedCounter = counter;
        }
        return _cachedCode;
    }

    public function secondsRemainingAt(timeSeconds as Long) as Number {
        return Totp.secondsRemaining(timeSeconds, period);
    }

    private function valueOr(value as Object?, fallback as Object) as Object {
        return value == null ? fallback : value;
    }
}
