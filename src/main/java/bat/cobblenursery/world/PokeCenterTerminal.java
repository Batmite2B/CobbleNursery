package bat.cobblenursery.world;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** A small, guarded edit to the five Cobblemon Pokecenters in vanilla village pools. */
public final class PokeCenterTerminal {
    private record Counter(BlockPos terminal, BlockPos healer, String surface) {}
    private static final String AIR = "minecraft:air";
    private static final Map<String, Counter> SITES=Map.of(
            "village_plains/village_plains_pokecenter", new Counter(new BlockPos(5,3,3), new BlockPos(7,2,2), "minecraft:smooth_stone"),
            "village_desert/village_desert_pokecenter", new Counter(new BlockPos(6,2,7), new BlockPos(8,1,7), "minecraft:cut_sandstone"),
            "village_savanna/village_savanna_pokecenter", new Counter(new BlockPos(7,3,6), new BlockPos(10,2,6), "minecraft:polished_granite"),
            "village_snowy/village_snowy_pokecenter", new Counter(new BlockPos(6,3,6), new BlockPos(8,2,6), "minecraft:deepslate_bricks"),
            "village_taiga/village_taiga_pokecenter", new Counter(new BlockPos(6,3,7), new BlockPos(7,2,6), "minecraft:stone_bricks"));

    private PokeCenterTerminal() {}

    public static boolean isTarget(ResourceLocation id) {
        return id.getNamespace().equals("cobblemon") && SITES.containsKey(id.getPath());
    }

    public static boolean add(ResourceLocation id, CompoundTag template) {
        if (!isTarget(id) || template.contains("palettes")) return false;
        var site=SITES.get(id.getPath());
        var pos=site.terminal();
        var palette=template.getList("palette",Tag.TAG_COMPOUND);
        var blocks=template.getList("blocks",Tag.TAG_COMPOUND);
        var target=at(blocks,pos);
        if (!AIR.equals(name(palette,target)) || target.contains("nbt")
                || !site.surface().equals(name(palette,at(blocks,pos.below())))
                || !"cobblemon:healing_machine".equals(name(palette,at(blocks,site.healer())))
                || !AIR.equals(name(palette,at(blocks,pos.above())))
                || !AIR.equals(name(palette,at(blocks,pos.west())))
                || !AIR.equals(name(palette,at(blocks,pos.west().below())))) return false;
        var terminal=new CompoundTag();
        terminal.putString("Name","cobblenursery:nursery_terminal");
        var properties=new CompoundTag();
        properties.putString("facing","west");
        terminal.put("Properties",properties);
        target.putInt("state",palette.size());
        palette.add(terminal);
        // No placer/owner NBT. A village terminal is a public machine with no hopper output.
        return true;
    }

    private static CompoundTag at(ListTag blocks, BlockPos pos) {
        for (Tag entry:blocks) {
            var row=(CompoundTag)entry;
            var coord=row.getList("pos",Tag.TAG_INT);
            if (coord.size()==3 && coord.getInt(0)==pos.getX() && coord.getInt(1)==pos.getY()
                    && coord.getInt(2)==pos.getZ()) return row;
        }
        return null;
    }

    private static String name(ListTag palette, CompoundTag row) {
        if (row==null || !row.contains("state",Tag.TAG_INT)) return "";
        int state=row.getInt("state");
        return state>=0 && state<palette.size() ? palette.getCompound(state).getString("Name") : "";
    }
}