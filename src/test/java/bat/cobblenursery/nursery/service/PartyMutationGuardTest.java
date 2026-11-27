package bat.cobblenursery.nursery.service;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static bat.cobblenursery.nursery.service.PartyMutationGuard.Result.*;

class PartyMutationGuardTest {
    private static final PartyMutationGuard.Observation PRESENT = state(true, true, false, true, false, false);
    private static final PartyMutationGuard.Observation ABSENT = state(false, false, true, false, true, false);

    @Test
    void slotOnlyInsertionDoesNotCommitAndCanBeRolledBack() {
        var storage = new AtomicReference<>(ABSENT);
        var result = PartyMutationGuard.apply(true, storage::get,
                () -> { storage.set(state(true, false, true, false, true, false)); throw new IllegalStateException("observer"); },
                () -> {}, () -> storage.set(ABSENT), ignored -> {});
        assertEquals(REJECTED, result);
        assertEquals(ABSENT, storage.get());
    }

    @Test
    void slotOnlyRemovalCanCommitOnlyAfterStaleUuidIndexIsGone() {
        var storage = new AtomicReference<>(PRESENT);
        var result = PartyMutationGuard.apply(false, storage::get,
                () -> { storage.set(state(false, true, false, false, true, false)); throw new IllegalStateException("observer"); },
                () -> storage.set(ABSENT), () -> fail("already reconciled"), ignored -> {});
        assertEquals(COMMITTED, result);
        assertEquals(ABSENT, storage.get());
    }

    @Test
    void staleOriginalOwnershipAndDuplicateUuidPreventAnyMutation() {
        var duplicate = state(true, true, false, true, false, true);
        var result = PartyMutationGuard.apply(false, () -> duplicate,
                () -> fail("no mutation allowed"), () -> fail("no repair allowed"),
                () -> fail("no rollback allowed"), ignored -> {});
        assertEquals(REJECTED, result);
    }

    @Test
    void mutationCompletingBeforeAnObserverErrorCommitsWithoutRetry() {
        var storage = new AtomicReference<>(ABSENT);
        assertEquals(COMMITTED, PartyMutationGuard.apply(true, storage::get,
                () -> { storage.set(PRESENT); throw new IllegalStateException("notification failed"); },
                () -> fail("no repair"), () -> fail("no rollback"), ignored -> {}));
    }

    private static PartyMutationGuard.Observation state(boolean atSlot, boolean indexedExact, boolean indexEmpty,
                                                         boolean exactCoordinates, boolean detached, boolean duplicate) {
        return new PartyMutationGuard.Observation(atSlot, indexedExact, indexEmpty, exactCoordinates, detached, duplicate);
    }
}
