package bat.cobblemondaycare.terminal;

/** The only information exposed to the owner's terminal renderers. Never stored on the terminal. */
public record TerminalDisplayState(int unlockedMask, Status status) {
    public static final TerminalDisplayState EMPTY = new TerminalDisplayState(0, Status.IDLE);

    public enum Status { IDLE, BREEDING, FULL }

    public boolean unlocked(int index) {
        return index >= 0 && index < 4 && (unlockedMask & (1 << index)) != 0;
    }

    public int packed() {
        return (unlockedMask & 0xF) | (status.ordinal() << 4);
    }

    public static TerminalDisplayState unpack(int value) {
        Status status = switch ((value >>> 4) & 3) {
            case 1 -> Status.BREEDING;
            case 2 -> Status.FULL;
            default -> Status.IDLE;
        };
        return new TerminalDisplayState(value & 0xF, status);
    }

    public static Builder builder(int unlockedYards) {
        return new Builder((1 << Math.clamp(unlockedYards, 0, 4)) - 1);
    }

    public static final class Builder {
        private final int mask;
        private boolean breeding;
        private boolean full;

        private Builder(int mask) {
            this.mask = mask;
        }

        public Builder yard(int index, boolean active, boolean pausedForSpace) {
            if (index >= 0 && index < 4 && (mask & (1 << index)) != 0) {
                breeding |= active;
                full |= pausedForSpace;
            }
            return this;
        }

        public TerminalDisplayState build() {
            return new TerminalDisplayState(mask, full ? Status.FULL : breeding ? Status.BREEDING : Status.IDLE);
        }
    }
}
