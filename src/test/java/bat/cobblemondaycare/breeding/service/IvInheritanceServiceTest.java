package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.BreedingItemSnapshot;
import bat.cobblemondaycare.nursery.model.ParentSnapshot;
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
    void successiveEggsDoNotRaiseThePairsMinimum() {
        var low = snapshot(10, 0, 0, 0, 0, 0);
        var mate = snapshot(3, 0, 0, 0, 0, 0);
        var weight = new BreedingItemSnapshot("cobblemon:power_weight", 1);
        var empty = BreedingItemSnapshot.EMPTY;
        var highEgg = service.generateInheritedIvs(low, mate, weight, empty, 17).get("hp");
        assertTrue(highEgg > 10);
        boolean sawTen = false;
        for (long seed = 0; seed < 100; seed++) {
            int hp = service.generateInheritedIvs(low, mate, weight, empty, seed * 1_000_003L).get("hp");
            assertTrue(hp >= 10 && hp <= 31);
            sawTen |= hp == 10;
        }
        assertTrue(sawTen);
    }

    @Test
    void improvingTheParentRaisesTheMinimumAndRemovingTheItemDropsIt() {
        var better = snapshot(24, 0, 0, 0, 0, 0);
        var mate = snapshot(3, 0, 0, 0, 0, 0);
        var weight = new BreedingItemSnapshot("cobblemon:power_weight", 1);
        var empty = BreedingItemSnapshot.EMPTY;
        boolean sawBelowTwentyFourWithoutItem = false;
        for (long seed = 0; seed < 100; seed++) {
            var equipped = service.generateInheritedIvs(better, mate, weight, empty, seed * 1_000_003L);
            assertTrue(equipped.get("hp") >= 24);
            var unequipped = service.generateInheritedIvs(better, mate, empty, empty, seed * 1_000_003L);
            sawBelowTwentyFourWithoutItem |= unequipped.get("hp") < 24;
        }
        assertTrue(sawBelowTwentyFourWithoutItem);
    }

    @Test
    void pairPreviewUsesBothItemsAndTheHigherParentForTheSameStat() {
        var left = List.of(12, 29, 0, 0, 0, 0);
        var right = List.of(24, 20, 0, 0, 0, 0);
        assertEquals(List.of(24, 0, 0, 0, 0, 0), IvInheritanceService.guaranteedMinimums(
            "cobblemon:power_weight", left, "cobblemon:power_weight", right));
        assertEquals(List.of(12, 20, 0, 0, 0, 0), IvInheritanceService.guaranteedMinimums(
            "cobblemon:power_weight", left, "cobblemon:power_bracer", right));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), IvInheritanceService.guaranteedMinimums(
            "cobblemon:destiny_knot", left, "", right));
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
