package bat.cobblenursery.nest;

import bat.cobblenursery.config.ServerNurserySettings;


final class NestWarmthRules {
    static final int SAMPLE_TICKS = 20;
    static final int ROOM = 1, LIGHT = 2, CAREGIVER = 4;

    private NestWarmthRules() {}

    static int mask(boolean room, int light, boolean caregiver) {
        return (room ? ROOM : 0) | (light >= ServerNurserySettings.current().bonuses().minimumLight() ? LIGHT : 0) | (caregiver ? CAREGIVER : 0);
    }

    static int bonus(int mask) {
        var config = ServerNurserySettings.current().bonuses();
        return ((mask & ROOM) != 0 ? config.shelterPercent() : 0) + ((mask & LIGHT) != 0 ? config.lightPercent() : 0)
            + ((mask & CAREGIVER) != 0 ? config.caregiverPercent() : 0);
    }

    static long credit(long prior, long now, long ticks, long inserted, int priorMask, int currentMask) {
        // A first sample, an unloaded gap, or a clock jump gets no retroactive acceleration.
        if (prior <= 0 || now <= prior || ticks != SAMPLE_TICKS || now - prior > 2_000) return 0;
        long elapsed = now - Math.max(prior, inserted);
        return Math.max(0, elapsed) * bonus(priorMask & currentMask) / 100;
    }

    static boolean qualifies(String species, String ability) {
        return ServerNurserySettings.current().bonuses().qualifies(species, ability);
    }

}
