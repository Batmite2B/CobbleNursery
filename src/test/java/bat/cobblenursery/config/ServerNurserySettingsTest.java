package bat.cobblenursery.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ServerNurserySettingsTest {
    @TempDir Path dir;

    @Test void createsExpandedDefaultsAndNeverOverwritesLegacy() throws Exception {
        Path file = dir.resolve("config/cobblenursery.json");
        var warnings = new ArrayList<String>();
        var defaults = ServerNurserySettings.load(file, id -> true, warnings::add);
        assertEquals(ServerNurserySettings.DEFAULT, defaults);
        assertTrue(Files.readString(file).contains("secondsPerPerfectIv"));
        String old = "{\"habitatUnlockCosts\":{\"yard2\":3,\"yard3\":4,\"yard4\":5},\"otherMod\":true}";
        Files.writeString(file, old);
        var existing = ServerNurserySettings.load(file, id -> true, warnings::add);
        assertEquals(3, existing.costs().yard2());
        assertEquals(defaults.breeding(), existing.breeding());
        assertEquals(defaults.incubation(), existing.incubation());
        assertEquals(old, Files.readString(file));
        assertTrue(warnings.isEmpty());
    }

    @Test void generatedConfigUsesShortCommentsAndStillLoadsEdits() throws Exception {
        Path file = dir.resolve("cobblenursery.json");
        var warnings = new ArrayList<String>();
        ServerNurserySettings.load(file, id -> true, warnings::add);
        String generated = Files.readString(file);
        var root = com.google.gson.JsonParser.parseString(generated).getAsJsonObject();
        assertFalse(root.has("_help"));
        assertFalse(root.getAsJsonObject("bonuses").has("recheckSeconds"));
        var lines = generated.lines().toList();
        for (String key : root.keySet()) {
            int line = java.util.stream.IntStream.range(0, lines.size())
                    .filter(i -> lines.get(i).startsWith("  \"" + key + "\":")).findFirst().orElseThrow();
            assertTrue(lines.get(line - 1).startsWith("  // "), "Missing comment before " + key);
            assertTrue(lines.get(line - 1).length() <= 92, "Long comment for " + key);
        }
        assertEquals(ServerNurserySettings.DEFAULT, ServerNurserySettings.load(file, id -> true, warnings::add));
        String edited = generated.replace("\"secondsPerEgg\": 300", "\"secondsPerEgg\": 90");
        Files.writeString(file, edited);
        assertEquals(90, ServerNurserySettings.load(file, id -> true, warnings::add).breeding().secondsPerEgg());
        assertTrue(warnings.isEmpty());
        assertEquals(edited, Files.readString(file));
    }

    @Test void readsCustomSettingsAndRejectsInvalidFieldsIndependently() throws Exception {
        Path file = dir.resolve("cobblenursery.json");
        Files.writeString(file, """
            {
              "habitatUnlockItem":"minecraft:emerald",
              "habitatUnlockCosts":{"yard2":0,"yard3":6,"yard4":7},
              "breeding":{"secondsPerEgg":45,"eggsPerHabitat":0,"allowDitto":false,"shinyOddsDenominator":32},
              "incubation":{"secondsPerPerfectIv":30,"minimumSeconds":30,"eggsPerNest":2},
              "bonuses":{"shelterPercent":0,"lightPercent":25,"minimumLight":8,"caregiverRadius":12,
                         "caregiverSpecies":[],"caregiverAbilities":["example:warm"],"recheckSeconds":120},
              "automation":{"terminalExport":false,"nestIntake":false}
            }
            """);
        var warnings = new ArrayList<String>();
        var cfg = ServerNurserySettings.load(file, id -> id.equals("minecraft:emerald"), warnings::add);
        assertEquals("minecraft:emerald", cfg.unlockItem());
        assertEquals(0, cfg.costs().yard2());
        assertEquals(45, cfg.breeding().secondsPerEgg());
        assertEquals(8, cfg.breeding().eggsPerHabitat());
        assertFalse(cfg.breeding().allowDitto());
        assertEquals(32, cfg.breeding().shinyOddsDenominator());
        assertEquals(30, cfg.incubation().secondsPerPerfectIv());
        assertEquals(2, cfg.incubation().eggsPerNest());
        assertEquals(0, cfg.bonuses().shelterPercent());
        assertEquals(25, cfg.bonuses().lightPercent());
        assertEquals(8, cfg.bonuses().minimumLight());
        assertEquals(12, cfg.bonuses().caregiverRadius());
        String legacy = Files.readString(file);
        Files.writeString(file, legacy.replace(",\"recheckSeconds\":120", ""));
        assertEquals(cfg, ServerNurserySettings.load(file, id -> id.equals("minecraft:emerald"), ignored -> {}));
        Files.writeString(file, legacy);
        assertTrue(cfg.bonuses().caregiverSpecies().isEmpty());
        assertTrue(cfg.bonuses().caregiverAbilities().contains("example:warm"));
        assertFalse(cfg.automation().terminalExport());
        assertFalse(cfg.automation().nestIntake());
        assertEquals(1, warnings.size());
        assertEquals("cobblemon:relic_coin", ServerNurserySettings.load(file, id -> false, warnings::add).unlockItem());
    }
}
