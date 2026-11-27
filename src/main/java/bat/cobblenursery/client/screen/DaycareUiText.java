package bat.cobblenursery.client.screen;

import java.util.function.ToIntFunction;

final class DaycareUiText {
    private DaycareUiText() {}

    static String failureOnly(String message, boolean success) {
        return success || message == null || message.isBlank() ? "" : message;
    }

    static float stockScale(int measuredWidth, int availableWidth, boolean compact) {
        return compact || measuredWidth > availableWidth ? 0.5F : 1.0F;
    }

    record StockAnchor(int x, int y, boolean centered, boolean shadow) {}

    static StockAnchor stockAnchor(double x, double y, double width, int measuredWidth, float scale, boolean centerInBox) {
        // A box's leading coordinate is calculated here, so Cobblemon must not center again.
        double left = centerInBox ? x + (width - measuredWidth * scale) / 2.0 : x;
        return new StockAnchor((int) Math.round(left), (int) Math.round(y), false, true);
    }

    static String fit(String text, int maxWidth, ToIntFunction<String> width) {
        if (maxWidth <= 0) return "";
        if (width.applyAsInt(text) <= maxWidth) return text;
        String ellipsis = "…";
        if (width.applyAsInt(ellipsis) > maxWidth) return "";
        int end = text.length();
        while (end > 0) {
            end = text.offsetByCodePoints(end, -1);
            String shortened = text.substring(0, end).stripTrailing() + ellipsis;
            if (width.applyAsInt(shortened) <= maxWidth) return shortened;
        }
        return ellipsis;
    }
}
