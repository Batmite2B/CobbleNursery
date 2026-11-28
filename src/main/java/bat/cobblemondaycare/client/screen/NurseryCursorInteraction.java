package bat.cobblemondaycare.client.screen;

final class NurseryCursorInteraction {
    enum Click { PICK, PLACE, CANCEL, IGNORE }

    private NurseryCursorInteraction() {}

    static Click decide(boolean holding, boolean sourceAvailable, boolean validTarget) {
        if (holding) return validTarget ? Click.PLACE : Click.CANCEL;
        return sourceAvailable ? Click.PICK : Click.IGNORE;
    }

    record Tilt(float pitch, float roll) {}

    // Keep the facing angle fixed: horizontal mouse motion only swings the hanging body.
    static org.joml.Quaternionf heldModelRotation(Tilt tilt) {
        return new org.joml.Quaternionf().rotationXYZ(0.10F + tilt.pitch(), -0.15F, 0);
    }

    static final class Sway {
        private long last = Long.MIN_VALUE;
        private double x, y;
        private float pitch, roll;

        Tilt sample(double nextX, double nextY, long now) {
            if (last != Long.MIN_VALUE) {
                long dt = Math.clamp(now - last, 4, 100);
                double dx = (nextX - x) * 16.0 / dt;
                double dy = (nextY - y) * 16.0 / dt;
                float horizontal = response(dt, dx == 0, 15);
                float vertical = response(dt, dy == 0, 22);
                pitch += ((float) Math.clamp(dy * 0.037, -0.46, 0.46) - pitch) * vertical;
                // Positive screen roll tips the lower end left (y increases downward).
                // Move right -> lower end trails left; move left -> lower end trails right.
                float target = (float) Math.clamp(dx * 0.105, -0.74, 0.74);
                if (target * roll < 0) roll = 0; // Never show the stale side after a real reversal.
                roll += (target - roll) * horizontal;
            }
            last = now;
            x = nextX;
            y = nextY;
            return new Tilt(pitch, roll);
        }
        private static float response(long dt, boolean still, int attack) {
            return (float) (1.0 - Math.exp(-dt / (still ? 67.0 : attack)));
        }
    }
}
