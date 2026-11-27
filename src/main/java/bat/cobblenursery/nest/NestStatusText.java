package bat.cobblenursery.nest;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class NestStatusText {
    private static final String PREFIX = "gui.cobblenursery.nest.";
    private NestStatusText() {}

    static Component format(long deadline, long now, int mask) {
        int bonus = NestWarmthRules.bonus(mask);
        long remaining = Math.max(0, deadline - now);
        long seconds = Math.ceilDiv(Math.ceilDiv(remaining * 100, 100 + bonus), 1000L);
        var time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);

        var details = Component.empty();
        add(details, "boost_room", NestWarmthRules.ROOM_PERCENT, (mask & NestWarmthRules.ROOM) != 0);
        add(details, "boost_light", NestWarmthRules.LIGHT_PERCENT, (mask & NestWarmthRules.LIGHT) != 0);
        add(details, "boost_caregiver", NestWarmthRules.CAREGIVER_PERCENT, (mask & NestWarmthRules.CAREGIVER) != 0);
        if (bonus == 0) return Component.translatable(PREFIX + "remaining_normal", time, details);
        return Component.translatable(PREFIX + "remaining_details", time, bonus, details);
    }

    private static void add(MutableComponent details, String key, int percent, boolean active) {
        if (!details.getSiblings().isEmpty()) details.append(Component.literal(" / "));
        details.append(active ? Component.translatable(PREFIX + key, percent)
                : Component.translatable(PREFIX + key + "_missing"));
    }
}