package bat.cobblenursery.nest;

import bat.cobblenursery.breeding.service.ShinyChanceService;
import bat.cobblenursery.config.ServerNurserySettings;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguredIncubationRulesTest {
    @Test void usesConfiguredIncubationAndCaregiverRules() {
        var prior = ServerNurserySettings.current();
        var custom = new ServerNurserySettings(prior.costs(), prior.unlockItem(),
                new ServerNurserySettings.Breeding(45, 2, false, 1),
                new ServerNurserySettings.Incubation(60, 30, 2),
                new ServerNurserySettings.Bonuses(0, 20, 40, 12, 8,
                        List.of("cobblemon:eevee"), List.of("cobblemon:magmaarmor")),
                prior.automation());
        try {
            ServerNurserySettings.install(custom);
            assertEquals(30, NestIncubation.durationSeconds(Map.of("hp", 1)));
            assertEquals(60, NestIncubation.durationSeconds(Map.of("hp", 31)));
            assertEquals(120, NestIncubation.durationSeconds(Map.of("hp", 31, "speed", 31)));
            assertEquals(0, NestWarmthRules.mask(false, 11, false));
            assertEquals(60, NestWarmthRules.bonus(NestWarmthRules.mask(false, 12, true)));
            assertTrue(NestWarmthRules.qualifies("cobblemon:eevee", null));
            assertTrue(NestWarmthRules.qualifies("cobblemon:magcargo", "magmaarmor"));
            assertFalse(NestWarmthRules.qualifies("cobblemon:magmar", "flamebody"));
            for (long seed = 0; seed < 20; seed++) assertTrue(new ShinyChanceService().rollShiny(seed));
        } finally {
            ServerNurserySettings.install(prior);
        }
    }

    @Test void recoveredEggKeepsItsDeadlineEvenWithShorterConfig() {
        long issued = 1_000_000L;
        long ready = issued + 3_600_000L;
        assertTrue(NestIncubation.acceptsRecoveredDeadline(Map.of("hp", 31), ready, issued + 1, issued));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(Map.of("hp", 31), ready, issued - 1, issued));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(Map.of("hp", 31),
                issued + ServerNurserySettings.MAX_INCUBATION_SECONDS * 1000L + 1, issued, issued));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(Map.of("hp", 31), ready, issued, 0));
    }
}
