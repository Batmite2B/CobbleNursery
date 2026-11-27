package bat.cobblenursery.nest;

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
        return durationSeconds(ivs,180);
    }
    private static int durationSeconds(Map<String,Integer> ivs,int secondsPerPerfectIv) {
        int sum = STATS.stream().mapToInt(k -> Math.clamp(ivs.getOrDefault(k,0),0,31)).sum();
        return Math.clamp((secondsPerPerfectIv*sum+30)/31,60,secondsPerPerfectIv*STATS.size());
    }
    static boolean acceptsRecoveredDeadline(Map<String,Integer> ivs,long saved,long now) {
        // Old eggs could have been recovered before the faster default was introduced.
        return saved>0 && saved<=now+durationSeconds(ivs,300)*1000L;
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