package bat.cobblenursery.breeding.service;

import bat.cobblenursery.config.NurseryConfig;

import java.util.Random;

public final class ShinyChanceService {
    public boolean rollShiny(long seed) {
        int denominator = Math.max(1, NurseryConfig.DEFAULT.shinyOddsDenominator());
        return new Random(seed ^ 0x5DEECE66DL ^ 0xC0BB1E5L).nextInt(denominator) == 0;
    }
}
