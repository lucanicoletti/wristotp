import Toybox.Lang;
import Toybox.Test;

(:test)
function syncHashMatchesCompanion(logger as Logger) as Boolean {
    var records = [
        {"i" => "GitHub", "n" => "demo", "s" => "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "a" => 1, "d" => 6, "p" => 30},
        {"i" => "Example", "n" => "8 digits", "s" => "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA", "a" => 2, "d" => 8, "p" => 30}
    ] as Array<Dictionary>;
    var hash = SyncPayload.hash(records);
    logger.debug("hash = " + hash);
    return hash.equals("d775e0cef4199700880e864acb242831133cd0bc0c0bcde360f5235bd7eb215d");
}

(:test)
function syncHashOfEmptyListMatchesCompanion(logger as Logger) as Boolean {
    return SyncPayload.hash([] as Array<Dictionary>).equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
}

(:test)
function syncDecodeTurnsSecretsIntoBytes(logger as Logger) as Boolean {
    var decoded = SyncPayload.decode([
        {"i" => "GitHub", "n" => "demo", "s" => "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "a" => 1, "d" => 6, "p" => 30}
    ] as Array<Dictionary>);
    return decoded != null
        && (decoded[0]["s"] as ByteArray).equals(OtpTestSupport.bytesOf("12345678901234567890"))
        && "GitHub".equals(decoded[0]["i"]);
}

(:test)
function syncDecodeRejectsInvalidSecret(logger as Logger) as Boolean {
    return SyncPayload.decode([
        {"i" => "Bad", "n" => "demo", "s" => "NOT-BASE32!", "a" => 1, "d" => 6, "p" => 30}
    ] as Array<Dictionary>) == null;
}
