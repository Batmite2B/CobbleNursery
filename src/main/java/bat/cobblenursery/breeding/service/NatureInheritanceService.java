package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.ParentSnapshot;

import java.util.List;
import java.util.Random;

public final class NatureInheritanceService {
    private static final List<String> FALLBACK_NATURES = List.of(
        "cobblemon:hardy",
        "cobblemon:lonely",
        "cobblemon:brave",
        "cobblemon:adamant",
        "cobblemon:naughty",
        "cobblemon:bold",
        "cobblemon:docile",
        "cobblemon:relaxed",
        "cobblemon:impish",
        "cobblemon:lax",
        "cobblemon:timid",
        "cobblemon:hasty",
        "cobblemon:serious",
        "cobblemon:jolly",
        "cobblemon:naive",
        "cobblemon:modest",
        "cobblemon:mild",
        "cobblemon:quiet",
        "cobblemon:bashful",
        "cobblemon:rash",
        "cobblemon:calm",
        "cobblemon:gentle",
        "cobblemon:sassy",
        "cobblemon:careful",
        "cobblemon:quirky"
    );

    public String resolveNature(ParentSnapshot a, ParentSnapshot b, BreedingItemSnapshot itemA,
                                BreedingItemSnapshot itemB, long seed) {
        boolean aEverstone = hasEverstone(itemA);
        boolean bEverstone = hasEverstone(itemB);

        if (aEverstone && !bEverstone) {
            return a.natureId();
        }
        if (bEverstone && !aEverstone) {
            return b.natureId();
        }

        if (aEverstone && bEverstone) {
            return chooseBetween(seed ^ 0x9E3779B97F4A7C15L, a.natureId(), b.natureId());
        }
        Random random = new Random(seed ^ 0x9E3779B97F4A7C15L);
        return FALLBACK_NATURES.get(random.nextInt(FALLBACK_NATURES.size()));
    }

    private static boolean hasEverstone(BreedingItemSnapshot item) {
        return item != null && item.matchesSuffix("everstone");
    }

    /** Reveal only an outcome already fixed by the held-item rule; never look at the cycle seed. */
    public static String guaranteedNature(String baseA, BreedingItemSnapshot itemA,
                                          String baseB, BreedingItemSnapshot itemB) {
        boolean a = hasEverstone(itemA), b = hasEverstone(itemB);
        if (!a && !b) return "";
        if (a && b && !java.util.Objects.equals(baseA, baseB)) return "";
        String candidate = a ? baseA : baseB;
        if (candidate == null || candidate.isBlank()) return "";
        var id = net.minecraft.resources.ResourceLocation.tryParse(candidate);
        if (id == null) return "";
        var nature = com.cobblemon.mod.common.api.pokemon.Natures.getNature(id);
        // Cobblemon may supply its fallback for unknown IDs; require an actual match.
        return nature != null && id.equals(nature.getName()) ? nature.getName().toString() : "";
    }

    private String chooseBetween(long seed, String first, String second) {
        long mixed = seed ^ (seed >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53l;
        mixed ^= (mixed >>> 33);
        return (mixed & 1L) == 0L ? first : second;
    }
}
