import Toybox.Application;
import Toybox.Lang;
import Toybox.Test;

(:test)
function pinGateAcceptsCorrectPin(logger as Logger) as Boolean {
    var gate = PinTestSupport.freshGate();
    gate.setPin("2468", 1000);
    return gate.hasPin()
        && gate.verify("2468", 2000) == PinGate.RESULT_OK
        && gate.isUnlockedAt(2010)
        && !gate.isUnlockedAt(2200);
}

(:test)
function pinGateRejectsWrongPinAndCounts(logger as Logger) as Boolean {
    var gate = PinTestSupport.freshGate();
    gate.setPin("2468", 1000);
    return gate.verify("1357", 2000) == PinGate.RESULT_WRONG
        && gate.failures() == 1
        && gate.lockedSecondsAt(2000) == 0;
}

(:test)
function pinGateLocksOutAfterFiveFailures(logger as Logger) as Boolean {
    var gate = PinTestSupport.freshGate();
    gate.setPin("2468", 1000);
    for (var i = 0; i < 5; i++) {
        gate.verify("0000", 2000);
    }
    var lockedFirst = gate.lockedSecondsAt(2000) == 30 && gate.verify("2468", 2010) == PinGate.RESULT_LOCKED;
    gate.verify("0000", 2030);
    var doubled = gate.lockedSecondsAt(2030) == 60;
    return lockedFirst && doubled && gate.verify("2468", 2100) == PinGate.RESULT_OK && gate.failures() == 0;
}

(:test)
function pinGateWipesAccountsAfterTenFailures(logger as Logger) as Boolean {
    var store = new AccountStore();
    store.replaceAccounts([{"i" => "GitHub", "n" => "demo", "s" => OtpTestSupport.bytesOf("12345678901234567890"), "a" => 1, "d" => 6, "p" => 30}] as Array<Dictionary>);
    var gate = PinTestSupport.freshGate();
    gate.setPin("2468", 1000);
    var now = 2000;
    var result = PinGate.RESULT_WRONG;
    for (var i = 0; i < 10; i++) {
        now += 100000;
        result = gate.verify("0000", now);
    }
    var records = Application.Storage.getValue("accounts") as Array?;
    var glance = Application.Storage.getValue("glance") as Array?;
    return result == PinGate.RESULT_WIPED && !gate.hasPin() && records != null && records.size() == 0 && glance != null && glance.size() == 0;
}

(:test)
module PinTestSupport {

    function freshGate() as PinGate {
        var store = new AccountStore();
        store.setPinRequired(true);
        var gate = new PinGate(store);
        gate.clear();
        return gate;
    }
}
