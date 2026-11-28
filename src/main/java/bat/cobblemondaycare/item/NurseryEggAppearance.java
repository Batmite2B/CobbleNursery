package bat.cobblemondaycare.item;

import java.util.Locale;

public enum NurseryEggAppearance {
    BUG(1), DARK(2), DRAGON(3), ELECTRIC(4), FAIRY(5), FIGHTING(6),
    FIRE(7), FLYING(8), GHOST(9), GRASS(10), GROUND(11), ICE(12),
    NORMAL(13), POISON(14), PSYCHIC(15), ROCK(16), STEEL(17), WATER(18);

    private final int modelData;
    private final String slug;

    NurseryEggAppearance(int modelData) {
        this.modelData = modelData;
        this.slug = name().toLowerCase(Locale.ROOT);
    }

    public int modelData() {
        return modelData;
    }

    public String slug() {
        return slug;
    }

    public static NurseryEggAppearance fromPrimaryType(String name) {
        if (name != null) {
            for (NurseryEggAppearance appearance : values()) {
                if (appearance.slug.equalsIgnoreCase(name)) {
                    return appearance;
                }
            }
        }
        return NORMAL;
    }
}
