package bat.cobblemondaycare.nursery.service;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EggClaimBatchTest {
    @Test void partialInventoryKeepsOverflowInOrderAndImmediatelyCheckpoints() {
        var checkpoints = new ArrayList<List<String>>();
        var source = List.of("fire", "water", "steel");
        var result = EggClaimBatch.take(source, egg -> !egg.equals("steel"), checkpoints::add);
        assertEquals(2, result.claimed());
        assertEquals(List.of("steel"), result.remaining());
        assertEquals(List.of(List.of("water", "steel"), List.of("steel")), checkpoints);
        assertEquals(List.of("fire", "water", "steel"), source);
    }
    @Test void fullInventoryNeverChangesTray() {
        var eggs = List.of("ice", "electric");
        assertEquals(eggs, EggClaimBatch.take(eggs, ignored -> false, ignored -> fail("saved full tray")).remaining());
    }
    @Test void priorDeliveryWasAlreadyCheckpointedIfALaterInsertionThrows() {
        var checkpoints = new ArrayList<List<String>>();
        assertThrows(IllegalStateException.class, () -> EggClaimBatch.take(List.of("a", "b", "c"), egg -> {
            if (egg.equals("b")) throw new IllegalStateException("insertion failed");
            return true;
        }, checkpoints::add));
        assertEquals(List.of(List.of("b", "c")), checkpoints);
    }
}