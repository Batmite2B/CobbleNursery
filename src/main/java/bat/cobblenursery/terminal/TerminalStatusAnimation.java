package bat.cobblenursery.terminal;

/** Screen icons use local monotonic time; no ticking or shared data. */
public final class TerminalStatusAnimation {
    private TerminalStatusAnimation() {}

    public static double heartScale(long millis) {
        long phase = Math.floorMod(millis, 1650L);
        return 1.0 + beat(phase, 0, .075) + beat(phase, 265, .055);
    }

    private static double beat(long phase, int start, double amplitude) {
        double relative = (phase - start) / 180.0;
        if (relative <= 0 || relative >= 1) return 0;
        double pulse = Math.sin(Math.PI * relative);
        return amplitude * pulse * pulse;
    }

    public static int alertTint(long millis) {
        double phase = Math.floorMod(millis, 2400L) / 2400.0;
        return (int) Math.round(186 + 69 * (1.0 - Math.cos(Math.TAU * phase)) * .5);
    }
}
