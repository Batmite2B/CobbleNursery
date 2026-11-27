package bat.cobblenursery.breeding.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShinyChanceServiceTest {
    private final ShinyChanceService service = new ShinyChanceService();

    @Test
    void shinyRollIsDeterministicForSameSeed() {
        long seed = 123456789L;
        boolean first = service.rollShiny(seed);
        boolean second = service.rollShiny(seed);

        assertEquals(first, second);
    }
}
