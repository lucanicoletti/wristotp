import Toybox.Graphics;
import Toybox.Lang;
import Toybox.Time;
import Toybox.Timer;
import Toybox.WatchUi;

(:glance)
class WristOtpGlanceView extends WatchUi.GlanceView {
    private const URGENT_SECONDS = 5;
    private const BAR_HEIGHT = 4;

    private var _accounts as Array<Account>;
    private var _locked as Boolean;
    private var _timer as Timer.Timer;

    public function initialize(store as AccountStore) {
        GlanceView.initialize();
        _accounts = store.getGlanceAccounts();
        _locked = store.isPinRequired();
        _timer = new Timer.Timer();
    }

    public function onShow() as Void {
        _timer.start(method(:onTick), 1000, true);
    }

    public function onHide() as Void {
        _timer.stop();
    }

    public function onTick() as Void {
        WatchUi.requestUpdate();
    }

    public function onUpdate(dc as Dc) as Void {
        var width = dc.getWidth();
        var height = dc.getHeight();

        if (_accounts.size() == 0) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, height / 2, Graphics.FONT_GLANCE, $.appName(), Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
            return;
        }

        var account = _accounts[0];
        if (_locked) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, height * 0.25, Graphics.FONT_GLANCE, account.issuer.toUpper(), Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, height * 0.62, Graphics.FONT_GLANCE, "••• •••  " + (WatchUi.loadResource($.Rez.Strings.Locked) as String), Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
            return;
        }
        var now = Time.now().value().toLong();
        var remaining = account.secondsRemainingAt(now);
        var urgent = remaining <= URGENT_SECONDS;

        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(0, height * 0.25, Graphics.FONT_GLANCE, account.issuer.toUpper(), Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
        dc.setColor(urgent ? Graphics.COLOR_RED : Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(0, height * 0.62, Graphics.FONT_GLANCE_NUMBER, CodeFormatter.spaced(account.codeAt(now)), Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);

        var barY = height - BAR_HEIGHT;
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.fillRectangle(0, barY, width, BAR_HEIGHT);
        dc.setColor(urgent ? Graphics.COLOR_RED : Graphics.COLOR_BLUE, Graphics.COLOR_TRANSPARENT);
        dc.fillRectangle(0, barY, width * remaining / account.period, BAR_HEIGHT);
    }
}
