package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.config.ServerNurserySettings;

import java.util.Random;

public final class ShinyChanceService {
    public boolean rollShiny(long seed) {
        int denominator = ServerNurserySettings.current().breeding().shinyOddsDenominator();
        return new Random(seed ^ 0x5DEECE66DL ^ 0xC0BB1E5L).nextInt(denominator) == 0;
    }
}
