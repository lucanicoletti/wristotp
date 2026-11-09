import Toybox.Application;
import Toybox.Lang;

(:glance)
class AccountStore {
    private const KEY_ACCOUNTS = "accounts";
    private const KEY_GLANCE = "glance";
    private const KEY_PIN_REQUIRED = "pinRequired";

    public function initialize() {
    }

    public function getAccounts() as Array<Account> {
        return load(KEY_ACCOUNTS);
    }

    public function getGlanceAccounts() as Array<Account> {
        return load(KEY_GLANCE);
    }

    private function load(key as String) as Array<Account> {
        var records = Application.Storage.getValue(key) as Array<Dictionary>?;
        if (records == null || records.size() == 0) {
            records = $.fallbackRecords();
        }
        var accounts = [] as Array<Account>;
        for (var i = 0; i < records.size(); i++) {
            accounts.add(new $.Account(records[i]));
        }
        return accounts;
    }

    public function replaceAccounts(records as Array<Dictionary>) as Void {
        Application.Storage.setValue(KEY_ACCOUNTS, records as Array<Application.Storage.ValueType>);
        Application.Storage.setValue(KEY_GLANCE, (records.size() > 0 ? records.slice(0, 1) : []) as Array<Application.Storage.ValueType>);
    }

    public function isPinRequired() as Boolean {
        return Application.Storage.getValue(KEY_PIN_REQUIRED) == true;
    }

    public function setPinRequired(required as Boolean) as Void {
        Application.Storage.setValue(KEY_PIN_REQUIRED, required);
    }
}
