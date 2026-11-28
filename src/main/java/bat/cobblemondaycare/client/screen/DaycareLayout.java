package bat.cobblemondaycare.client.screen;

public final class DaycareLayout {
    public static final int BASE_WIDTH = 293;
    public static final int BASE_HEIGHT = 238;
    public static final int DEFAULT_SCALE = 1;
    public static final int WINDOW_OFFSET_Y = 14;

    private static final int PARTY_LEFT_COLUMN_X = 11;
    private static final int PARTY_RIGHT_COLUMN_X = 41;
    private static final int PARTY_SLOT_START_Y = 47;
    private static final int PARTY_SLOT_WIDTH = 22;
    private static final int PARTY_SLOT_HEIGHT = 22;
    private static final int PARTY_SLOT_GAP_Y = 7;
    private static final int PARTY_LEFT_COLUMN_Y_OFFSET = 2;
    private static final int PARTY_RIGHT_COLUMN_Y_OFFSET = -6;

    private static final int YARD_WIDTH = 74;
    private static final int YARD_HEIGHT = 82;
    private static final int YARD_START_X = 72;
    private static final int YARD_START_Y = 21;
    private static final int YARD_GAP_X = 2;
    private static final int YARD_GAP_Y = 2;

    private static final int STATS_PANEL_X = 0;
    private static final int STATS_PANEL_Y = 135;
    private static final int STATS_PANEL_WIDTH = 58;
    private static final int STATS_PANEL_HEIGHT = 68;

    private static final int DETAILS_PREVIEW_X = 227;
    private static final int DETAILS_PREVIEW_Y = 22;
    private static final int DETAILS_PREVIEW_WIDTH = 58;
    private static final int DETAILS_PREVIEW_HEIGHT = 58;

    private static final int DETAILS_TEXT_X = 223;
    private static final int DETAILS_TEXT_Y = 79;
    private static final int DETAILS_TEXT_WIDTH = 67;
    private static final int DETAILS_TEXT_HEIGHT = 103;

    private static final int DETAILS_BOTTOM_X = 223;
    private static final int DETAILS_BOTTOM_Y = 178;
    private static final int DETAILS_BOTTOM_WIDTH = 60;
    private static final int DETAILS_BOTTOM_HEIGHT = 56;

    private static final int TRAY_X = 72;
    private static final int TRAY_Y = 201;
    private static final int TRAY_WIDTH = 149;
    private static final int TRAY_HEIGHT = 35;

    private static final int CLAIM_BUTTON_X = 118;
    private static final int CLAIM_BUTTON_Y = 181;
    private static final int CLAIM_BUTTON_WIDTH = 56;
    private static final int CLAIM_BUTTON_HEIGHT = 13;
    private static final int CLOSE_BUTTON_X = 2;
    private static final int CLOSE_BUTTON_Y = 6;
    private static final int CLOSE_BUTTON_WIDTH = 26;
    private static final int CLOSE_BUTTON_HEIGHT = 13;


    private static final int TRADE_BUTTON_WIDTH = 31;
    private static final int TRADE_BUTTON_HEIGHT = 10;
    private static final int YARD_BUTTON_ROW_BOTTOM_OFFSET = 24;
    private static final int LOWER_YARD_BUTTON_ROW_Y_OFFSET = -2;
    private static final int YARD_BUTTON_GAP = 4;

    private static final int PARTY_RENDER_ANCHOR_X = 11;
    private static final int PARTY_RENDER_ANCHOR_Y = -15;
    private static final int YARD_PARENT_A_X = 19;
    private static final int YARD_PARENT_B_X = 54;
    private static final int YARD_PARENT_Y = 20;
    private static final int YARD_HEART_LEFT_X = 64;
    private static final int YARD_HEART_RIGHT_X = 3;
    private static final int YARD_HEART_TOP_Y = 27;
    private static final int YARD_HEART_BOTTOM_Y = 25;
    private static final int DETAILS_PREVIEW_CENTER_X = 256;
    private static final int DETAILS_PREVIEW_CENTER_Y = 51;

    private static final int YARD_TITLE_Y = -6;
    private static final int YARD_LOCK_COST_Y = 47;
    private static final int YARD_BUY_BUTTON_Y = 57;
    private static final int YARD_BUY_BUTTON_WIDTH = 31;
    private static final int YARD_BUY_BUTTON_HEIGHT = 10;
    private static final int YARD_EGG_SUMMARY_LEFT_X = 3;
    private static final int YARD_EGG_SUMMARY_RIGHT_X = 57;
    private static final int YARD_EGG_SUMMARY_Y = 12;
    private static final int YARD_EGG_SUMMARY_WIDTH = 14;
    private static final int YARD_EGG_SUMMARY_HEIGHT = 14;

    private static final int EGG_CELL_START_X = 74;
    private static final int EGG_CELL_START_Y = 201;
    private static final int EGG_CELL_WIDTH = 17;
    private static final int EGG_CELL_HEIGHT = 15;
    private static final int EGG_CELL_GAP_X = 3;
    private static final int EGG_CELL_GAP_Y = 2;
    private static final int EGG_CELL_COLUMNS = 7;

    private final int leftPos;
    private final int topPos;
    private final int scale;

    public DaycareLayout(int leftPos, int topPos, int scale) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.scale = scale;
    }

    public int windowWidth() {
        return BASE_WIDTH * scale;
    }

    public int windowHeight() {
        return BASE_HEIGHT * scale;
    }

    public int scale() {
        return scale;
    }

    public Rect partySlotRect(int slotIndex) {
        boolean rightColumn = (slotIndex % 2) == 1;
        int baseX = rightColumn ? PARTY_RIGHT_COLUMN_X : PARTY_LEFT_COLUMN_X;
        int baseY = PARTY_SLOT_START_Y
            + ((slotIndex / 2) * (PARTY_SLOT_HEIGHT + PARTY_SLOT_GAP_Y))
            + (rightColumn ? PARTY_RIGHT_COLUMN_Y_OFFSET : PARTY_LEFT_COLUMN_Y_OFFSET);
        return rect(baseX, baseY, PARTY_SLOT_WIDTH, PARTY_SLOT_HEIGHT);
    }

    public Point partyPokemonAnchor(int slotIndex) {
        Rect slot = partySlotRect(slotIndex);
        return new Point(slot.x() + scaled(PARTY_RENDER_ANCHOR_X), slot.y() + scaled(PARTY_RENDER_ANCHOR_Y));
    }

    public Rect statsPanelRect() {
        return rect(STATS_PANEL_X, STATS_PANEL_Y, STATS_PANEL_WIDTH, STATS_PANEL_HEIGHT);
    }

    public Rect yardRect(int yardIndex) {
        int baseX = YARD_START_X + ((yardIndex % 2) * (YARD_WIDTH + YARD_GAP_X));
        int baseY = YARD_START_Y + ((yardIndex / 2) * (YARD_HEIGHT + YARD_GAP_Y));
        return rect(baseX, baseY, YARD_WIDTH, YARD_HEIGHT);
    }

    public Point yardParentAnchor(int yardIndex, boolean parentB) {
        Rect yard = yardRect(yardIndex);
        int offsetX = parentB ? YARD_PARENT_B_X : YARD_PARENT_A_X;
        return new Point(yard.x() + scaled(offsetX), yard.y() + scaled(YARD_PARENT_Y));
    }

    public Point yardHeartAnchor(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        int heartX = (yardIndex % 2) == 0 ? YARD_HEART_LEFT_X : YARD_HEART_RIGHT_X;
        int heartY = yardIndex >= 2 ? YARD_HEART_BOTTOM_Y : YARD_HEART_TOP_Y;
        return new Point(yard.x() + scaled(heartX), yard.y() + scaled(heartY));
    }

    public Rect yardStartButtonRect(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        int totalRowWidth = scaled((TRADE_BUTTON_WIDTH * 2) + YARD_BUTTON_GAP);
        int rowX = yard.x() + ((yard.width() - totalRowWidth) / 2);
        int rowY = yard.y() + yard.height() - scaled(YARD_BUTTON_ROW_BOTTOM_OFFSET);
        if (yardIndex >= 2) {
            rowY += scaled(LOWER_YARD_BUTTON_ROW_Y_OFFSET);
        }
        return new Rect(
            rowX,
            rowY,
            scaled(TRADE_BUTTON_WIDTH),
            scaled(TRADE_BUTTON_HEIGHT)
        );
    }

    public Rect yardTakeButtonRect(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        int totalRowWidth = scaled((TRADE_BUTTON_WIDTH * 2) + YARD_BUTTON_GAP);
        int rowX = yard.x() + ((yard.width() - totalRowWidth) / 2);
        int rowY = yard.y() + yard.height() - scaled(YARD_BUTTON_ROW_BOTTOM_OFFSET);
        if (yardIndex >= 2) {
            rowY += scaled(LOWER_YARD_BUTTON_ROW_Y_OFFSET);
        }
        return new Rect(
            rowX + scaled(TRADE_BUTTON_WIDTH + YARD_BUTTON_GAP),
            rowY,
            scaled(TRADE_BUTTON_WIDTH),
            scaled(TRADE_BUTTON_HEIGHT)
        );
    }

    public Rect detailsPreviewRect() {
        return rect(DETAILS_PREVIEW_X, DETAILS_PREVIEW_Y, DETAILS_PREVIEW_WIDTH, DETAILS_PREVIEW_HEIGHT);
    }

    public Point detailsPreviewCenter() {
        return point(DETAILS_PREVIEW_CENTER_X, DETAILS_PREVIEW_CENTER_Y);
    }

    public Rect detailsTextRect() {
        return rect(DETAILS_TEXT_X, DETAILS_TEXT_Y, DETAILS_TEXT_WIDTH, DETAILS_TEXT_HEIGHT);
    }

    // The background atlas has transparent padding below the opaque content.
    public int detailsGlyphBottom() { return screenY(193); }
    public int summaryInfoTitleY() { return screenY(131); }
    public int compatibilityLabelY(int index) { return screenY(121 + 5 * index); }
    public int summaryEggMoveY(int index, boolean compact) {
        return screenY(173 + (compact ? 5 : 9) * index);
    }

    public Rect detailsBottomRect() {
        return rect(DETAILS_BOTTOM_X, DETAILS_BOTTOM_Y, DETAILS_BOTTOM_WIDTH, DETAILS_BOTTOM_HEIGHT);
    }

    public Rect trayRect() {
        return rect(TRAY_X, TRAY_Y, TRAY_WIDTH, TRAY_HEIGHT);
    }

    public Rect claimButtonRect() {
        return rect(CLAIM_BUTTON_X, CLAIM_BUTTON_Y, CLAIM_BUTTON_WIDTH, CLAIM_BUTTON_HEIGHT);
    }

    public Rect closeButtonRect() {
        return rect(CLOSE_BUTTON_X, CLOSE_BUTTON_Y, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
    }

    public Rect eggCellRect(int index) {
        int row = index / EGG_CELL_COLUMNS;
        int col = index % EGG_CELL_COLUMNS;
        int baseX = EGG_CELL_START_X + (col * (EGG_CELL_WIDTH + EGG_CELL_GAP_X));
        int baseY = EGG_CELL_START_Y + (row * (EGG_CELL_HEIGHT + EGG_CELL_GAP_Y));
        return rect(baseX, baseY, EGG_CELL_WIDTH, EGG_CELL_HEIGHT);
    }

    public Rect yardEggSummaryRect(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        int eggSummaryX = (yardIndex % 2) == 0 ? YARD_EGG_SUMMARY_LEFT_X : YARD_EGG_SUMMARY_RIGHT_X;
        int eggSummaryYOffset = yardIndex < 2 ? 3 : 1;
        if ((yardIndex % 2) == 0) {
            eggSummaryX += 1;
        }
        return new Rect(
            yard.x() + scaled(eggSummaryX),
            yard.y() + scaled(YARD_EGG_SUMMARY_Y + eggSummaryYOffset),
            scaled(YARD_EGG_SUMMARY_WIDTH),
            scaled(YARD_EGG_SUMMARY_HEIGHT)
        );
    }

    public int scaled(int baseValue) {
        return baseValue * scale;
    }

    public int screenX(int baseX) {
        return leftPos + scaled(baseX);
    }

    public int screenY(int baseY) {
        return topPos + scaled(baseY);
    }

    public Rect rect(int baseX, int baseY, int baseWidth, int baseHeight) {
        return new Rect(screenX(baseX), screenY(baseY), scaled(baseWidth), scaled(baseHeight));
    }

    public Point point(int baseX, int baseY) {
        return new Point(screenX(baseX), screenY(baseY));
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public int centerX() {
            return x + (width / 2);
        }

        public int centerY() {
            return y + (height / 2);
        }
    }

    public record Point(int x, int y) {
    }


    public Point yardTitleCenter(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        int nudgeTowardScreenCenter = scaled(23);

        int x = yardIndex % 2 == 0
                ? yard.centerX() + nudgeTowardScreenCenter   // yards 1 y 3 → a la derecha
                : yard.centerX() - nudgeTowardScreenCenter;  // yards 2 y 4 → a la izquierda

        return new Point(x, yard.y + scaled(YARD_TITLE_Y));
    }

    public Point yardLockCostCenter(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        return new Point(yard.centerX(), yard.y + scaled(YARD_LOCK_COST_Y));
    }

    public Rect yardBuyButtonRect(int yardIndex) {
        Rect yard = yardRect(yardIndex);
        return new Rect(
                yard.x + ((yard.width - scaled(YARD_BUY_BUTTON_WIDTH)) / 2),
                yard.y + scaled(YARD_BUY_BUTTON_Y),
                scaled(YARD_BUY_BUTTON_WIDTH),
                scaled(YARD_BUY_BUTTON_HEIGHT)
        );
    }


}
