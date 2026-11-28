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
import java.util.function.Consumer;

public record HabitatUnlockCosts(int yard2, int yard3, int yard4) {
    public static final String FILE_NAME = "cobblemondaycare.json";
    public static final int MAX_COST = 1_000_000;
    public static final HabitatUnlockCosts DEFAULT = new HabitatUnlockCosts(64, 128, 192);
    private static final String GROUP = "habitatUnlockCosts";

    public int forYardIndex(int yardIndex) {
        return switch (yardIndex) {
            case 0 -> 0;
            case 1 -> yard2;
            case 2 -> yard3;
            case 3 -> yard4;
            default -> throw new IllegalArgumentException("Invalid habitat index: " + yardIndex);
        };
    }

    public static HabitatUnlockCosts load(Path file, Consumer<String> warn) {
        try {
            if (Files.notExists(file)) {
                Path parent = file.getParent();
                if (parent != null) Files.createDirectories(parent);
                try {
                    Files.writeString(file, toDefaultJson(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    return DEFAULT;
                } catch (FileAlreadyExistsException ignored) {
                    // A file appeared since the existence check. Read it without modifying it.
                }
            }
            return parse(Files.readString(file), warn);
        } catch (IOException | SecurityException e) {
            warn.accept("Could not read or create the config; using default habitat costs (" +
                    e.getClass().getSimpleName() + ").");
            return DEFAULT;
        }
    }

    static HabitatUnlockCosts parse(String json, Consumer<String> warn) {
        final JsonElement document;
        try {
            document = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            warn.accept("Config is not valid JSON; using default habitat costs.");
            return DEFAULT;
        }
        if (!document.isJsonObject()) {
            warn.accept("Config must contain a JSON object; using default habitat costs.");
            return DEFAULT;
        }

        JsonElement group = document.getAsJsonObject().get(GROUP);
        if (group == null || !group.isJsonObject()) {
            warn.accept(GROUP + " is missing or is not an object; using default habitat costs.");
            return DEFAULT;
        }
        JsonObject costs = group.getAsJsonObject();
        return new HabitatUnlockCosts(
                readCost(costs, "yard2", DEFAULT.yard2(), warn),
                readCost(costs, "yard3", DEFAULT.yard3(), warn),
                readCost(costs, "yard4", DEFAULT.yard4(), warn)
        );
    }

    private static int readCost(JsonObject object, String key, int fallback, Consumer<String> warn) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            try {
                int amount = value.getAsBigDecimal().intValueExact();
                if (amount >= 0 && amount <= MAX_COST) return amount;
            } catch (NumberFormatException | ArithmeticException ignored) {
                // Nonintegral and oversized values fall through to a safe default.
            }
        }
        warn.accept(GROUP + "." + key + " must be an integer from 0 to " + MAX_COST +
                "; using " + fallback + ".");
        return fallback;
    }

    private static String toDefaultJson() {
        JsonObject costs = new JsonObject();
        costs.addProperty("yard2", DEFAULT.yard2());
        costs.addProperty("yard3", DEFAULT.yard3());
        costs.addProperty("yard4", DEFAULT.yard4());
        JsonObject root = new JsonObject();
        root.add(GROUP, costs);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + System.lineSeparator();
    }
}
