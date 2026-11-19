import Toybox.Lang;
import Toybox.WatchUi;

function accountLimit() as Number {
    return 50;
}

(:glance)
function appName() as String {
    return WatchUi.loadResource($.Rez.Strings.AppName) as String;
}
