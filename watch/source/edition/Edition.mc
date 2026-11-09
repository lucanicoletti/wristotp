import Toybox.Lang;
import Toybox.WatchUi;

(:free)
function accountLimit() as Number {
    return 1;
}

(:pro)
function accountLimit() as Number {
    return 50;
}

(:free :glance)
function appName() as String {
    return WatchUi.loadResource($.Rez.Strings.AppNameFree) as String;
}

(:pro :glance)
function appName() as String {
    return WatchUi.loadResource($.Rez.Strings.AppName) as String;
}
