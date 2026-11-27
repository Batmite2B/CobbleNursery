package bat.cobblenursery.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabitatUnlockCostsTest {
    @Test
    void legacyAndMissingValuesUsePerYardDefaults() {
        var warnings = new ArrayList<String>();
        var prices = HabitatUnlockCosts.parse("{\"otherSetting\":true}", warnings::add);
        assertPrices(prices, 0, 64, 128, 192);
        assertEquals(1, warnings.size());
        warnings.clear();
        assertPrices(HabitatUnlockCosts.parse("{\"habitatUnlockCosts\":{\"yard3\":99}}", warnings::add),
                0, 64, 99, 192);
        assertEquals(2, warnings.size());
    }

    @Test
    void customCostsAndZeroAreAcceptedButNeverChangeTheOpenYard() {
        var warnings = new ArrayList<String>();
        var prices = HabitatUnlockCosts.parse("{\"habitatUnlockCosts\":{\"yard1\":35,\"yard2\":0,\"yard3\":42,\"yard4\":1000000}}", warnings::add);
        assertPrices(prices, 0, 0, 42, 1000000);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void invalidCostFallsBackIndividuallyAndMalformedDocumentUsesDefaults() {
        var warnings = new ArrayList<String>();
        var prices = HabitatUnlockCosts.parse("{\"habitatUnlockCosts\":{\"yard2\":-1,\"yard3\":1.5,\"yard4\":1000001}}", warnings::add);
        assertPrices(prices, 0, 64, 128, 192);
        assertEquals(3, warnings.size());
        assertPrices(HabitatUnlockCosts.parse("{\"habitatUnlockCosts\":{\"yard2\":\"55\",\"yard3\":21,\"yard4\":false}}", warnings::add),
                0, 64, 21, 192);
        assertPrices(HabitatUnlockCosts.parse("{", warnings::add), 0, 64, 128, 192);
    }

    @Test
    void firstRunCreatesDefaultsButExistingConfigRemainsByteForByte() throws Exception {
        Path workspace = Files.createDirectories(Path.of("build"));
        Path folder = Files.createTempDirectory(workspace, "nursery-config-test");
        Path file = folder.resolve("cobblenursery.json");
        var warnings = new ArrayList<String>();
        try {
            assertPrices(HabitatUnlockCosts.load(file, warnings::add), 0, 64, 128, 192);
            assertTrue(Files.isRegularFile(file));
            assertTrue(warnings.isEmpty());
            String current = "{ \"unrelated\": true, \"habitatUnlockCosts\": {\"yard2\": 0} }\n";
            Files.writeString(file, current);
            assertPrices(HabitatUnlockCosts.load(file, warnings::add), 0, 0, 128, 192);
            assertEquals(current, Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(folder);
        }
    }

    private void assertPrices(HabitatUnlockCosts prices, int first, int second, int third, int fourth) {
        assertEquals(List.of(first, second, third, fourth),
                java.util.stream.IntStream.range(0, 4).map(prices::forYardIndex).boxed().toList());
    }
}
