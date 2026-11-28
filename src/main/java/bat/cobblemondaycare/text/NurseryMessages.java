package bat.cobblemondaycare.text;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Renders player feedback in the recipient's locale without changing legacy packets or their unlock acknowledgement. */
public final class NurseryMessages {
    private static final String PREFIX = "gui.cobblemondaycare.feedback.";
    private static final Map<String,String> FIXED = Map.ofEntries(
        Map.entry("missing_parent", "missing_parent"),
        Map.entry("undiscovered_egg_group", "undiscovered_egg_group"),
        Map.entry("same_pokemon", "same_pokemon"),
        Map.entry("ditto_pair_not_allowed", "ditto_pair_not_allowed"),
        Map.entry("ditto_disabled", "ditto_disabled"),
        Map.entry("parents_must_have_opposite_genders", "parents_must_have_opposite_genders"),
        Map.entry("no_shared_egg_group", "no_shared_egg_group"),
        Map.entry("compatible", "compatible"),
        Map.entry("No nursery process was found in that slot.", "result_8"),
        Map.entry("That nursery slot is already closed.", "result_9"),
        Map.entry("Could not return the parents to your party/PC.", "result_10"),
        Map.entry("Nursery process cancelled and parents returned.", "result_11"),
        Map.entry("Nursery process is already ready.", "result_12"),
        Map.entry("Nursery process forced to READY for testing.", "result_13"),
        Map.entry("Invalid yard selection.", "result_14"),
        Map.entry("Invalid yard.", "result_15"),
        Map.entry("This yard is still locked.", "result_16"),
        Map.entry("That yard already has a full pair assigned.", "result_17"),
        Map.entry("There is no Pokemon in that party slot.", "result_18"),
        Map.entry("Could not move that Pokemon into the yard.", "result_19"),
        Map.entry("That yard is already empty.", "result_20"),
        Map.entry("Could not return the yard pair to your party/PC.", "result_21"),
        Map.entry("Invalid move destination.", "result_22"),
        Map.entry("Select an unlocked habitat first.", "result_23"),
        Map.entry("The Pokemon or target space changed. Try again.", "result_24"),
        Map.entry("The Pokemon or party slot changed. Try again.", "result_25"),
        Map.entry("That Pokemon is already in your party.", "result_26"),
        Map.entry("Could not restore that Pokemon.", "result_27"),
        Map.entry("Party storage did not confirm the move. The habitat was not changed.", "result_28"),
        Map.entry("Could not move that Pokemon. The habitat was not changed.", "result_29"),
        Map.entry("Select the unlocked habitat before stopping breeding.", "result_30"),
        Map.entry("That habitat is not breeding.", "result_31"),
        Map.entry("Unlock that habitat before starting.", "result_32"),
        Map.entry("That yard needs two parents before it can start.", "result_33"),
        Map.entry("That yard is already breeding.", "result_34"),
        Map.entry("That yard is full. Claim its eggs first.", "result_35"),
        Map.entry("That yard is already unlocked.", "result_36"),
        Map.entry("Unlock the previous yard first.", "result_37"),
        Map.entry("Could not pay the yard unlock fee.", "result_38"),
        Map.entry("That yard is not currently breeding.", "result_39"),
        Map.entry("That yard is no longer selected or unlocked.", "result_40"),
        Map.entry("There are no eggs waiting in that yard.", "result_41"),
        Map.entry("Make room in your inventory for an egg.", "result_42"),
        Map.entry("Choose two different party slots.", "result_43"),
        Map.entry("You have no free nursery slots.", "result_44"),
        Map.entry("Invalid party slots for nursery start.", "result_45"),
        Map.entry("You do not have enough batcg:pokedollar.", "result_46"),
        Map.entry("One of the selected parents is no longer in the party.", "result_47"),
        Map.entry("Could not withdraw the nursery fee.", "result_48"),
        Map.entry("Could not remove the selected parents from the party.", "result_49"),
        Map.entry("Nursery process started.", "result_50"),
        Map.entry("Could not return the parents to your party/PC, so the egg was not claimed.", "result_51"),
        Map.entry("You do not have permission to force nursery completion.", "result_52")
    );
    private record Template(Pattern pattern, String key) {
        Template(String pattern, String key) { this(Pattern.compile(pattern), key); }
    }
    private static final List<Template> TEMPLATES = List.of(
        new Template("Selected yard (\\d+)\\.", "selected"),
        new Template("Assigned (.+) to yard (\\d+)\\.", "assigned"),
        new Template("Returned the pair from yard (\\d+)\\.", "returned"),
        new Template("Started breeding in yard (\\d+)\\.", "started"),
        new Template("You need (\\d+) relic coins to unlock this yard\\.", "cost"),
        new Template("You need (\\d+) of ([a-z0-9_.-]+:[a-z0-9/._-]+) to unlock this yard\\.", "cost_item"),
        new Template("Unlocked yard (\\d+)\\.", "unlocked"),
        new Template("Forced yard (\\d+) to finish its current cycle\\.", "forced"),
        new Template("Claimed (\\d+) egg\\(s\\) from yard (\\d+)\\.", "claimed"),
        new Template("Nursery egg claimed from slot (\\d+) and parents returned\\.", "claim_one"),
        new Template("This nursery process is not ready yet\\. Remaining: (\\d+) minute\\(s\\)\\.", "waiting"),
        new Template("(?:This pair cannot breed: |Breeding failed: )([a-z_]+)", "cannot_breed")
    );
    private NurseryMessages() {}

    public static Component itemName(String itemId) {
        var id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (id == null) return Component.literal(itemId);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id)
                .<Component>map(item -> new net.minecraft.world.item.ItemStack(item).getHoverName())
                .orElseGet(() -> Component.literal(itemId));
    }

    public static MutableComponent component(String message) {
        if (message == null || message.isBlank()) return Component.empty();
        String key = FIXED.get(message);
        if (key != null) return Component.translatable(PREFIX + key);
        for (Template template : TEMPLATES) {
            var match = template.pattern.matcher(message);
            if (!match.matches()) continue;
            Object[] args = new Object[match.groupCount()];
            for (int i = 0; i < args.length; i++) args[i] = match.group(i + 1);
            if ("cannot_breed".equals(template.key)) args[0] = component(match.group(1));
            if ("cost_item".equals(template.key)) args[1] = itemName(match.group(2));
            return Component.translatable(PREFIX + template.key, args);
        }
        return Component.literal(message);
    }
}
