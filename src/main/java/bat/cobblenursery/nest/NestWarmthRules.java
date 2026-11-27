package bat.cobblenursery.nest;

import java.util.Set;

final class NestWarmthRules {
    static final int SAMPLE_TICKS = 20;
    static final int ROOM = 1, LIGHT = 2, CAREGIVER = 4;
    static final int ROOM_PERCENT = 15, LIGHT_PERCENT = 15, CAREGIVER_PERCENT = 30;
    private static final Set<String> CAREGIVERS = Set.of("cobblemon:togepi", "cobblemon:exeggcute",
        "cobblemon:chansey", "cobblemon:blissey", "cobblemon:happiny", "cobblemon:vullaby",
        "cobblemon:togetic", "cobblemon:kangaskhan", "cobblemon:maushold");

    private NestWarmthRules() {}

    static int mask(boolean room, int light, boolean caregiver) {
        return (room ? ROOM : 0) | (light >= 10 ? LIGHT : 0) | (caregiver ? CAREGIVER : 0);
    }

    static int bonus(int mask) {
        return ((mask & ROOM) != 0 ? ROOM_PERCENT : 0) + ((mask & LIGHT) != 0 ? LIGHT_PERCENT : 0)
            + ((mask & CAREGIVER) != 0 ? CAREGIVER_PERCENT : 0);
    }

    static long credit(long prior, long now, long ticks, long inserted, int priorMask, int currentMask) {
        // A first sample, an unloaded gap, or a clock jump gets no retroactive acceleration.
        if (prior <= 0 || now <= prior || ticks != SAMPLE_TICKS || now - prior > 2_000) return 0;
        long elapsed = now - Math.max(prior, inserted);
        return Math.max(0, elapsed) * bonus(priorMask & currentMask) / 100;
    }

    static boolean qualifies(String species, String ability) {
        return CAREGIVERS.contains(species) || "flamebody".equals(ability) || "cobblemon:flamebody".equals(ability);
    }

}
