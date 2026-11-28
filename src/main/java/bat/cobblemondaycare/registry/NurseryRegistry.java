package bat.cobblemondaycare.registry;

import bat.cobblemondaycare.CobblemonDaycare;
import bat.cobblemondaycare.block.NurseryBlock;
import bat.cobblemondaycare.block.NurseryTerminalBlock;
import bat.cobblemondaycare.terminal.NurseryTerminalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import bat.cobblemondaycare.item.NurseryEggItem;
import bat.cobblemondaycare.item.NurseryNurseItem;
import bat.cobblemondaycare.nursery.model.GeneratedEggData;
import bat.cobblemondaycare.entity.NurseryNurseEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Map;

public final class NurseryRegistry {
    public static final EntityType<NurseryNurseEntity> NURSERY_NURSE_ENTITY = EntityType.Builder
        .of(NurseryNurseEntity::new, MobCategory.MISC)
        .sized(0.6F, 1.95F)
        .eyeHeight(1.62F)
        .build("cobblemondaycare:nursery_nurse");

    public static final Block NURSERY_BLOCK = new NurseryBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
    );

    public static final Block NURSERY_TERMINAL = new NurseryTerminalBlock(
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F, 6.0F)
            .sound(SoundType.METAL).noOcclusion()
    );

    public static final BlockEntityType<NurseryTerminalBlockEntity> NURSERY_TERMINAL_ENTITY =
        BlockEntityType.Builder.of(NurseryTerminalBlockEntity::new, NURSERY_TERMINAL).build(null);

    public static final Block INCUBATION_NEST = new bat.cobblemondaycare.nest.IncubationNestBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(.6F)
                    .sound(SoundType.GRASS).noOcclusion().dynamicShape()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final BlockEntityType<bat.cobblemondaycare.nest.IncubationNestBlockEntity> INCUBATION_NEST_ENTITY =
            BlockEntityType.Builder.of(bat.cobblemondaycare.nest.IncubationNestBlockEntity::new,INCUBATION_NEST).build(null);
    public static final Item INCUBATION_NEST_ITEM = new bat.cobblemondaycare.nest.IncubationNestItem((bat.cobblemondaycare.nest.IncubationNestBlock) INCUBATION_NEST,new Item.Properties());

    public static final Item NURSERY_TERMINAL_ITEM = new BlockItem(NURSERY_TERMINAL, new Item.Properties());

    public static final Item NURSERY_NURSE_ITEM = new NurseryNurseItem(
        new Item.Properties()
    );

    public static final Item NURSERY_EGG_ITEM = new NurseryEggItem(
        new Item.Properties().stacksTo(1)
    );

    public static final CreativeModeTab ITEM_GROUP = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup.cobblemondaycare"))
        .icon(() -> NURSERY_TERMINAL_ITEM.getDefaultInstance())
        .displayItems((parameters, output) -> {
            output.accept(NURSERY_TERMINAL_ITEM);
            output.accept(INCUBATION_NEST_ITEM);
            output.accept(createNurseryEgg(new GeneratedEggData(
                "cobblemon:eevee",
                "Normal",
                "hardy",
                "adaptability",
                "cobblemon:poke_ball",
                java.util.List.of("wish"),
                Map.of(
                    "hp", 10,
                    "attack", 10,
                    "defence", 10,
                    "special_attack", 10,
                    "special_defence", 10,
                    "speed", 10
                ),
                false
            )));
        })
        .build();

    private NurseryRegistry() {
    }

    public static void register() {
        registerEntityType("nursery_nurse", NURSERY_NURSE_ENTITY);
        registerBlock("nursery", NURSERY_BLOCK);
        registerItem("nursery", NURSERY_NURSE_ITEM);
        registerItem("nursery_egg", NURSERY_EGG_ITEM);
        registerBlock("nursery_terminal", NURSERY_TERMINAL);
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("nursery_terminal"), NURSERY_TERMINAL_ENTITY);
        registerItem("nursery_terminal", NURSERY_TERMINAL_ITEM);
        registerBlock("incubation_nest", INCUBATION_NEST);
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("incubation_nest"),INCUBATION_NEST_ENTITY);
        registerItem("incubation_nest", INCUBATION_NEST_ITEM);
        bat.cobblemondaycare.nest.IncubationNestBlock.protectOccupiedBreaks();
        FabricDefaultAttributeRegistry.register(NURSERY_NURSE_ENTITY, NurseryNurseEntity.createAttributes());
        net.minecraft.core.Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            id("main"),
            ITEM_GROUP
        );
    }

    private static void registerBlock(String path, Block block) {
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, id(path), block);
    }

    private static void registerItem(String path, Item item) {
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, id(path), item);
    }

    private static void registerEntityType(String path, EntityType<?> entityType) {
        net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, id(path), entityType);
    }

    public static ItemStack createNurseryEgg(GeneratedEggData eggData) {
        return NurseryEggItem.createStack(NURSERY_EGG_ITEM, eggData);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, path);
    }
}
