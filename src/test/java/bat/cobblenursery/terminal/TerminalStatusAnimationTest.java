package bat.cobblenursery.terminal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TerminalStatusAnimationTest {
    @Test void heartHasTwoGentleBeatsAndLongRest() {
        assertEquals(1.0, TerminalStatusAnimation.heartScale(0), 1e-6);
        assertTrue(TerminalStatusAnimation.heartScale(90) >= 1.07);
        assertEquals(1.0, TerminalStatusAnimation.heartScale(220), 1e-6);
        assertTrue(TerminalStatusAnimation.heartScale(355) > 1.045);
        assertTrue(TerminalStatusAnimation.heartScale(355) < TerminalStatusAnimation.heartScale(90));
        assertEquals(1.0, TerminalStatusAnimation.heartScale(900), 1e-6);
        assertEquals(TerminalStatusAnimation.heartScale(90), TerminalStatusAnimation.heartScale(1740), 1e-6);
    }
    @Test void amberPulseNeverDisappearsAndChangesSmoothly() {
        assertTrue(TerminalStatusAnimation.alertTint(0) >= 180);
        assertTrue(TerminalStatusAnimation.alertTint(1200) > TerminalStatusAnimation.alertTint(0));
        for (int t = 1; t < 2400; t++) {
            int tint = TerminalStatusAnimation.alertTint(t);
            assertTrue(tint >= 180 && tint <= 255);
            assertTrue(Math.abs(tint - TerminalStatusAnimation.alertTint(t - 1)) <= 1);
        }
    }
}
