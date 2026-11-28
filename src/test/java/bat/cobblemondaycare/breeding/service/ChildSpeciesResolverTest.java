package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildSpeciesResolverTest {
    private final ChildSpeciesResolver resolver = new ChildSpeciesResolver();

    @Test
    void usesFemaleSpeciesWhenNoDittoExists() {
        ParentSnapshot male = snapshot("cobblemon:pikachu", "MALE", false);
        ParentSnapshot female = snapshot("cobblemon:raichu", "FEMALE", false);

        assertEquals("cobblemon:raichu", resolver.resolveBaseChildSpecies(male, female));
    }

    @Test
    void usesNonDittoSpeciesWhenDittoExists() {
        ParentSnapshot ditto = snapshot("cobblemon:ditto", "GENDERLESS", true);
        ParentSnapshot partner = snapshot("cobblemon:dratini", "MALE", false);

        assertEquals("cobblemon:dratini", resolver.resolveBaseChildSpecies(ditto, partner));
    }

    @Test
    void manaphyResolvesToPhione() {
        assertEquals("cobblemon:phione", resolver.resolveParentBaseSpeciesId("cobblemon:manaphy", 5L));
    }

    @Test
    void illumiseSpecialCaseCanProduceEitherSpecies() {
        Set<String> observed = new java.util.HashSet<>();
        for (long seed = 0; seed < 32; seed++) {
            observed.add(resolver.resolveParentBaseSpeciesId("cobblemon:illumise", seed));
        }
        assertEquals(Set.of("cobblemon:illumise", "cobblemon:volbeat"), observed);
    }

    @Test
    void walksToEarliestBabySpeciesInLineage() {
        Map<String, String> preEvolutions = Map.of(
            "cobblemon:raichu", "cobblemon:pikachu",
            "cobblemon:pikachu", "cobblemon:pichu"
        );

        assertEquals(
            "cobblemon:pichu",
            resolver.walkToBabySpecies("cobblemon:raichu", preEvolutions::get)
        );
    }

    @Test
    void everstonePreservesMatchingRegionalForm() {
        assertEquals(
            "Alola",
            resolver.resolveInheritedFormId("Alola", true, List.of("Normal", "Alola"), "Normal")
        );
    }

    @Test
    void withoutEverstoneFallsBackToStandardForm() {
        assertEquals(
            "Normal",
            resolver.resolveInheritedFormId("Alola", false, List.of("Normal", "Alola"), "Normal")
        );
    }

    @Test
    void unknownInheritedFormFallsBackToStandardForm() {
        String resolved = resolver.resolveInheritedFormId("Alola-Bias", true, List.of("Normal", "Alola"), "Normal");
        assertTrue(Set.of("Normal", "Alola").contains(resolved));
        assertEquals("Normal", resolved);
    }

    private ParentSnapshot snapshot(String speciesId, String gender, boolean ditto) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            speciesId,
            "normal",
            gender,
            "cobblemon:hardy",
            "shed_skin",
            "cobblemon:poke_ball",
            true,
            ditto,
            1, 2, 3, 4, 5, 6,
            List.of(ditto ? "DITTO" : "DRAGON"),
            List.of("dragon_rage"),
            new net.minecraft.nbt.CompoundTag()
        );
    }
}
