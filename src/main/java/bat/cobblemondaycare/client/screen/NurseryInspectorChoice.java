package bat.cobblemondaycare.client.screen;

/** Production uses live hover with offspring fallback; idle retains stock PC sticky semantics. */
final class NurseryInspectorChoice {
    static boolean production(String visualState, boolean hasPreview) {
        return hasPreview && ("BREEDING".equals(visualState) || "PAUSED_NO_SPACE".equals(visualState));
    }

    static <T> T resolve(boolean production, T offspring, T explicitlySelected, T occupiedHover, T lastPreview) {
        if (production) return occupiedHover == null ? offspring : occupiedHover;
        if (explicitlySelected != null) return explicitlySelected;
        return occupiedHover == null ? lastPreview : occupiedHover;
    }
    private NurseryInspectorChoice() {}
}