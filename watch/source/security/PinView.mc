import Toybox.Graphics;
import Toybox.Lang;
import Toybox.System;
import Toybox.Time;
import Toybox.Timer;
import Toybox.WatchUi;

class PinView extends WatchUi.View {
    enum Mode {
        MODE_ENTER,
        MODE_SET,
        MODE_CONFIRM
    }

    private const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "0", "ok"] as Array<String>;
    private const BUTTON_KEYS = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "del", "ok"] as Array<String>;
    private const GRID_LEFT = 0.19;
    private const GRID_TOP = 0.32;
    private const GRID_WIDTH = 0.62;
    private const GRID_HEIGHT = 0.56;

    private var _gate as PinGate;
    private var _onUnlocked as Method() as Void;
    private var _mode as Mode;
    private var _digits as String = "";
    private var _firstPin as String = "";
    private var _message as String? = null;
    private var _buttonIndex as Number = 0;
    private var _timer as Timer.Timer;
    private var _touch as Boolean;

    public function initialize(gate as PinGate, onUnlocked as Method() as Void) {
        View.initialize();
        _gate = gate;
        _onUnlocked = onUnlocked;
        _mode = gate.hasPin() ? MODE_ENTER : MODE_SET;
        _timer = new Timer.Timer();
        _touch = System.getDeviceSettings().isTouchScreen;
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

    public function isTouch() as Boolean {
        return _touch;
    }

    public function keyAt(x as Number, y as Number) as String? {
        var width = System.getDeviceSettings().screenWidth;
        var height = System.getDeviceSettings().screenHeight;
        var column = ((x - width * GRID_LEFT) / (width * GRID_WIDTH / 3)).toNumber();
        var row = ((y - height * GRID_TOP) / (height * GRID_HEIGHT / 4)).toNumber();
        if (x < width * GRID_LEFT || y < height * GRID_TOP || column > 2 || row > 3) {
            return null;
        }
        return KEYS[row * 3 + column];
    }

    public function cycle(step as Number) as Void {
        _buttonIndex = (_buttonIndex + step + BUTTON_KEYS.size()) % BUTTON_KEYS.size();
        WatchUi.requestUpdate();
    }

    public function pressSelected() as Void {
        press(BUTTON_KEYS[_buttonIndex]);
    }

    public function press(key as String) as Void {
        var now = Time.now().value();
        if (_mode == MODE_ENTER && _gate.lockedSecondsAt(now) > 0) {
            return;
        }
        if (key.equals("del")) {
            if (_digits.length() > 0) {
                _digits = _digits.substring(0, _digits.length() - 1) as String;
            }
        } else if (key.equals("ok")) {
            submit(now);
        } else if (_digits.length() < PinGate.MAX_LENGTH) {
            _digits += key;
            _message = null;
        }
        WatchUi.requestUpdate();
    }

    private function submit(now as Number) as Void {
        if (_digits.length() < PinGate.MIN_LENGTH) {
            _message = WatchUi.loadResource($.Rez.Strings.PinTooShort) as String;
            return;
        }
        if (_mode == MODE_SET) {
            _firstPin = _digits;
            _digits = "";
            _mode = MODE_CONFIRM;
            _message = null;
        } else if (_mode == MODE_CONFIRM) {
            if (_digits.equals(_firstPin)) {
                _gate.setPin(_digits, now);
                _onUnlocked.invoke();
            } else {
                _digits = "";
                _firstPin = "";
                _mode = MODE_SET;
                _message = WatchUi.loadResource($.Rez.Strings.PinMismatch) as String;
            }
        } else {
            var result = _gate.verify(_digits, now);
            _digits = "";
            if (result == PinGate.RESULT_OK) {
                _onUnlocked.invoke();
            } else if (result == PinGate.RESULT_WIPED) {
                _mode = MODE_SET;
                _message = WatchUi.loadResource($.Rez.Strings.PinWiped) as String;
            } else {
                _message = WatchUi.loadResource($.Rez.Strings.PinWrong) as String;
            }
        }
    }

    public function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_TRANSPARENT, Graphics.COLOR_BLACK);
        dc.clear();
        if (dc has :setAntiAlias) {
            dc.setAntiAlias(true);
        }
        var width = dc.getWidth();
        var height = dc.getHeight();
        var centerX = width / 2;
        var locked = _mode == MODE_ENTER ? _gate.lockedSecondsAt(Time.now().value()) : 0;

        var title = _message;
        var titleColor = Graphics.COLOR_RED;
        if (locked > 0) {
            title = Lang.format(WatchUi.loadResource($.Rez.Strings.PinLocked) as String, [locked]);
        } else if (title == null) {
            titleColor = Graphics.COLOR_WHITE;
            if (_mode == MODE_SET) {
                title = WatchUi.loadResource($.Rez.Strings.PinSet) as String;
            } else if (_mode == MODE_CONFIRM) {
                title = WatchUi.loadResource($.Rez.Strings.PinConfirm) as String;
            } else {
                title = WatchUi.loadResource($.Rez.Strings.PinEnter) as String;
            }
        }
        dc.setColor(titleColor, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height * 0.15, Graphics.FONT_XTINY, title, Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);

        drawDots(dc, centerX, (height * 0.24).toNumber());

        if (_touch) {
            drawKeypad(dc);
        } else {
            drawButtonPicker(dc);
        }
    }

    private function drawDots(dc as Dc, centerX as Number, y as Number) as Void {
        var count = _digits.length() > PinGate.MIN_LENGTH ? _digits.length() : PinGate.MIN_LENGTH;
        var spacing = dc.getWidth() / 16;
        var radius = spacing / 4;
        var left = centerX - (count - 1) * spacing / 2;
        for (var i = 0; i < count; i++) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            if (i < _digits.length()) {
                dc.fillCircle(left + i * spacing, y, radius);
            } else {
                dc.setPenWidth(2);
                dc.drawCircle(left + i * spacing, y, radius);
                dc.setPenWidth(1);
            }
        }
    }

    private function drawKeypad(dc as Dc) as Void {
        var width = dc.getWidth();
        var height = dc.getHeight();
        var cellWidth = width * GRID_WIDTH / 3;
        var cellHeight = height * GRID_HEIGHT / 4;
        for (var i = 0; i < KEYS.size(); i++) {
            var x = width * GRID_LEFT + (i % 3) * cellWidth + cellWidth / 2;
            var y = height * GRID_TOP + (i / 3) * cellHeight + cellHeight / 2;
            drawKey(dc, KEYS[i], x, y, cellHeight * 0.42, false);
        }
    }

    private function drawButtonPicker(dc as Dc) as Void {
        var centerX = dc.getWidth() / 2;
        var height = dc.getHeight();
        var key = BUTTON_KEYS[_buttonIndex];
        drawKey(dc, key, centerX, height * 0.55, height * 0.12, true);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(centerX, height * 0.78, Graphics.FONT_XTINY, WatchUi.loadResource($.Rez.Strings.PinButtonHint) as String, Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
    }

    private function drawKey(dc as Dc, key as String, x as Numeric, y as Numeric, radius as Numeric, highlighted as Boolean) as Void {
        var isOk = key.equals("ok");
        var ready = _digits.length() >= PinGate.MIN_LENGTH;
        var fill = Graphics.COLOR_DK_GRAY;
        if (isOk && ready) {
            fill = Graphics.COLOR_BLUE;
        } else if (highlighted) {
            fill = Graphics.COLOR_BLUE;
        }
        var isDelete = key.equals("del");
        dc.setColor(fill, Graphics.COLOR_TRANSPARENT);
        dc.fillCircle(x, y, radius);
        var label = key;
        if (isDelete) {
            label = "DEL";
        } else if (isOk) {
            label = "OK";
        }
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(x, y, isOk || isDelete ? Graphics.FONT_XTINY : Graphics.FONT_SMALL, label, Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
    }
}

class PinDelegate extends WatchUi.BehaviorDelegate {
    private var _view as PinView;

    public function initialize(view as PinView) {
        BehaviorDelegate.initialize();
        _view = view;
    }

    public function onTap(event as ClickEvent) as Boolean {
        var coordinates = event.getCoordinates();
        var key = _view.keyAt(coordinates[0], coordinates[1]);
        if (key != null) {
            _view.press(key);
        }
        return true;
    }

    public function onSelect() as Boolean {
        if (_view.isTouch()) {
            _view.press("ok");
        } else {
            _view.pressSelected();
        }
        return true;
    }

    public function onNextPage() as Boolean {
        if (!_view.isTouch()) {
            _view.cycle(1);
        }
        return true;
    }

    public function onPreviousPage() as Boolean {
        if (!_view.isTouch()) {
            _view.cycle(-1);
        }
        return true;
    }
}
