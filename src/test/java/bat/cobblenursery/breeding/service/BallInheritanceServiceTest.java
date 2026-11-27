package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BallInheritanceServiceTest {
    private final BallInheritanceService service = new BallInheritanceService();

    @Test
    void usesFemaleBallForDifferentSpeciesBreeding() {
        ParentSnapshot male = snapshot("cobblemon:pikachu", "MALE", false, "cobblemon:great_ball");
        ParentSnapshot female = snapshot("cobblemon:raichu", "FEMALE", false, "cobblemon:love_ball");

        assertEquals("cobblemon:love_ball", service.resolveInheritedBall(male, female, 123L));
    }

    @Test
    void usesNonDittoBallWhenDittoExists() {
        ParentSnapshot ditto = snapshot("cobblemon:ditto", "GENDERLESS", true, "cobblemon:ultra_ball");
        ParentSnapshot partner = snapshot("cobblemon:dratini", "MALE", false, "cobblemon:lure_ball");

        assertEquals("cobblemon:lure_ball", service.resolveInheritedBall(ditto, partner, 456L));
    }

    @Test
    void normalizesMasterBallToPokeBall() {
        ParentSnapshot male = snapshot("cobblemon:eevee", "MALE", false, "cobblemon:master_ball");
        ParentSnapshot female = snapshot("cobblemon:eevee", "FEMALE", false, "cobblemon:cherish_ball");

        assertEquals("cobblemon:poke_ball", service.resolveInheritedBall(male, female, 4L));
    }

    private ParentSnapshot snapshot(String speciesId, String gender, boolean ditto, String ballId) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            speciesId,
            "normal",
            gender,
            "cobblemon:hardy",
            "adaptability",
            ballId,
            true,
            ditto,
            1, 2, 3, 4, 5, 6,
            List.of(ditto ? "DITTO" : "FIELD"),
            List.of("wish"),
            new net.minecraft.nbt.CompoundTag()
        );
    }
}
