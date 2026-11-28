package bat.cobblemondaycare.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DaycareUiTextTest {
    @Test
    void successfulActionsAndEmptyRefreshesNeverBecomeMessages() {
        assertEquals("", DaycareUiText.failureOnly("Breeding started", true));
        assertEquals("", DaycareUiText.failureOnly(null, false));
        assertEquals("", DaycareUiText.failureOnly("   ", false));
        assertEquals("Not enough Pokédollars", DaycareUiText.failureOnly("Not enough Pokédollars", false));
    }

    @Test
    void textFitsMeasuredWidthAndDoesNotBreakSurrogatePairs() {
        var width = (java.util.function.ToIntFunction<String>) value -> value.codePointCount(0, value.length());
        assertEquals("Tiradas", DaycareUiText.fit("Tiradas", 7, width));
        assertEquals("Resu…", DaycareUiText.fit("Resumen", 5, width));
        assertEquals("A🐱…", DaycareUiText.fit("A🐱BC", 3, width));
        assertEquals("", DaycareUiText.fit("Resumen", 0, width));
    }
    @Test
    void usesOnlyCobblemonNativeAndStockCompactScales() {
        assertEquals(1.0f, DaycareUiText.stockScale(24, 24, false));
        assertEquals(0.5f, DaycareUiText.stockScale(25, 24, false));
        assertEquals(0.5f, DaycareUiText.stockScale(12, 24, true));
    }

    @Test
    void stockRendererDoesNotCenterAnAlreadyPositionedButtonAgain() {
        var anchor = DaycareUiText.stockAnchor(349, 102, 50, 72, 0.5F, true);
        assertEquals(356, anchor.x());
        assertEquals(102, anchor.y());
        assertEquals(false, anchor.centered());
        assertEquals(true, anchor.shadow());
        assertEquals(356.0, actualStockX(anchor, 72, 0.5F));
    }

    @Test
    void nonzeroScreenOriginAndFractionalAnchorsSnapOnceAtBothScales() {
        for (float scale : new float[]{0.5F, 1.0F}) {
            var anchor = DaycareUiText.stockAnchor(281.6, 97.7, 0, 38, scale, false);
            assertEquals(282, anchor.x());
            assertEquals(98, anchor.y());
            assertEquals(282.0, actualStockX(anchor, 38, scale));
            assertEquals(98.0, ((int)(anchor.y() / scale)) * (double) scale);
            for (int guiScale = 1; guiScale <= 4; guiScale++) {
                assertEquals(282.0 * guiScale, actualStockX(anchor, 38, scale) * guiScale);
            }
        }
    }

    private static double actualStockX(DaycareUiText.StockAnchor anchor, int glyphWidth, float scale) {
        // Cobblemon scales the pose, converts x to inverse-scaled coordinates, then GuiUtils truncates to int.
        return (int)(anchor.x() / scale - (anchor.centered() ? glyphWidth / 2 : 0)) * (double) scale;
    }

}
