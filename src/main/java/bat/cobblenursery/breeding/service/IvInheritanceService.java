package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.ParentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class IvInheritanceService {
    private static final List<String> STAT_ORDER = List.of(
        "hp",
        "attack",
        "defence",
        "special_attack",
        "special_defence",
        "speed"
    );

    public Map<String, Integer> generateInheritedIvs(ParentSnapshot a, ParentSnapshot b,
                                                     BreedingItemSnapshot itemA, BreedingItemSnapshot itemB,
                                                     long seed) {
        return generateInheritedIvs(a, b, itemA, itemB, Map.of(), seed);
    }

    public Map<String, Integer> generateInheritedIvs(ParentSnapshot a, ParentSnapshot b,
                                                     BreedingItemSnapshot itemA, BreedingItemSnapshot itemB,
                                                     Map<String, Integer> rollMinimums, long seed) {
        Random random = new Random(seed);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String stat : STAT_ORDER) {
            result.put(stat, randomIv(random, rollMinimums.getOrDefault(stat, 0)));
        }

        int inheritedCount = hasDestinyKnot(itemA, itemB) ? 5 : 3;
        List<String> remainingStats = new ArrayList<>(STAT_ORDER);
        inheritedCount -= applyPowerItemInheritance(result, remainingStats, a, itemA);
        inheritedCount -= applyPowerItemInheritance(result, remainingStats, b, itemB);
        for (int i = 0; i < inheritedCount && !remainingStats.isEmpty(); i++) {
            String stat = remainingStats.remove(random.nextInt(remainingStats.size()));
            ParentSnapshot source = random.nextBoolean() ? a : b;
            result.put(stat, getIv(source, stat));
        }
        return result;
    }

    private int randomIv(Random random, int minimum) {
        int clampedMinimum = Math.max(0, Math.min(31, minimum));
        if (clampedMinimum >= 31) {
            return 31;
        }
        return clampedMinimum + random.nextInt(32 - clampedMinimum);
    }

    private boolean hasDestinyKnot(BreedingItemSnapshot itemA, BreedingItemSnapshot itemB) {
        return itemA.matchesSuffix("destiny_knot") || itemB.matchesSuffix("destiny_knot");
    }

    private int applyPowerItemInheritance(Map<String, Integer> result, List<String> remainingStats,
                                          ParentSnapshot snapshot, BreedingItemSnapshot item) {
        String forcedStat = powerItemStat(item);
        if (forcedStat == null) {
            return 0;
        }
        result.put(forcedStat, Math.max(result.getOrDefault(forcedStat, 0), getIv(snapshot, forcedStat)));
        remainingStats.remove(forcedStat);
        return 1;
    }

    /** Seed-independent floor in the same stat order as the v3 parent and offspring previews. */
    public static List<Integer> guaranteedMinimums(List<Integer> lineage, String itemA, List<Integer> ivsA,
                                                   String itemB, List<Integer> ivsB) {
        String statA = powerItemStat(new BreedingItemSnapshot(itemA, 1));
        String statB = powerItemStat(new BreedingItemSnapshot(itemB, 1));
        List<Integer> result = new ArrayList<>(STAT_ORDER.size());
        for (int i = 0; i < STAT_ORDER.size(); i++) {
            String stat = STAT_ORDER.get(i);
            int floor = Math.clamp(lineage.get(i), 0, 31);
            if (stat.equals(statA) && ivsA.size() == STAT_ORDER.size()) floor = Math.max(floor, ivsA.get(i));
            if (stat.equals(statB) && ivsB.size() == STAT_ORDER.size()) floor = Math.max(floor, ivsB.get(i));
            result.add(Math.clamp(floor, 0, 31));
        }
        return List.copyOf(result);
    }

    private static String powerItemStat(BreedingItemSnapshot item) {
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

    private int getIv(ParentSnapshot snapshot, String stat) {
        return switch (stat) {
            case "hp" -> snapshot.hpIv();
            case "attack" -> snapshot.attackIv();
            case "defence" -> snapshot.defenceIv();
            case "special_attack" -> snapshot.specialAttackIv();
            case "special_defence" -> snapshot.specialDefenceIv();
            case "speed" -> snapshot.speedIv();
            default -> 0;
        };
    }
}
