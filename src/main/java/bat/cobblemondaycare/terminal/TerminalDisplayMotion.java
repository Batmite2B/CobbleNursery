package bat.cobblemondaycare.terminal;

/** One transition for one terminal as viewed by the local player. No sound, network or block state. */
public final class TerminalDisplayMotion {
    public enum Frame { OFF, TRANSITION, ON }

    private static final double ENTER_SQUARED = 16.0;
    private static final double EXIT_SQUARED = 20.25;
    private static final long TRANSITION_TIME_MS = 100L;

    private boolean near;
    private long transitionUntil;

    public Frame frame(double distanceSquared, long monotonicMillis) {
        boolean next = distanceSquared <= (near ? EXIT_SQUARED : ENTER_SQUARED);
        if (near != next) {
            near = next;
            transitionUntil = monotonicMillis + TRANSITION_TIME_MS;
        }
        return monotonicMillis < transitionUntil ? Frame.TRANSITION : near ? Frame.ON : Frame.OFF;
    }
}
