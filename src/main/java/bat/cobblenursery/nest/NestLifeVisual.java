package bat.cobblenursery.nest;

import java.util.Map;

/** Quantized cosmetic projection from authoritative data; no individual stats reach the renderer. */
public final class NestLifeVisual {
    public static final int MAX_TIER=32;
    // Old saved incubation deadlines can still be up to 30 minutes away.
    public static final long MAX_REMAINING_MS=1_800_000L;
    public static final long MAX_READY_AGE_MS=3_153_600_000_000L;
    private static final String[] STATS={"hp","attack","defence","special_attack","special_defence","speed"};
    private NestLifeVisual() {}

    public static int sizeTier(Map<String,Integer> ivs) {
        int total=0;
        for(String stat:STATS) total+=Math.clamp(ivs.getOrDefault(stat,0),0,31);
        return (total*MAX_TIER+93)/186;
    }
    public static double scale(int tier) {
        return .8+.2*Math.clamp(tier,0,MAX_TIER)/MAX_TIER;
    }
    public static int seed(java.util.UUID id) { return mix(id.hashCode() ^ 0x59ac713d); }
    private static int mix(int val) {
        val ^= val >>> 16; val *= 0x7feb352d; val ^= val >>> 15; val *= 0x846ca68b; return val ^ (val >>> 16);
    }
    static int period(long remainingMs) {
        if (remainingMs<=0) return 4_200;
        if (remainingMs<=18_000) return 3_000;
        if (remainingMs<=60_000) return 6_000;
        if (remainingMs<=120_000) return 12_000;
        return 20_000;
    }
    public static double shake(long remainingMs,int seed) {
        return pulse(shakeElapsed(remainingMs,seed));
    }
    public static long shakeElapsed(long remainingMs,int seed) {
        int period=period(remainingMs);
        int cell=(int)Math.floorDiv(remainingMs,period);
        int offset=Math.floorMod(mix(seed+cell*31),period/4)-period/8;
        long untilCenter=Math.floorMod(remainingMs,period)-period/2-offset;
        return 330-untilCenter;
    }
    /** Shared two-burst waveform; callers choose their own start time and scale. */
    public static double pulse(long elapsed) {
        if(elapsed<0 || elapsed>=620) return 0;
        boolean later=elapsed>=340;
        long t=later ? elapsed-340 : elapsed;
        if(t>=260) return 0;
        double radians=Math.PI*t/260;
        double fade=Math.sin(radians);
        return Math.sin(2*radians)*fade*fade*(8/(3*Math.sqrt(3)))*(later ? .76 : 1);
    }
    static boolean nearMotion(long remainingMs,int seed) {
        for (int offset=-360;offset<=360;offset+=90)
            if (Math.abs(shake(remainingMs+offset,seed))>.0001) return true;
        return false;
    }
}