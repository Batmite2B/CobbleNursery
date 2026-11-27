package bat.cobblenursery.client.screen;

/** A single confirmed drop, driven by client render time rather than game ticks. */
final class NurseryLandingMotion {
    static final long DURATION_MS = 620;
    private static final long MAX_FIRST_FRAME_WAIT_MS = 1000;
    private static final double DESIRED_PEAK = 36.0;
    private final long confirmedAt;
    private long startedAt = -1;
    private double peak;

    NurseryLandingMotion(long confirmedAt) { this.confirmedAt = confirmedAt; }

    boolean ongoing(long now) {
        return startedAt < 0 ? now - confirmedAt <= MAX_FIRST_FRAME_WAIT_MS : now - startedAt < DURATION_MS;
    }

    double lift(long now, double headroom) {
        if (!ongoing(now)) return 0;
        headroom = Math.max(0,headroom);
        if (startedAt < 0) {
            startedAt = now;
            // A small invariant margin absorbs idle mesh motion unless a tall model already
            // occupies the fixed maximum size, whose fitted height is capped every frame.
            peak = Math.min(DESIRED_PEAK, headroom > 22 ? headroom - 2 : headroom);
        }
        double t = Math.clamp((now-startedAt) / (double)DURATION_MS, 0, 1);
        double remaining = 1 - t*t*(3-2*t);
        return Math.min(headroom,peak*remaining);
    }
}