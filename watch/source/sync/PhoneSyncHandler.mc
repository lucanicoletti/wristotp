import Toybox.Communications;
import Toybox.Lang;
import Toybox.WatchUi;

class PhoneSyncHandler {
    private var _store as AccountStore;
    private var _onSynced as Method() as Void;
    private var _syncId as Number? = null;
    private var _total as Number = 0;
    private var _pinRequired as Boolean = false;
    private var _chunks as Dictionary<Number, Array<Dictionary>> = {};

    public function initialize(store as AccountStore, onSynced as Method() as Void) {
        _store = store;
        _onSynced = onSynced;
    }

    public function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        var data = message.data;
        if (!(data instanceof Dictionary)) {
            return;
        }
        var op = data["op"];
        if ("begin".equals(op)) {
            begin(data);
        } else if ("chunk".equals(op)) {
            chunk(data);
        } else if ("commit".equals(op)) {
            commit(data);
        }
    }

    private function begin(data as Dictionary) as Void {
        _syncId = data["id"] as Number;
        _total = data["total"] as Number;
        _pinRequired = data["pinRequired"] == true;
        _chunks = {};
    }

    private function chunk(data as Dictionary) as Void {
        if (_syncId == null || data["id"] != _syncId) {
            return;
        }
        _chunks[data["idx"] as Number] = data["accounts"] as Array<Dictionary>;
    }

    private function commit(data as Dictionary) as Void {
        var id = data["id"] as Number;
        if (_syncId == null || id != _syncId) {
            reply(id, false, "unknown sync");
            return;
        }
        var records = [] as Array<Dictionary>;
        for (var i = 0; i < _total; i++) {
            var accounts = _chunks[i];
            if (accounts == null) {
                reset();
                reply(id, false, "missing chunk " + i);
                return;
            }
            records.addAll(accounts);
        }
        if (!SyncPayload.hash(records).equals((data["hash"] as String).toLower())) {
            reset();
            reply(id, false, "hash mismatch");
            return;
        }
        var limit = $.accountLimit();
        if (records.size() > limit) {
            reset();
            reply(id, false, "limit " + limit);
            return;
        }
        var decoded = SyncPayload.decode(records);
        if (decoded == null) {
            reset();
            reply(id, false, "invalid secret");
            return;
        }
        _store.replaceAccounts(decoded);
        _store.setPinRequired(_pinRequired);
        if (!_pinRequired) {
            new $.PinGate(_store).clear();
        }
        reset();
        reply(id, true, "");
        _onSynced.invoke();
        if (WatchUi has :showToast) {
            var message = decoded.size() == 1
                ? WatchUi.loadResource($.Rez.Strings.SyncedOne) as String
                : Lang.format(WatchUi.loadResource($.Rez.Strings.Synced) as String, [decoded.size()]);
            WatchUi.showToast(message, null);
        }
    }

    private function reset() as Void {
        _syncId = null;
        _total = 0;
        _chunks = {};
    }

    private function reply(id as Number, ok as Boolean, error as String) as Void {
        Communications.transmit({"op" => "ack", "id" => id, "ok" => ok, "err" => error}, null, new $.SyncReplyListener());
    }
}

class SyncReplyListener extends Communications.ConnectionListener {

    public function initialize() {
        ConnectionListener.initialize();
    }

    public function onComplete() as Void {
    }

    public function onError() as Void {
    }
}
