package bat.cobblemondaycare.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NurseryAccessPolicyTest {
    @Test
    void onlyAnActiveMatchingReachableSessionMayAct() {
        assertTrue(NurseryAccessPolicy.allows(true, true, true, 64.0, true));
        assertFalse(NurseryAccessPolicy.allows(false, true, true, 0.0, true));
        assertFalse(NurseryAccessPolicy.allows(true, false, true, 0.0, true));
        assertFalse(NurseryAccessPolicy.allows(true, true, false, 0.0, true));
        assertFalse(NurseryAccessPolicy.allows(true, true, true, 64.0001, true));
        assertFalse(NurseryAccessPolicy.allows(true, true, true, 0.0, false));
        assertFalse(NurseryAccessPolicy.allows(true, true, true, Double.NaN, true));
    }
}
