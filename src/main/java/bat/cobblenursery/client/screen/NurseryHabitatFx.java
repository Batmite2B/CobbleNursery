package bat.cobblenursery.client.screen;

import java.util.random.RandomGenerator;

/** Transient effects on one client screen; no authoritative nursery state. */
final class NurseryHabitatFx {
    private int pending = -1;
    private long requestMs;
    private int open = -1;
    private int side;
    private long firstFrame = -1;

    void request(int yard, long now) { pending = yard; requestMs = now; }
    void leave() { pending = open = -1; firstFrame = -1; }

    boolean confirm(int selected, boolean wasUnlocked, boolean unlocked, boolean success,
                    String message, long now, RandomGenerator random) {
        if (pending < 0) return false;
        if (now - requestMs > 4_000 || selected != pending) { pending = -1; return false; }
        if (!success) { pending = -1; return false; }
        if (wasUnlocked || !unlocked || !("Unlocked yard " + (pending + 1) + ".").equals(message)) return false;
        open = pending;
        pending = -1;
        side = random.nextBoolean() ? 1 : -1;
        firstFrame = -1;
        return true;
    }

    record Frame(double dx, double dy, double rotation, double landscape) {}

    Frame frame(int selected, long now, double distance) {
        if (open != selected || open < 0) return null;
        if (firstFrame < 0) firstFrame = now;
        double elapsed = Math.max(0, now - firstFrame);
        double jump = Math.min(1, elapsed / 132.0);
        double fallMs = Math.max(0, elapsed - 132.0);
        // The lift and fall both reach zero velocity at the apex. Gravity does
        // not depend on window height; a bigger window simply takes longer to exit.
        double dy = elapsed < 132 ? -10*(2*jump-jump*jump) : -10 + .000115*fallMs*fallMs;
        if (dy >= distance) { open = -1; return null; }
        double fade = Math.min(1, elapsed / 330.0);
        // The horizontal impulse stays constant through the apex and the entire fall.
        return new Frame(side * .055 * elapsed, dy, side*.25*elapsed,
                fade*fade*(3-2*fade));
    }

    static String registeredCry(String species, String form, java.util.function.Predicate<String> hasSound) {
        String suffix = form == null ? "" : form.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        String prefix = "pokemon." + species;
        String candidate = prefix + "_" + suffix + ".cry";
        if (!suffix.isEmpty() && hasSound.test(candidate)) return candidate;
        candidate = prefix + ".cry";
        return hasSound.test(candidate) ? candidate : null;
    }

    static final class CryClock {
        private final RandomGenerator random;
        private int yard = -1;
        private long next;
        CryClock(RandomGenerator random) { this.random = random; }

        boolean ready(int selected, boolean hasCandidate, long now) {
            if (selected != yard) {
                yard = selected;
                next = now + random.nextLong(5_000, 11_001);
            }
            if (!hasCandidate || now < next) return false;
            next = now + random.nextLong(13_000, 26_001);
            return true;
        }

        int firstSide() { return random.nextInt(2); }
    }
}
