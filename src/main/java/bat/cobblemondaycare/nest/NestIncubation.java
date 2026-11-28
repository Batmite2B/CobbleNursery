package bat.cobblemondaycare.nest;

import bat.cobblemondaycare.config.ServerNurserySettings;
import java.util.List;
import java.util.Map;

public final class NestIncubation {
    public static final int SEATS = 4;
    public static final double ANCHOR_Y = 1.9 / 16.0;
    private static final List<String> STATS = List.of("hp","attack","defence","special_attack","special_defence","speed");
    private NestIncubation() {}
    public static boolean validIvs(Map<String,Integer> ivs) {
        return ivs != null && ivs.size() == 6 && STATS.stream().allMatch(k -> {
            Integer v = ivs.get(k);
            return v != null && v >= 0 && v <= 31;
        });
    }
    public static int durationSeconds(Map<String,Integer> ivs) {
        return ServerNurserySettings.current().incubation().durationSeconds(sumIvs(ivs));
    }
    private static int sumIvs(Map<String,Integer> ivs) {
        return STATS.stream().mapToInt(k -> Math.clamp(ivs.getOrDefault(k, 0), 0, 31)).sum();
    }
    static boolean acceptsRecoveredDeadline(Map<String,Integer> ivs,long saved,long now) {
        // Older eggs only carry a deadline, not the time that their recovery stamp was issued.
        return saved > 0 && saved <= now + Math.max(60, (300 * sumIvs(ivs) + 30) / 31) * 1000L;
    }
    static boolean acceptsRecoveredDeadline(Map<String,Integer> ivs,long saved,long now,long issued) {
        if (issued <= 0) return acceptsRecoveredDeadline(ivs, saved, now);
        // Keep the original deadline even if the admin later shortens incubation.
        return saved > 0 && issued <= now && saved <= issued + ServerNurserySettings.MAX_INCUBATION_SECONDS * 1000L;
    }
    public static long secondsRemaining(long deadline,long now) {
        return deadline <= now ? 0 : Math.ceilDiv(deadline-now,1000L);
    }
    public static boolean mayAccess(java.util.UUID owner,java.util.UUID player) {
        return player != null && (owner == null || owner.equals(player));
    }
    public static <T> T requireCommitted(T physical,T indexed,boolean coordinatesMatch) {
        if (physical != indexed || physical != null && !coordinatesMatch)
            throw new IllegalStateException("Incubation storage insertion not committed");
        return physical;
    }
}