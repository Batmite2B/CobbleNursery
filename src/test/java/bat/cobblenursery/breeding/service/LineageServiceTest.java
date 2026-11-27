package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.LineageRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineageServiceTest {
    private final LineageService service = new LineageService();

    @Test
    void aMinimumEarnedForOneSpeciesCarriesToAnotherButNotToAnotherPlayer() {
        var data = new bat.cobblenursery.nursery.storage.LineageSavedData();
        UUID owner = UUID.randomUUID();
        service.updateLineageFromPowerItems(data, owner, Map.of("hp", 24, "attack", 31),
            new BreedingItemSnapshot("cobblemon:power_weight", 1), BreedingItemSnapshot.EMPTY);
        var next = data.find(owner).orElseThrow();
        assertEquals(24, service.applyFloor(next, Map.of("hp", 12)).get("hp"));
        assertEquals(29, service.applyFloor(next, Map.of("hp", 29)).get("hp"));
        assertEquals(0, next.attackIv());
        assertEquals(true, data.find(UUID.randomUUID()).isEmpty());
    }

    @Test
    void recordedLineageActsAsPermanentIvFloor() {
        LineageRecord record = new LineageRecord(UUID.randomUUID(), 0, 0, 0, 0, 0, 31);

        Map<String, Integer> adjusted = service.applyFloor(record, Map.of(
            "hp", 10,
            "attack", 10,
            "defence", 10,
            "special_attack", 10,
            "special_defence", 10,
            "speed", 12
        ));

        assertEquals(31, adjusted.get("speed"));
    }

    @Test
    void onlyPowerItemStatsUpdateTheLineageRecord() {
        LineageRecord base = LineageRecord.createDefault(UUID.randomUUID());

        LineageRecord updated = service.updateRecord(
            base,
            Map.of(
                "hp", 4,
                "attack", 8,
                "defence", 12,
                "special_attack", 16,
                "special_defence", 20,
                "speed", 24
            ),
            new BreedingItemSnapshot("minecraft:power_bracer", 1),
            BreedingItemSnapshot.EMPTY
        );

        assertEquals(8, updated.attackIv());
        assertEquals(0, updated.hpIv());
        assertEquals(0, updated.speedIv());
    }

    @Test
    void lineageNeverLowersExistingStatProgress() {
        LineageRecord base = new LineageRecord(UUID.randomUUID(), 0, 0, 0, 0, 0, 31);

        LineageRecord updated = service.updateRecord(
            base,
            Map.of(
                "hp", 1,
                "attack", 1,
                "defence", 1,
                "special_attack", 1,
                "special_defence", 1,
                "speed", 12
            ),
            new BreedingItemSnapshot("minecraft:power_anklet", 1),
            BreedingItemSnapshot.EMPTY
        );

        assertEquals(31, updated.speedIv());
    }
}
