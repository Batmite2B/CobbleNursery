package bat.cobblemondaycare.nursery.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NurseryHabitatParentMoveTest {
    @Test
    void removingOneExactParentKeepsTheOtherHeldItemAndQueuedEggs() {
        var a = parent("cobblemon:vulpix");
        var b = parent("cobblemon:ditto");
        var held = new BreedingItemSnapshot("cobblemon:everstone", 1);
        var egg = new NurseryEggTrayEntry(UUID.randomUUID(), 2, null, 123L);
        var old = NurseryYardRecord.empty(2).withParentAt(false, a, BreedingItemSnapshot.EMPTY)
                .withParentAt(true, b, held).addEgg(egg).startCycle(10L, 100L, 42L);
        var next = old.withParentAt(false, null, BreedingItemSnapshot.EMPTY);
        assertNull(next.parentA());
        assertSame(b, next.parentB());
        assertSame(held, next.itemB());
        assertEquals(List.of(egg), next.eggs());
        assertEquals(old.completedCycles(), next.completedCycles());
        assertFalse(next.active());
        assertFalse(next.pausedForEggSpace());
        assertEquals(0L, next.cycleReadyAtEpochMs());
    }

    @Test void manualStopPreservesParentsEggsAndProgressionAndClearsAutoResumeForActiveOrPaused() {
        var a=parent("cobblemon:wooper"); var b=parent("cobblemon:shellos");
        var held=new BreedingItemSnapshot("cobblemon:everstone",1);
        var egg=new NurseryEggTrayEntry(UUID.randomUUID(),2,null,123L);
        var running=NurseryYardRecord.empty(2).withParentAt(false,a,held)
                .withParentAt(true,b,BreedingItemSnapshot.EMPTY).addEgg(egg)
                .markCycleCompleted().startCycle(10,100,334);
        for(var yard:List.of(running,running.pauseForEggSpace())) {
            var stopped=yard.stopCycle();
            assertSame(a,stopped.parentA());
            assertSame(b,stopped.parentB());
            assertSame(held,stopped.itemA());
            assertEquals(List.of(egg),stopped.eggs());
            assertEquals(yard.completedCycles(),stopped.completedCycles());
            assertFalse(stopped.active());
            assertFalse(stopped.pausedForEggSpace());
            assertEquals(0,stopped.cycleStartedAtEpochMs());
            assertEquals(0,stopped.cycleReadyAtEpochMs());
            var afterClaim=stopped.withEggs(List.of());
            assertFalse(afterClaim.active() || afterClaim.pausedForEggSpace(), "claim no longer restarts this yard");
        }
    }

    private static ParentSnapshot parent(String species) {
        return new ParentSnapshot(UUID.randomUUID(), species, "normal", "FEMALE", "adamant", "test", "cobblemon:poke_ball",
                true, false, 31, 31, 31, 31, 31, 31, List.of(), List.of(), null);
    }
    @Test void newDefaultIsFiveMinutesButOldActiveCycleRetainsItsOriginalProgressAndDeadline() {
        assertEquals(5,bat.cobblemondaycare.config.NurseryConfig.DEFAULT.baseDurationMinutes());
        var oldCycle=NurseryYardRecord.empty(0).startCycle(1_000,601_000,99);
        var reloaded=NurseryYardRecord.load(oldCycle.save());
        assertEquals(.5F,reloaded.cycleProgressAt(301_000),.0001F);
        assertEquals(0F,reloaded.cycleProgressAt(999),.0001F);
        assertEquals(1F,reloaded.cycleProgressAt(601_001),.0001F);
        assertFalse(reloaded.isCycleReady(600_999));
        assertTrue(reloaded.isCycleReady(601_000));
        assertEquals(0F,reloaded.stopCycle().cycleProgressAt(301_000),.0001F);
    }
}
