package bat.cobblemondaycare.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PokeCenterTerminalTest {
    static final ResourceLocation PLAINS = ResourceLocation.fromNamespaceAndPath("cobblemon","village_plains/village_plains_pokecenter");
    static CompoundTag data(String atTarget) {
        var tag=new CompoundTag();
        var palette=new ListTag();
        for(String block:new String[]{atTarget,"minecraft:smooth_stone","cobblemon:healing_machine","minecraft:air"}) {
            var entry=new CompoundTag();entry.putString("Name",block);palette.add(entry);
        }
        tag.put("palette",palette);
        var blocks=new ListTag();
        blocks.add(block(5,3,3,0));blocks.add(block(5,2,3,1));blocks.add(block(7,2,2,2));
        blocks.add(block(4,2,3,3));blocks.add(block(4,3,3,3));blocks.add(block(5,4,3,3));
        tag.put("blocks",blocks);
        tag.put("entities",new ListTag());
        return tag;
    }
    static CompoundTag block(int x,int y,int z,int state){
        var t=new CompoundTag();var p=new ListTag();p.add(IntTag.valueOf(x));p.add(IntTag.valueOf(y));p.add(IntTag.valueOf(z));t.put("pos",p);t.putInt("state",state);return t;
    }

    @Test void onlyKnownAirCellGetsPublicTerminal() {
        var tag=data("minecraft:air");
        var old=tag.copy();
        assertTrue(PokeCenterTerminal.add(PLAINS,tag));
        var palette=tag.getList("palette",10);
        assertEquals(5,palette.size());
        var terminal=palette.getCompound(4);
        assertEquals("cobblemondaycare:nursery_terminal",terminal.getString("Name"));
        assertEquals("west",terminal.getCompound("Properties").getString("facing"));
        assertEquals(block(5,3,3,4),tag.getList("blocks",10).getCompound(0));
        for(int i=1;i<6;i++)assertEquals(old.getList("blocks",10).get(i),tag.getList("blocks",10).get(i));
        assertFalse(PokeCenterTerminal.add(PLAINS,tag));
    }

    @Test void unrelatedAndCustomizedTemplatesStayUntouched() {
        var occupied=data("minecraft:chest");var copy=occupied.copy();
        assertFalse(PokeCenterTerminal.add(PLAINS,occupied));
        assertEquals(copy,occupied);
        var original=data("minecraft:air");var unchanged=original.copy();
        assertFalse(PokeCenterTerminal.add(ResourceLocation.parse("other:village_plains/village_plains_pokecenter"),original));
        assertEquals(unchanged,original);
        original.getList("blocks",10).getCompound(3).putInt("state",1);
        var blocked=original.copy();
        assertFalse(PokeCenterTerminal.add(PLAINS,original));
        assertEquals(blocked,original);
    }
}