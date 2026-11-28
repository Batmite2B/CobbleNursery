package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.BreedingItemSnapshot;
import bat.cobblemondaycare.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NatureInheritanceServiceTest {
    private final NatureInheritanceService service = new NatureInheritanceService();

    @Test
    void everstoneForcesHolderNature() {
        ParentSnapshot a = snapshot("cobblemon:adamant");
        ParentSnapshot b = snapshot("cobblemon:modest");

        String nature = service.resolveNature(
            a,
            b,
            new BreedingItemSnapshot("minecraft:everstone", 1),
            BreedingItemSnapshot.EMPTY,
            42L
        );

        assertEquals("cobblemon:adamant", nature);
    }

    @Test
    void doubleEverstoneChoosesBetweenBothNatures() {
        ParentSnapshot a = snapshot("cobblemon:adamant");
        ParentSnapshot b = snapshot("cobblemon:modest");
        Set<String> observed = new java.util.HashSet<>();

        for (long seed = 0; seed < 64; seed++) {
            observed.add(service.resolveNature(
                a,
                b,
                new BreedingItemSnapshot("minecraft:everstone", 1),
                new BreedingItemSnapshot("minecraft:everstone", 1),
                seed
            ));
        }

        assertTrue(observed.contains("cobblemon:adamant"));
        assertTrue(observed.contains("cobblemon:modest"));
    }

    private ParentSnapshot snapshot(String natureId) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            "cobblemon:eevee",
            "normal",
            "FEMALE",
            natureId,
            "adaptability",
            "cobblemon:poke_ball",
            true,
            false,
            1, 2, 3, 4, 5, 6,
            List.of("FIELD"),
            List.of("wish"),
            new net.minecraft.nbt.CompoundTag()
        );
    }
}
