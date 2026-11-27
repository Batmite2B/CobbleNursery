package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.ParentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityInheritanceServiceTest {
    private final AbilityInheritanceService service = new AbilityInheritanceService();

    @Test
    void commonAbilityPrefersMatchingNormalSlot() {
        ParentSnapshot mother = snapshot("adaptability");

        String result = service.resolveInheritedAbility(
            mother,
            List.of("run_away", "adaptability", "anticipation"),
            List.of("run_away", "adaptability", "anticipation"),
            1L
        );

        assertEquals("adaptability", result);
    }

    @Test
    void hiddenAbilityCanResolveToHiddenSlot() {
        ParentSnapshot mother = snapshot("anticipation");

        String result = service.resolveInheritedAbility(
            mother,
            List.of("run_away", "adaptability", "anticipation"),
            List.of("run_away", "adaptability", "anticipation"),
            4L
        );

        assertEquals("anticipation", result);
    }

    @Test
    void normalParentsCanStillRarelyProduceHiddenAbility() {
        ParentSnapshot mother = snapshot("adaptability");
        Set<String> observed = new java.util.HashSet<>();

        for (long seed = 0; seed < 1024; seed++) {
            observed.add(service.resolveInheritedAbility(
                mother,
                List.of("run_away", "adaptability", "anticipation"),
                List.of("run_away", "adaptability", "anticipation"),
                seed
            ));
        }

        assertTrue(observed.contains("anticipation"));
    }

    private ParentSnapshot snapshot(String abilityId) {
        return new ParentSnapshot(
            UUID.randomUUID(),
            "cobblemon:eevee",
            "normal",
            "FEMALE",
            "cobblemon:hardy",
            abilityId,
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
