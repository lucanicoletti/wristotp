import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.Time;
import Toybox.WatchUi;

class WristOtpApp extends Application.AppBase {
    private var _syncHandler as PhoneSyncHandler?;
    private var _menu as AccountMenu?;
    private var _gate as PinGate?;

    public function initialize() {
        AppBase.initialize();
    }

    public function onStart(state as Dictionary?) as Void {
    }

    public function onStop(state as Dictionary?) as Void {
    }

    public function getInitialView() as [Views] or [Views, InputDelegates] {
        var store = new $.AccountStore();
        var menu = new $.AccountMenu(store);
        var syncHandler = new $.PhoneSyncHandler(store, method(:onSynced));
        Communications.registerForPhoneAppMessages(syncHandler.method(:onPhoneMessage));
        _syncHandler = syncHandler;
        _menu = menu;
        var gate = new $.PinGate(store);
        _gate = gate;
        if (gate.isRequired() && !gate.isUnlockedAt(Time.now().value())) {
            var pinView = new $.PinView(gate, method(:onUnlocked));
            return [pinView, new $.PinDelegate(pinView)];
        }
        return [menu, new $.AccountMenuDelegate()];
    }

    public function onSynced() as Void {
        var gate = _gate;
        if (gate != null && gate.isRequired() && !gate.isUnlockedAt(Time.now().value())) {
            var pinView = new $.PinView(gate, method(:onUnlocked));
            WatchUi.switchToView(pinView, new $.PinDelegate(pinView), WatchUi.SLIDE_IMMEDIATE);
            return;
        }
        var menu = _menu;
        if (menu != null) {
            menu.reload();
        }
    }

    public function onUnlocked() as Void {
        var menu = _menu;
        if (menu != null) {
            menu.reload();
            WatchUi.switchToView(menu, new $.AccountMenuDelegate(), WatchUi.SLIDE_IMMEDIATE);
        }
    }

    (:glance)
    public function getGlanceView() as [WatchUi.GlanceView] or [WatchUi.GlanceView, WatchUi.GlanceViewDelegate] or Null {
        return [new $.WristOtpGlanceView(new $.AccountStore())];
    }
}
