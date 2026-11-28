package bat.cobblemondaycare.nursery.service;

import bat.cobblemondaycare.nursery.model.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HopperEggOutputTest {
    @Test void sendsOldestUnlockedEggAndCheckpointsOnlyAcceptedEgg() {
        var locked = new NurseryEggTrayEntry(UUID.randomUUID(), 3, null, 0);
        var later = new NurseryEggTrayEntry(UUID.randomUUID(), 0, null, 2);
        var first = new NurseryEggTrayEntry(UUID.randomUUID(), 0, null, 1);
        var state = NurseryPlayerState.createDefault(UUID.randomUUID())
            .upsertYard(NurseryYardRecord.empty(0).addEgg(later).addEgg(first))
            .upsertYard(NurseryYardRecord.empty(3).addEgg(locked));
        var checkpoint = new AtomicReference<NurseryPlayerState>();
        assertFalse(HopperEggOutput.transferOne(state, 1, entry -> false, checkpoint::set));
        assertNull(checkpoint.get());
        assertTrue(HopperEggOutput.transferOne(state, 1, entry -> {
            assertEquals(first, entry);
            return true;
        }, checkpoint::set));
        assertEquals(java.util.List.of(later), checkpoint.get().findYard(0).orElseThrow().eggs());
        assertEquals(java.util.List.of(locked), checkpoint.get().findYard(3).orElseThrow().eggs());
    }
    @Test void oneTerminalMustNotDrainAnUnassignedPatio() {
        var egg = new NurseryEggTrayEntry(UUID.randomUUID(), 1, null, 1);
        var state = NurseryPlayerState.createDefault(UUID.randomUUID()).withUnlockedYardSlots(4)
            .upsertYard(NurseryYardRecord.empty(1).addEgg(egg));
        // Patio I is activated at this endpoint; Patio II must stay in its tray.
        assertFalse(HopperEggOutput.transferOne(state, 1, entry -> true, ignored -> {}));
    }
    @Test void interleavedFourOutletStressConservesEveryEggExactlyOnce() {
        int deliveredAcrossRuns = 0;
        for (int seed = 0; seed < 128; seed++) {
            var rng = new java.util.Random(seed);
            var owner = UUID.randomUUID();
            var data = new bat.cobblemondaycare.nursery.storage.NurserySavedData();
            var state = NurseryPlayerState.createDefault(owner).withUnlockedYardSlots(4);
            var expected = new java.util.HashSet<UUID>();
            var delivered = new java.util.HashSet<UUID>();
            var routes = new bat.cobblemondaycare.nursery.storage.NurserySavedData.EggOutlet[4];
            for (int yard = 0; yard < 4; yard++) {
                routes[yard] = new bat.cobblemondaycare.nursery.storage.NurserySavedData.EggOutlet(
                    yard < 2 ? "minecraft:overworld" : "minecraft:the_nether", yard % 2, UUID.randomUUID());
                var record = NurseryYardRecord.empty(yard);
                for (int n = 0; n < 16; n++) {
                    var id = UUID.randomUUID();
                    expected.add(id);
                    record = record.addEgg(new NurseryEggTrayEntry(id, yard, null, rng.nextInt(32)));
                }
                state = state.upsertYard(record);
            }
            data.put(state);
            int[] target = {-1, -1, -1, -1};

            for (int op = 0; op < 256; op++) {
                final int terminal = rng.nextInt(4);
                int yard = rng.nextInt(4);
                switch (rng.nextInt(5)) {
                    case 0 -> {
                        assertTrue(data.setEggOutlet(owner, yard, routes[terminal], true));
                        target[yard] = terminal;
                    }
                    case 1 -> {
                        assertTrue(data.setEggOutlet(owner, yard, routes[terminal], false));
                        if (target[yard] == terminal) target[yard] = -1;
                    }
                    default -> {
                        var previous = data.find(owner).orElseThrow();
                        int mask = data.eggOutletMask(owner, routes[terminal]);
                        boolean full = rng.nextBoolean();
                        boolean changed = HopperEggOutput.transferOne(previous, mask, egg -> {
                            assertEquals(terminal, target[egg.sourceYardIndex()]);
                            if (full) return false;
                            assertTrue(delivered.add(egg.entryId()), "duplicate egg");
                            return true;
                        }, data::put);
                        if (!changed) assertSame(previous, data.find(owner).orElseThrow());
                    }
                }
            }
            for (int yard = 0; yard < 4; yard++) data.setEggOutlet(owner, yard, routes[yard], true);
            for (int terminal = 0; terminal < 4; terminal++) {
                final int outlet = terminal;
                int mask = data.eggOutletMask(owner, routes[terminal]);
                while (HopperEggOutput.transferOne(data.find(owner).orElseThrow(), mask, egg -> {
                    assertEquals(outlet, egg.sourceYardIndex());
                    assertTrue(delivered.add(egg.entryId()), "duplicate egg after final assignment");
                    return true;
                }, data::put)) {}
            }
            assertEquals(expected, delivered, "lost or extra egg in run "+seed);
            assertTrue(data.find(owner).orElseThrow().yards().stream().allMatch(yard->yard.eggs().isEmpty()));
            deliveredAcrossRuns += delivered.size();
        }
        assertEquals(8192, deliveredAcrossRuns);
    }
}
