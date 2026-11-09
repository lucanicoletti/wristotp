import Toybox.Application;
import Toybox.Lang;
import Toybox.Test;

(:test)
function accountStoreKeepsOnlyFirstAccountForGlance(logger as Logger) as Boolean {
    var store = new AccountStore();
    store.replaceAccounts([
        {"i" => "GitHub", "n" => "demo", "s" => OtpTestSupport.bytesOf("12345678901234567890"), "a" => 1, "d" => 6, "p" => 30},
        {"i" => "AWS", "n" => "root", "s" => OtpTestSupport.bytesOf("12345678901234567890"), "a" => 1, "d" => 6, "p" => 30}
    ] as Array<Dictionary>);
    var glance = store.getGlanceAccounts();
    return store.getAccounts().size() == 2 && glance.size() == 1 && "GitHub".equals(glance[0].issuer);
}
