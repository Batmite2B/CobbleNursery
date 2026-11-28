package bat.cobblemondaycare.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaycareLayoutLegibilityTest {
    @Test
    void detailsTrackTheOpaquePanelAndGlyphRowsStayWithinIt() {
        for (int scale = 1; scale <= 2; scale++) {
            DaycareLayout layout = new DaycareLayout(23, 41, scale);
            var card = layout.detailsTextRect();
            assertEquals(23 + 223 * scale + (67 * scale) / 2, card.centerX());
            assertEquals(67 * scale, card.width());
            assertTrue(layout.summaryEggMoveY(3, true) + 4 * scale < layout.detailsGlyphBottom());
            assertEquals(5 * scale, layout.summaryEggMoveY(3, true) - layout.summaryEggMoveY(2, true));
            assertTrue(layout.compatibilityLabelY(1) + 4 * scale < layout.summaryInfoTitleY());
        }
    }
}
