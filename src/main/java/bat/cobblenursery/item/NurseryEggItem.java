package bat.cobblenursery.item;

import bat.cobblenursery.nursery.model.GeneratedEggData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class NurseryEggItem extends Item {
    private static final String ROOT_TAG = "CobbleNurseryEgg";
    private static final String DATA_TAG = "GeneratedEggData";

    public NurseryEggItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack(Item item, GeneratedEggData eggData) {
        ItemStack stack = new ItemStack(item);
        CompoundTag rootTag = new CompoundTag();
        rootTag.put(DATA_TAG, eggData.save());

        CompoundTag outerTag = new CompoundTag();
        outerTag.put(ROOT_TAG, rootTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(outerTag));
        NurseryEggAppearance appearance = NurseryEggAppearanceResolver.resolve(eggData.childSpeciesId(), eggData.childFormId());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(appearance.modelData()));
        return stack;
    }

    private static final String AUTOMATION_OWNER = "CobbleNurseryAutomationOwner";

    public static void bindAutomationOwner(ItemStack stack, java.util.UUID owner) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putUUID(AUTOMATION_OWNER, owner);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static java.util.UUID automationOwner(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.contains(AUTOMATION_OWNER)) return null;
        var tag = data.copyTag();
        return tag.hasUUID(AUTOMATION_OWNER) ? tag.getUUID(AUTOMATION_OWNER) : null;
    }

    public static Optional<GeneratedEggData> readEggData(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag outerTag = customData.copyTag();
        if (!outerTag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag rootTag = outerTag.getCompound(ROOT_TAG);
        if (!rootTag.contains(DATA_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        return Optional.of(GeneratedEggData.load(rootTag.getCompound(DATA_TAG)));
    }

    public static Optional<GeneratedEggData> readIncubatableData(ItemStack stack) {
        if (!stack.is(bat.cobblenursery.registry.NurseryRegistry.NURSERY_EGG_ITEM)) return Optional.empty();
        try {
            var raw=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                    .getCompound(ROOT_TAG).getCompound(DATA_TAG);
            var ivs=raw.getCompound("ivs");
            for (String stat : List.of("hp","attack","defence","special_attack","special_defence","speed"))
                if (!ivs.contains(stat,Tag.TAG_INT)) return Optional.empty();
            var egg=readEggData(stack).orElse(null);
            if (egg==null || !bat.cobblenursery.nest.NestIncubation.validIvs(egg.ivs()) || egg.inheritedMoveIds().size()>4)
                return Optional.empty();
            var id=ResourceLocation.tryParse(egg.childSpeciesId());
            var species=id==null ? null : com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(id);
            if (species==null || egg.natureId().isBlank() || egg.inheritedAbilityId().isBlank()
                    || ResourceLocation.tryParse(egg.inheritedBallId())==null) return Optional.empty();
            if (!egg.childFormId().isBlank() && !species.getStandardForm().getName().equalsIgnoreCase(egg.childFormId())
                    && species.getForms().stream().noneMatch(form -> form.getName().equalsIgnoreCase(egg.childFormId())))
                return Optional.empty();
            return Optional.of(egg);
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
            serverPlayer.displayClientMessage(Component.translatable("gui.cobblenursery.nest.use_nest"),true);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand),level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        readEggData(stack).ifPresent(eggData -> {
            tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.species", nativeText(eggData.childSpeciesId(), "cobblemon.species.", ".name"))
                .withStyle(ChatFormatting.YELLOW));
            if (!isStandardForm(eggData.childFormId())) {
                tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.form", localizedForm(eggData.childFormId()))
                    .withStyle(ChatFormatting.BLUE));
            }
            tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.nature", nativeText(eggData.natureId(), "cobblemon.nature.", ""))
                .withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.ability", nativeText(eggData.inheritedAbilityId(), "cobblemon.ability.", ""))
                .withStyle(ChatFormatting.GREEN));
            tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.ball", localizedBall(eggData.inheritedBallId()))
                .withStyle(ChatFormatting.GOLD));
            if (eggData.shiny()) {
                tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.shiny")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (!eggData.inheritedMoveIds().isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.cobblenursery.egg.moves", localizedMoves(eggData.inheritedMoveIds()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            tooltipComponents.add(localizedIvs(eggData.ivs()).copy()
                .withStyle(ChatFormatting.GRAY));
        });
    }

    private static Component localizedIvs(Map<String, Integer> ivs) {
        return Component.translatable("tooltip.cobblenursery.egg.ivs",
            ivs.getOrDefault("hp", 0), ivs.getOrDefault("attack", 0), ivs.getOrDefault("defence", 0),
            ivs.getOrDefault("special_attack", 0), ivs.getOrDefault("special_defence", 0),
            ivs.getOrDefault("speed", 0));
    }

    private static Component nativeText(String rawId, String prefix, String suffix) {
        var id = ResourceLocation.tryParse(rawId);
        String path = id == null ? rawId : id.getPath();
        String key = prefix + path + suffix;
        return Component.translatableWithFallback(key, humanizeId(rawId));
    }

    private static Component localizedBall(String rawId) {
        var id = ResourceLocation.tryParse(rawId);
        if (id == null) return Component.literal(humanizeId(rawId));
        // Cobblemon's stored Poké Ball ID and its registered item use the same identifier.
        return Component.translatableWithFallback(id.toLanguageKey("item"), humanizeId(rawId));
    }

    private static Component localizedForm(String rawId) {
        String normalized = rawId.toLowerCase(Locale.ROOT).replace('_', '-');
        String vanillaFormKey = switch (normalized) {
            case "alolan" -> "alola";
            case "galarian" -> "galar";
            case "hisuian" -> "hisui";
            case "paldean" -> "paldea";
            default -> normalized;
        };
        return Component.translatableWithFallback("cobblemon.ui.pokedex.info.form." + vanillaFormKey, humanizeId(rawId));
    }

    private static String humanizeId(String rawId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(rawId);
        String source = resourceLocation != null ? resourceLocation.getPath() : rawId;
        String[] parts = source.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? rawId : builder.toString();
    }

    private static boolean isStandardForm(String rawFormId) {
        if (rawFormId == null || rawFormId.isBlank()) {
            return true;
        }
        String normalized = rawFormId.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.isBlank() || normalized.equals("normal");
    }

    private static Component localizedMoves(List<String> ids) {
        var list = Component.empty();
        boolean first = true;
        for (String id : ids) {
            if (!first) list.append(", ");
            list.append(nativeText(id, "cobblemon.move.", ""));
            first = false;
        }
        return list;
    }

}
