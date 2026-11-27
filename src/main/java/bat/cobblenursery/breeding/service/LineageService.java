package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.LineageRecord;
import bat.cobblenursery.nursery.storage.LineageSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LineageService {
    private static final Map<String, Integer> ZERO_IVS = Map.of(
        "hp", 0,
        "attack", 0,
        "defence", 0,
        "special_attack", 0,
        "special_defence", 0,
        "speed", 0
    );

    public Map<String, Integer> getLineageRollFloor(MinecraftServer server, UUID ownerUuid) {
        LineageSavedData savedData = LineageSavedData.find(server);
        if (savedData == null) {
            return ZERO_IVS;
        }

        return savedData.find(ownerUuid)
            .map(record -> applyFloor(record, ZERO_IVS))
            .orElse(ZERO_IVS);
    }

    public void updateLineageFromPowerItems(MinecraftServer server, UUID ownerUuid,
                                            Map<String, Integer> finalIvs, BreedingItemSnapshot itemA,
                                            BreedingItemSnapshot itemB) {
        updateLineageFromPowerItems(LineageSavedData.get(server), ownerUuid, finalIvs, itemA, itemB);
    }

    void updateLineageFromPowerItems(LineageSavedData savedData, UUID ownerUuid,
                                     Map<String, Integer> finalIvs, BreedingItemSnapshot itemA,
                                     BreedingItemSnapshot itemB) {
        LineageRecord current = savedData.find(ownerUuid).orElseGet(() -> LineageRecord.createDefault(ownerUuid));
        LineageRecord updated = updateRecord(current, finalIvs, itemA, itemB);
        if (!updated.equals(current)) {
            savedData.put(updated);
        }
    }

    Map<String, Integer> applyFloor(LineageRecord record, Map<String, Integer> generatedIvs) {
        return record.applyAsFloor(generatedIvs);
    }

    LineageRecord updateRecord(LineageRecord record, Map<String, Integer> finalIvs, BreedingItemSnapshot itemA,
                               BreedingItemSnapshot itemB) {
        LineageRecord updated = record;
        for (String stat : targetedStats(itemA, itemB)) {
            updated = updated.withRaisedIv(stat, finalIvs.getOrDefault(stat, 0));
        }
        return updated;
    }

    Set<String> targetedStats(BreedingItemSnapshot itemA, BreedingItemSnapshot itemB) {
        Set<String> targetedStats = new LinkedHashSet<>();
        String statA = powerItemStat(itemA);
        String statB = powerItemStat(itemB);
        if (statA != null) {
            targetedStats.add(statA);
        }
        if (statB != null) {
            targetedStats.add(statB);
        }
        return targetedStats;
    }

    String powerItemStat(BreedingItemSnapshot item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        if (item.matchesSuffix("power_weight")) {
            return "hp";
        }
        if (item.matchesSuffix("power_bracer")) {
            return "attack";
        }
        if (item.matchesSuffix("power_belt")) {
            return "defence";
        }
        if (item.matchesSuffix("power_lens")) {
            return "special_attack";
        }
        if (item.matchesSuffix("power_band")) {
            return "special_defence";
        }
        if (item.matchesSuffix("power_anklet")) {
            return "speed";
        }
        return null;
    }
}
