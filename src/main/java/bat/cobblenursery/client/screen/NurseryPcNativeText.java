package bat.cobblenursery.client.screen;

import java.util.function.Consumer;

/** Exact arguments replayed by the active screen from Cobblemon 1.8.x PCGUI and StorageSlot. */
final class NurseryPcNativeText {
    private static final int WHITE = 0xFFFFFFFF;
    private static final String[] STAT_KEYS = {"hp", "atk", "def", "sp_atk", "sp_def", "speed"};

    record Draw(String key, double x, double y, float scale, boolean large, boolean bold,
                boolean centered, boolean shadow, int color) {}

    static void header(String gender, Consumer<Draw> draw) {
        draw.accept(new Draw("level_label", 6, 1.5, 1, true, true, false, true, WHITE));
        draw.accept(new Draw("level", 19, 1.5, 1, true, true, false, true, WHITE));
        draw.accept(new Draw("name", 12, 11.5, 1, true, true, false, true, WHITE));
        if ("MALE".equals(gender)) draw.accept(new Draw("\u2642", 69, 11.5, 1, true, true, false, true, 0x32CBFF));
        if ("FEMALE".equals(gender)) draw.accept(new Draw("\u2640", 69, 11.5, 1, true, true, false, true, 0xFC5454));
    }

    static void info(Consumer<Draw> draw) {
        draw.accept(body("ui.info.nature", 40, 129.5, true, true, false));
        draw.accept(body("ui.info.ability", 40, 146.5, true, true, false));
        draw.accept(body("ui.moves", 40, 163.5, true, true, false));
        draw.accept(body("nature", 40, 137, false, true, true));
        draw.accept(body("ability", 40, 154, false, true, true));
        for (int i = 0; i < 4; i++) draw.accept(body("move" + i, 40, 170.5 + i * 7, false, true, true));
    }

    static void stats(boolean iv, Consumer<Draw> draw) {
        draw.accept(body(iv ? "ui.stats.ivs" : "ui.stats.evs", 40, 129.5, true, true, false));
        for (int i = 0; i < STAT_KEYS.length; i++) {
            draw.accept(body("ui.stats." + STAT_KEYS[i], 13, 139 + i * 10, false, false, true));
            draw.accept(body("stat" + i, 65, 139 + i * 10, false, true, true));
        }
    }

    static Draw partyLevel(int x, int y) {
        return body("ui.lv.number", x + 1, y + 1, false, false, true);
    }

    static Draw nurseryButton(double x, double y, boolean narrow, int color) {
        // No stock nursery button: use the PC's bold, centered box-name face. Shrink only if it cannot fit the authored button.
        return new Draw("button", x, y, narrow ? .5F : 1F, !narrow, true, true, false, color);
    }

    static java.util.List<Draw> lockedHabitat(DaycareLayout.Rect stage, String titleKey,
                                             int titleWidth, int priceWidth, int lineHeight, int priceColor) {
        double available = stage.width() - 12.0;
        // Keep each line proportional and reserve one glyph pixel for its shadow.
        float titleScale = (float)Math.min(1.4, available / (titleWidth + 1.0));
        float priceScale = (float)Math.min(1.15, available / (priceWidth + 1.0));
        double gap = 5;
        double top = stage.y() + (stage.height() - (titleScale + priceScale)*lineHeight - gap) / 2.0;
        return java.util.List.of(
                new Draw(titleKey, stage.centerX(), top, titleScale, true, true, true, true, WHITE),
                new Draw("price", stage.centerX(), top + titleScale*lineHeight + gap,
                        priceScale, true, true, true, true, priceColor));
    }

    static Draw nurseryStatus(double x, double y, int color) {
        return new Draw("status", x, y, .5F, false, false, false, true, color);
    }

    private static Draw body(String key, double x, double y, boolean bold, boolean centered, boolean shadow) {
        return new Draw(key, x, y, .5F, false, bold, centered, shadow, WHITE);
    }

    private NurseryPcNativeText() {}
}