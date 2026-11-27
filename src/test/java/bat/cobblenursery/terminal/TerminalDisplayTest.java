package bat.cobblenursery.terminal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalDisplayTest {
    @Test void fullPausedYardWinsOverBreedingAndLockedYardsDoNotCount() {
        TerminalDisplayState state = TerminalDisplayState.builder(2)
            .yard(0, true, false).yard(1, false, true).yard(2, true, true).build();
        assertEquals(TerminalDisplayState.Status.FULL, state.status());
        assertEquals(3, state.unlockedMask());
        assertFalse(state.unlocked(2));
        assertEquals(TerminalDisplayState.Status.IDLE,
            TerminalDisplayState.builder(1).yard(1, true, true).build().status());
    }
    @Test void countsOnlyUnlockedSlotsAndActiveCycles() {
        assertEquals(15, TerminalDisplayState.builder(100).build().unlockedMask());
        assertEquals(0, TerminalDisplayState.builder(-5).build().unlockedMask());
        assertEquals(TerminalDisplayState.Status.BREEDING,
            TerminalDisplayState.builder(1).yard(0, true, false).build().status());
    }
    @Test void eachViewerTransitionsIndependentlyWithExitHysteresis() {
        TerminalDisplayMotion first = new TerminalDisplayMotion();
        TerminalDisplayMotion second = new TerminalDisplayMotion();
        assertEquals(TerminalDisplayMotion.Frame.TRANSITION, first.frame(16, 1000));
        assertEquals(TerminalDisplayMotion.Frame.OFF, second.frame(25, 1000));
        assertEquals(TerminalDisplayMotion.Frame.TRANSITION, first.frame(16, 1099));
        assertEquals(TerminalDisplayMotion.Frame.ON, first.frame(18, 1100));
        assertEquals(TerminalDisplayMotion.Frame.TRANSITION, first.frame(21, 1200));
        assertEquals(TerminalDisplayMotion.Frame.OFF, first.frame(18, 1300));
        assertEquals(TerminalDisplayMotion.Frame.OFF, second.frame(18, 1300));
    }
    @Test void doorMovementReversesDuringTransitionWithoutStaleOnFrame() {
        TerminalDisplayMotion motion = new TerminalDisplayMotion();
        assertEquals(TerminalDisplayMotion.Frame.TRANSITION, motion.frame(4, 0));
        assertEquals(TerminalDisplayMotion.Frame.TRANSITION, motion.frame(25, 50));
        assertEquals(TerminalDisplayMotion.Frame.OFF, motion.frame(25, 150));
    }
}
