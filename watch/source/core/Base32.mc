import Toybox.Lang;

(:glance)
module Base32 {

    function decode(text as String) as ByteArray? {
        var bytes = []b;
        var buffer = 0;
        var bits = 0;
        var chars = text.toUpper().toCharArray();
        for (var i = 0; i < chars.size(); i++) {
            var c = chars[i];
            if (c == ' ' || c == '-' || c == '=') {
                continue;
            }
            var value = valueOf(c);
            if (value == null) {
                return null;
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                bytes.add((buffer >> bits) & 0xFF);
                buffer &= (1 << bits) - 1;
            }
        }
        return bytes;
    }

    function valueOf(c as Char) as Number? {
        if (c >= 'A' && c <= 'Z') {
            return c.toNumber() - 'A'.toNumber();
        }
        if (c >= '2' && c <= '7') {
            return c.toNumber() - '2'.toNumber() + 26;
        }
        return null;
    }
}
