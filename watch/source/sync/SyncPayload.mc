import Toybox.Cryptography;
import Toybox.Lang;
import Toybox.StringUtil;

module SyncPayload {

    function hash(records as Array<Dictionary>) as String {
        var canonical = "";
        for (var i = 0; i < records.size(); i++) {
            var record = records[i];
            canonical += (record["s"] as String) + "|" + record["a"] + "|" + record["d"] + "|" + record["p"] + "\n";
        }
        var hasher = new Cryptography.Hash({:algorithm => Cryptography.HASH_SHA256});
        if (canonical.length() > 0) {
            hasher.update(StringUtil.convertEncodedString(canonical, {
                :fromRepresentation => StringUtil.REPRESENTATION_STRING_PLAIN_TEXT,
                :toRepresentation => StringUtil.REPRESENTATION_BYTE_ARRAY
            }) as ByteArray);
        }
        var hex = StringUtil.convertEncodedString(hasher.digest(), {
            :fromRepresentation => StringUtil.REPRESENTATION_BYTE_ARRAY,
            :toRepresentation => StringUtil.REPRESENTATION_STRING_HEX
        }) as String;
        return hex.toLower();
    }

    function decode(records as Array<Dictionary>) as Array<Dictionary>? {
        var decoded = [] as Array<Dictionary>;
        for (var i = 0; i < records.size(); i++) {
            var record = records[i];
            var secret = Base32.decode(record["s"] as String);
            if (secret == null || secret.size() == 0) {
                return null;
            }
            decoded.add({
                "i" => record["i"],
                "n" => record["n"],
                "s" => secret,
                "a" => record["a"],
                "d" => record["d"],
                "p" => record["p"]
            });
        }
        return decoded;
    }
}
