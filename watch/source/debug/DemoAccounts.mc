import Toybox.Lang;

(:debug :glance)
function fallbackRecords() as Array<Dictionary> {
    return [
        {"i" => "GitHub", "n" => "demo@example.com", "s" => Base32.decode("JBSWY3DPEHPK3PXP"), "a" => 1, "d" => 6, "p" => 30},
        {"i" => "AWS", "n" => "root", "s" => Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"), "a" => 1, "d" => 6, "p" => 30},
        {"i" => "Example SHA-256", "n" => "8 digits", "s" => Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA"), "a" => 2, "d" => 8, "p" => 30},
        {"i" => "Google", "n" => "demo@example.com", "s" => Base32.decode("KRSXG5CTMVRXEZLUKN2XAZLSKNSWG4TFOQ"), "a" => 1, "d" => 6, "p" => 30},
        {"i" => "Dropbox", "n" => "demo", "s" => Base32.decode("MFRGGZDFMZTWQ2LKNNWG23TPOBYXE43U"), "a" => 1, "d" => 6, "p" => 30},
        {"i" => "Slack", "n" => "workspace", "s" => Base32.decode("ON2HE2LOM4QGC3DQNBQWEZLU"), "a" => 1, "d" => 6, "p" => 30}
    ] as Array<Dictionary>;
}

(:release :glance)
function fallbackRecords() as Array<Dictionary> {
    return [] as Array<Dictionary>;
}
