import Toybox.Graphics;
import Toybox.Lang;
import Toybox.System;
import Toybox.Time;
import Toybox.Timer;
import Toybox.WatchUi;

class AccountMenu extends WatchUi.CustomMenu {
    private var _store as AccountStore;
    private var _ring as CountdownRing;
    private var _itemCount as Number = 0;
    private var _timer as Timer.Timer;

    public function initialize(store as AccountStore) {
        var height = System.getDeviceSettings().screenHeight;
        var ring = new $.CountdownRing();
        CustomMenu.initialize(height / 3, Graphics.COLOR_BLACK, {
            :focus => 0,
            :focusItemHeight => (height * 0.4).toNumber(),
            :foreground => ring
        });
        _store = store;
        _ring = ring;
        _timer = new Timer.Timer();
        reload();
        _timer.start(method(:onTick), 1000, true);
    }

    public function reload() as Void {
        for (var i = _itemCount - 1; i >= 0; i--) {
            deleteItem(i);
        }
        var accounts = _store.getAccounts();
        if (accounts.size() == 0) {
            addItem(new $.EmptyMenuItem());
            _itemCount = 1;
            _ring.setPeriod(null);
        } else {
            for (var i = 0; i < accounts.size(); i++) {
                addItem(new $.AccountMenuItem(i, accounts[i]));
            }
            _itemCount = accounts.size();
            _ring.setPeriod(accounts[0].period);
        }
        WatchUi.requestUpdate();
    }

    public function onTick() as Void {
        WatchUi.requestUpdate();
    }
}

class AccountMenuItem extends WatchUi.CustomMenuItem {
    private const URGENT_SECONDS = 5;

    private var _account as Account;

    public function initialize(id as Number, account as Account) {
        CustomMenuItem.initialize(id, {});
        _account = account;
    }

    public function draw(dc as Dc) as Void {
        var width = dc.getWidth();
        var height = dc.getHeight();
        var centerX = width / 2;
        var now = Time.now().value().toLong();
        var focused = isFocused();
        var urgent = _account.secondsRemainingAt(now) <= URGENT_SECONDS;
        var maxWidth = (width * 0.72).toNumber();

        var title = _account.issuer.length() > 0 ? _account.issuer : _account.name;
        var titleFont = focused ? Graphics.FONT_SMALL : Graphics.FONT_TINY;
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height * 0.22, titleFont, TextFit.ellipsize(dc, title, titleFont, maxWidth), Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);

        var codeFont = focused ? Graphics.FONT_NUMBER_MILD : Graphics.FONT_MEDIUM;
        dc.setColor(urgent ? Graphics.COLOR_RED : Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height * 0.56, codeFont, CodeFormatter.spaced(_account.codeAt(now)), Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);

        if (focused && _account.issuer.length() > 0 && _account.name.length() > 0) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(centerX, height * 0.86, Graphics.FONT_XTINY, TextFit.ellipsize(dc, _account.name, Graphics.FONT_XTINY, maxWidth), Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
        }
    }
}

class EmptyMenuItem extends WatchUi.CustomMenuItem {

    public function initialize() {
        CustomMenuItem.initialize(:empty, {});
    }

    public function draw(dc as Dc) as Void {
        var centerX = dc.getWidth() / 2;
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, dc.getHeight() * 0.35, Graphics.FONT_SMALL, WatchUi.loadResource($.Rez.Strings.NoAccounts) as String, Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, dc.getHeight() * 0.7, Graphics.FONT_XTINY, WatchUi.loadResource($.Rez.Strings.SyncFromPhone) as String, Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
    }
}

class CountdownRing extends WatchUi.Drawable {
    private const URGENT_SECONDS = 5;

    private var _period as Number? = null;

    public function initialize() {
        Drawable.initialize({});
    }

    public function setPeriod(period as Number?) as Void {
        _period = period;
    }

    public function draw(dc as Dc) as Void {
        var period = _period;
        if (period == null) {
            return;
        }
        if (dc has :setAntiAlias) {
            dc.setAntiAlias(true);
        }
        var centerX = dc.getWidth() / 2;
        var centerY = dc.getHeight() / 2;
        var penWidth = (dc.getWidth() * 0.02).toNumber();
        if (penWidth < 4) {
            penWidth = 4;
        }
        var radius = (centerX < centerY ? centerX : centerY) - penWidth / 2 - 1;
        var remaining = Totp.secondsRemaining(Time.now().value().toLong(), period);

        dc.setPenWidth(penWidth);
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawCircle(centerX, centerY, radius);
        dc.setColor(remaining <= URGENT_SECONDS ? Graphics.COLOR_RED : Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
        if (remaining >= period) {
            dc.drawCircle(centerX, centerY, radius);
        } else {
            dc.drawArc(centerX, centerY, radius, Graphics.ARC_CLOCKWISE, 90, 90 - 360 * remaining / period);
        }
        dc.setPenWidth(1);
    }
}

class AccountMenuDelegate extends WatchUi.Menu2InputDelegate {

    public function initialize() {
        Menu2InputDelegate.initialize();
    }

    public function onSelect(item as MenuItem) as Void {
    }

    public function onWrap(key as Key) as Boolean {
        return false;
    }
}
