package bat.cobblemondaycare.nursery.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NurseryTransferTest {
    private final UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID replacement = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void exactSnapshotAndEmptyTargetRequiredInBothDirections() {
        for (boolean toHabitat : new boolean[]{true, false}) {
            var transfer = new NurseryTransfer(toHabitat, 3, 5, true, source);
            assertTrue(transfer.matches(source, false));
            assertFalse(transfer.matches(replacement, false));
            assertFalse(transfer.matches(null, false));
            assertFalse(transfer.matches(source, true));
        }
    }

    @Test
    void invalidIndexesOrMissingExpectedIdentityAreRejected() {
        assertFalse(new NurseryTransfer(true, -1, 0, false, source).matches(source, false));
        assertFalse(new NurseryTransfer(true, 4, 0, false, source).matches(source, false));
        assertFalse(new NurseryTransfer(false, 0, 6, false, source).matches(source, false));
        assertFalse(new NurseryTransfer(false, 0, -1, false, source).matches(source, false));
        assertFalse(new NurseryTransfer(false, 0, 0, false, null).matches(null, false));
    }
}
