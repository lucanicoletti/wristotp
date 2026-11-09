import Toybox.Application;
import Toybox.Cryptography;
import Toybox.Lang;
import Toybox.StringUtil;

class PinGate {
    enum VerifyResult {
        RESULT_OK,
        RESULT_WRONG,
        RESULT_LOCKED,
        RESULT_WIPED
    }

    public static const MIN_LENGTH = 4;
    public static const MAX_LENGTH = 6;

    private const FAILURES_BEFORE_LOCKOUT = 5;
    private const FAILURES_BEFORE_WIPE = 10;
    private const FIRST_LOCKOUT_SECONDS = 30;
    private const UNLOCK_GRACE_SECONDS = 120;

    private const KEY_SALT = "pinSalt";
    private const KEY_HASH = "pinHash";
    private const KEY_FAILURES = "pinFailures";
    private const KEY_LOCKED_UNTIL = "pinLockedUntil";
    private const KEY_UNLOCKED_UNTIL = "pinUnlockedUntil";

    private var _store as AccountStore;

    public function initialize(store as AccountStore) {
        _store = store;
    }

    public function isRequired() as Boolean {
        return _store.isPinRequired();
    }

    public function hasPin() as Boolean {
        return Application.Storage.getValue(KEY_HASH) != null;
    }

    public function isUnlockedAt(now as Number) as Boolean {
        var until = Application.Storage.getValue(KEY_UNLOCKED_UNTIL) as Number?;
        return until != null && now < until;
    }

    public function lockedSecondsAt(now as Number) as Number {
        var until = Application.Storage.getValue(KEY_LOCKED_UNTIL) as Number?;
        if (until == null || now >= until) {
            return 0;
        }
        return until - now;
    }

    public function failures() as Number {
        var count = Application.Storage.getValue(KEY_FAILURES) as Number?;
        return count == null ? 0 : count;
    }

    public function setPin(pin as String, now as Number) as Void {
        var salt = Cryptography.randomBytes(16);
        Application.Storage.setValue(KEY_SALT, salt);
        Application.Storage.setValue(KEY_HASH, hash(salt, pin));
        Application.Storage.setValue(KEY_FAILURES, 0);
        Application.Storage.deleteValue(KEY_LOCKED_UNTIL);
        Application.Storage.setValue(KEY_UNLOCKED_UNTIL, now + UNLOCK_GRACE_SECONDS);
    }

    public function verify(pin as String, now as Number) as VerifyResult {
        if (lockedSecondsAt(now) > 0) {
            return RESULT_LOCKED;
        }
        var salt = Application.Storage.getValue(KEY_SALT) as ByteArray?;
        var expected = Application.Storage.getValue(KEY_HASH) as String?;
        if (salt != null && expected != null && expected.equals(hash(salt, pin))) {
            Application.Storage.setValue(KEY_FAILURES, 0);
            Application.Storage.deleteValue(KEY_LOCKED_UNTIL);
            Application.Storage.setValue(KEY_UNLOCKED_UNTIL, now + UNLOCK_GRACE_SECONDS);
            return RESULT_OK;
        }
        var count = failures() + 1;
        Application.Storage.setValue(KEY_FAILURES, count);
        if (count >= FAILURES_BEFORE_WIPE) {
            _store.replaceAccounts([] as Array<Dictionary>);
            clear();
            return RESULT_WIPED;
        }
        if (count >= FAILURES_BEFORE_LOCKOUT) {
            var lockout = FIRST_LOCKOUT_SECONDS;
            for (var i = FAILURES_BEFORE_LOCKOUT; i < count; i++) {
                lockout *= 2;
            }
            Application.Storage.setValue(KEY_LOCKED_UNTIL, now + lockout);
        }
        return RESULT_WRONG;
    }

    public function clear() as Void {
        Application.Storage.deleteValue(KEY_SALT);
        Application.Storage.deleteValue(KEY_HASH);
        Application.Storage.deleteValue(KEY_FAILURES);
        Application.Storage.deleteValue(KEY_LOCKED_UNTIL);
        Application.Storage.deleteValue(KEY_UNLOCKED_UNTIL);
    }

    private function hash(salt as ByteArray, pin as String) as String {
        var hasher = new Cryptography.Hash({:algorithm => Cryptography.HASH_SHA256});
        hasher.update(salt);
        hasher.update(StringUtil.convertEncodedString(pin, {
            :fromRepresentation => StringUtil.REPRESENTATION_STRING_PLAIN_TEXT,
            :toRepresentation => StringUtil.REPRESENTATION_BYTE_ARRAY
        }) as ByteArray);
        return StringUtil.convertEncodedString(hasher.digest(), {
            :fromRepresentation => StringUtil.REPRESENTATION_BYTE_ARRAY,
            :toRepresentation => StringUtil.REPRESENTATION_STRING_HEX
        }) as String;
    }
}
