import Toybox.Lang;

(:glance)
module CodeFormatter {

    function split(code as String) as [String, String] {
        var half = (code.length() + 1) / 2;
        return [code.substring(0, half) as String, code.substring(half, code.length()) as String];
    }

    function spaced(code as String) as String {
        var halves = split(code);
        return halves[0] + " " + halves[1];
    }
}
