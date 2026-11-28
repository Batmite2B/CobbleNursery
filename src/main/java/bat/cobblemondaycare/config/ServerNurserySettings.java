package bat.cobblemondaycare.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Server rules, loaded once at startup. The client receives only the presentation values it needs. */
public record ServerNurserySettings(
        HabitatUnlockCosts costs, String unlockItem, Breeding breeding, Incubation incubation,
        Bonuses bonuses, Automation automation) {
    public static final String DEFAULT_ITEM = "cobblemon:relic_coin";
    public static final int MAX_IV_SECONDS = 3600;
    public static final int MAX_INCUBATION_SECONDS = MAX_IV_SECONDS * 6;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public static final ServerNurserySettings DEFAULT = new ServerNurserySettings(
            HabitatUnlockCosts.DEFAULT, DEFAULT_ITEM, new Breeding(300, 8, true, 4096),
            new Incubation(180, 60, 4),
            new Bonuses(15, 15, 30, 10, 16,
                    List.of("cobblemon:togepi", "cobblemon:exeggcute", "cobblemon:chansey", "cobblemon:blissey",
                            "cobblemon:happiny", "cobblemon:vullaby", "cobblemon:togetic", "cobblemon:kangaskhan",
                            "cobblemon:maushold"), List.of("cobblemon:flamebody")),
            new Automation(true, true));
    private static volatile ServerNurserySettings current = DEFAULT;
    public static ServerNurserySettings current() { return current; }
    public static void install(ServerNurserySettings value) { current = value; }

    public record Breeding(int secondsPerEgg, int eggsPerHabitat, boolean allowDitto, int shinyOddsDenominator) {}
    public record Incubation(int secondsPerPerfectIv, int minimumSeconds, int eggsPerNest) {
        public int durationSeconds(int totalIvs) {
            int sum = Math.clamp(totalIvs, 0, 186);
            return Math.max(minimumSeconds, (secondsPerPerfectIv * sum + 30) / 31);
        }
    }
    public record Bonuses(int shelterPercent, int lightPercent, int caregiverPercent, int minimumLight,
                          int caregiverRadius, List<String> caregiverSpecies,
                          List<String> caregiverAbilities) {
        public Bonuses {
            caregiverSpecies = List.copyOf(caregiverSpecies);
            caregiverAbilities = List.copyOf(caregiverAbilities);
        }
        public boolean qualifies(String species, String ability) {
            return caregiverSpecies.contains(species) || (ability != null && caregiverAbilities.contains(
                    ability.contains(":") ? ability : "cobblemon:" + ability));
        }
    }
    public record Automation(boolean terminalExport, boolean nestIntake) {}

    public static ServerNurserySettings load(Path path, Predicate<String> itemExists, Consumer<String> warn) {
        try {
            if (Files.notExists(path)) {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
                try {
                    Files.writeString(path, defaultJson(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    return DEFAULT;
                } catch (FileAlreadyExistsException ignored) {
                    // Read the file created by someone else without rewriting it.
                }
            }
            return parse(Files.readString(path), itemExists, warn);
        } catch (IOException | SecurityException e) {
            warn.accept("Could not read or create config; using defaults (" + e.getClass().getSimpleName() + ").");
            return DEFAULT;
        }
    }

    private static ServerNurserySettings parse(String json, Predicate<String> itemExists, Consumer<String> warn) {
        JsonObject root;
        try {
            var parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) throw new JsonParseException("Expected an object");
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            warn.accept("Config is not a valid JSON object; using defaults.");
            return DEFAULT;
        }
        var d = DEFAULT;
        var price = group(root, "habitatUnlockCosts", warn);
        var breed = group(root, "breeding", warn);
        var nest = group(root, "incubation", warn);
        var bonus = group(root, "bonuses", warn);
        var auto = group(root, "automation", warn);
        String currency = DEFAULT_ITEM;
        var rawItem = root.get("habitatUnlockItem");
        if (rawItem != null) {
            if (isId(rawItem) && !rawItem.getAsString().equals("minecraft:air") && itemExists.test(rawItem.getAsString())) {
                currency = rawItem.getAsString();
            } else warn.accept("habitatUnlockItem is not a registered item; using " + DEFAULT_ITEM + ".");
        }
        return new ServerNurserySettings(new HabitatUnlockCosts(
                price.integer("yard2", d.costs.yard2(), 0, HabitatUnlockCosts.MAX_COST),
                price.integer("yard3", d.costs.yard3(), 0, HabitatUnlockCosts.MAX_COST),
                price.integer("yard4", d.costs.yard4(), 0, HabitatUnlockCosts.MAX_COST)), currency,
                new Breeding(breed.integer("secondsPerEgg", d.breeding.secondsPerEgg, 1, 86400),
                        breed.integer("eggsPerHabitat", d.breeding.eggsPerHabitat, 1, 64),
                        breed.bool("allowDitto", d.breeding.allowDitto),
                        breed.integer("shinyOddsDenominator", d.breeding.shinyOddsDenominator, 1, 1000000)),
                new Incubation(nest.integer("secondsPerPerfectIv", d.incubation.secondsPerPerfectIv, 0, MAX_IV_SECONDS),
                        nest.integer("minimumSeconds", d.incubation.minimumSeconds, 1, MAX_INCUBATION_SECONDS),
                        nest.integer("eggsPerNest", d.incubation.eggsPerNest, 1, 4)),
                new Bonuses(bonus.integer("shelterPercent", d.bonuses.shelterPercent, 0, 100),
                        bonus.integer("lightPercent", d.bonuses.lightPercent, 0, 100),
                        bonus.integer("caregiverPercent", d.bonuses.caregiverPercent, 0, 100),
                        bonus.integer("minimumLight", d.bonuses.minimumLight, 0, 15),
                        bonus.integer("caregiverRadius", d.bonuses.caregiverRadius, 1, 32),
                        bonus.ids("caregiverSpecies", d.bonuses.caregiverSpecies),
                        bonus.ids("caregiverAbilities", d.bonuses.caregiverAbilities)),
                new Automation(auto.bool("terminalExport", d.automation.terminalExport),
                        auto.bool("nestIntake", d.automation.nestIntake)));
    }

    private static boolean isId(JsonElement item) {
        return item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()
                && item.getAsString().length() <= 256 && ID.matcher(item.getAsString()).matches();
    }

    private static Section group(JsonObject root, String name, Consumer<String> warn) {
        var element = root.get(name);
        if (element != null && !element.isJsonObject()) warn.accept(name + " must be an object; using defaults.");
        return new Section(name, element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject(), warn);
    }

    private record Section(String prefix, JsonObject object, Consumer<String> warn) {
        int integer(String key, int fallback, int min, int max) {
            var element = object.get(key);
            if (element == null) return fallback;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                try {
                    int n = element.getAsBigDecimal().intValueExact();
                    if (n >= min && n <= max) return n;
                } catch (ArithmeticException | NumberFormatException ignored) { }
            }
            warn.accept(prefix + "." + key + " must be an integer from " + min + " to " + max + "; using " + fallback + ".");
            return fallback;
        }
        boolean bool(String key, boolean fallback) {
            var element = object.get(key);
            if (element == null) return fallback;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
            warn.accept(prefix + "." + key + " must be a boolean; using " + fallback + ".");
            return fallback;
        }
        List<String> ids(String key, List<String> fallback) {
            var e = object.get(key);
            if (e == null) return fallback;
            if (e.isJsonArray() && e.getAsJsonArray().size() <= 256) {
                var ids = new LinkedHashSet<String>();
                boolean valid = true;
                for (var element : e.getAsJsonArray()) {
                    if (!isId(element)) { valid = false; break; }
                    ids.add(element.getAsString());
                }
                if (valid) return List.copyOf(ids);
            }
            warn.accept(prefix + "." + key + " must be a list of at most 256 namespaced IDs; using defaults.");
            return fallback;
        }
    }

    private static String defaultJson() {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var blocks = new java.util.StringJoiner(",\n\n", "{\n", "\n}\n");
        blocks.add(configBlock("habitatUnlockItem", gson.toJson(DEFAULT_ITEM),
                "Item used to unlock habitats. Use an item ID like minecraft:emerald."));
        blocks.add(configBlock("habitatUnlockCosts", gson.toJson(DEFAULT.costs),
                "Prices for habitats II–IV, in order. Set a price to 0 to make it free."));
        blocks.add(configBlock("breeding", gson.toJson(DEFAULT.breeding),
                "Egg production, storage limits, Ditto and shiny odds (1 in N)."));
        blocks.add(configBlock("incubation", gson.toJson(DEFAULT.incubation),
                "Timing scales with total IVs, with a minimum. Nest capacity: 1–4 eggs."));
        blocks.add(configBlock("bonuses", gson.toJson(DEFAULT.bonuses),
                "Boost percentages (0% disables), required light, and caregiver range and eligibility."));
        blocks.add(configBlock("automation", gson.toJson(DEFAULT.automation),
                "Allow hoppers to collect from terminals or fill nests."));
        return blocks.toString();
    }

    private static String configBlock(String key, String json, String comment) {
        return "  // " + comment + "\n  \"" + key + "\": " + json.replace("\n", "\n  ");
    }
}
