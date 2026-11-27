package bat.cobblenursery.terminal;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HopperAutomationRulesTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();

    @Test void onlyPlacerCanChangeExport() {
        assertTrue(HopperAutomationRules.mayConfigure(a, a));
        assertFalse(HopperAutomationRules.mayConfigure(a, b));
        assertFalse(HopperAutomationRules.mayConfigure(null, a));
        assertFalse(HopperAutomationRules.mayConfigure(null, null));
    }

    @Test void nestAutomationDoesNotMixPrivateOrPublicEggs() {
        assertTrue(HopperAutomationRules.mayInsert(false, null, a));
        assertTrue(HopperAutomationRules.mayInsert(false, null, null));
        assertTrue(HopperAutomationRules.mayInsert(true, a, a));
        assertTrue(HopperAutomationRules.mayInsert(true, null, null));
        assertFalse(HopperAutomationRules.mayInsert(true, a, b));
        assertFalse(HopperAutomationRules.mayInsert(true, a, null));
        assertFalse(HopperAutomationRules.mayInsert(true, null, a));
    }
}
