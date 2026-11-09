import Toybox.Graphics;
import Toybox.Lang;

module TextFit {

    function ellipsize(dc as Dc, text as String, font as FontDefinition, maxWidth as Number) as String {
        if (dc.getTextWidthInPixels(text, font) <= maxWidth) {
            return text;
        }
        var length = text.length();
        while (length > 1) {
            length--;
            var candidate = text.substring(0, length) + "…";
            if (dc.getTextWidthInPixels(candidate, font) <= maxWidth) {
                return candidate;
            }
        }
        return "…";
    }
}
