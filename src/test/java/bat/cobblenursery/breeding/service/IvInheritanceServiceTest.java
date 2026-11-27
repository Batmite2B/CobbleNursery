package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IvInheritanceServiceTest {
    private final IvInheritanceService service = new IvInheritanceService();

    @Test
    void generatesSixStatsWithinExpectedRange() {
        Map<String, Integer> ivs = service.generateInheritedIvs(
            snapshot(31, 0, 31, 0, 31, 0),
            snapshot(0, 31, 0, 31, 0, 31),
            BreedingItemSnapshot.EMPTY,
            BreedingItemSnapshot.EMPTY,
            12345L
        );

        assertEquals(6, ivs.size());
        assertTrue(ivs.values().stream().allMatch(value -> value >= 0 && value <= 31));
    }

    @Test
    void destinyKnotStillKeepsValidIvs() {
        Map<String, Integer> ivs = service.generateInheritedIvs(
            snapshot(31, 31, 31, 31, 31, 31),
            snapshot(0, 0, 0, 0, 0, 0),
            new BreedingItemSnapshot("minecraft:destiny_knot", 1),
            BreedingItemSnapshot.EMPTY,
            98765L
        );

        assertEquals(6, ivs.size());
        assertTrue(ivs.values().stream().allMatch(value -> value >= 0 && value <= 31));
    }

    @Test
    void powerItemForcesItsStatFromParent() {
        Map<String, Integer> ivs = service.generateInheritedIvs(
            snapshot(3, 7, 5, 11, 13, 31),
            snapshot(1, 2, 3, 4, 5, 6),
            new BreedingItemSnapshot("minecraft:power_anklet", 1),
            BreedingItemSnapshot.EMPTY,
            555L
        );

        assertEquals(31, ivs.get("speed"));
    }

    @Test
    void twoPowerItemsCanForceTwoDifferentStats() {
        Map<String, Integer> ivs = service.generateInheritedIvs(
            snapshot(31, 7, 5, 11, 13, 9),
            snapshot(1, 2, 3, 4, 29, 6),
            new BreedingItemSnapshot("minecraft:power_weight", 1),
            new BreedingItemSnapshot("minecraft:power_band", 1),
            777L
        );

        assertEquals(31, ivs.get("hp"));
        assertEquals(29, ivs.get("special_defence"));
    }

    @Test
    void lineageFloorChangesTheRollRangeUpToThirtyOne() {
        boolean sawThirty = false;
        boolean sawThirtyOne = false;

        for (long seed = 0; seed < 512; seed++) {
            Map<String, Integer> ivs = service.generateInheritedIvs(
                snapshot(10, 30, 10, 10, 10, 10),
                snapshot(10, 30, 10, 10, 10, 10),
                BreedingItemSnapshot.EMPTY,
                BreedingItemSnapshot.EMPTY,
                Map.of("attack", 30),
                seed
            );

            int attackIv = ivs.get("attack");
            assertTrue(attackIv >= 30 && attackIv <= 31);
            sawThirty |= attackIv == 30;
            sawThirtyOne |= attackIv == 31;
        }

        assertTrue(sawThirty);
        assertTrue(sawThirtyOne);
    }

    private ParentSnapshot snapshot(int hp, int atk, int def, int spa, int spd, int spe) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            "cobblemon:eevee",
            "normal",
            "FEMALE",
            "cobblemon:hardy",
            "adaptability",
            "cobblemon:poke_ball",
            true,
            false,
            hp, atk, def, spa, spd, spe,
            List.of("FIELD"),
            List.of("wish"),
            new net.minecraft.nbt.CompoundTag()
        );
    }
}
