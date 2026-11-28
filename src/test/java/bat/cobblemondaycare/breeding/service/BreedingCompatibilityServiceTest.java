package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingCompatibilityServiceTest {
    private final BreedingCompatibilityService service = new BreedingCompatibilityService();

    @Test
    void allowsOppositeGenderParentsWithSharedEggGroup() {
        ParentSnapshot a = snapshot("cobblemon:eevee", "MALE", false, true, List.of("FIELD"));
        ParentSnapshot b = snapshot("cobblemon:eevee", "FEMALE", false, true, List.of("FIELD"));

        assertTrue(service.evaluate(a, b).compatible());
    }

    @Test
    void rejectsDittoPair() {
        ParentSnapshot a = snapshot("cobblemon:ditto", "GENDERLESS", true, true, List.of("DITTO"));
        ParentSnapshot b = snapshot("cobblemon:ditto", "GENDERLESS", true, true, List.of("DITTO"));

        assertFalse(service.evaluate(a, b).compatible());
    }

    private ParentSnapshot snapshot(String speciesId, String gender, boolean ditto, boolean breedable, List<String> eggGroups) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            speciesId,
            "normal",
            gender,
            "cobblemon:hardy",
            "run_away",
            "cobblemon:poke_ball",
            breedable,
            ditto,
            1, 2, 3, 4, 5, 6,
            eggGroups,
            List.of("wish"),
            new net.minecraft.nbt.CompoundTag()
        );
    }
}
