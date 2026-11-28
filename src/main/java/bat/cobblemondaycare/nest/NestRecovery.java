package bat.cobblemondaycare.nest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Only used when spawning an owner-protected recovery drop is not possible. */
final class NestRecovery extends SavedData {
    private static final String ID="cobblemondaycare_pending_nest_recovery";
    private static final Factory<NestRecovery> FACTORY=new Factory<>(NestRecovery::new,NestRecovery::load,DataFixTypes.LEVEL);
    private final HashMap<UUID,List<ItemStack>> pending=new HashMap<>();

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount()%20!=0) return;
            var data=server.overworld().getDataStorage().get(FACTORY,ID);
            if (data==null || data.pending.isEmpty()) return;
            for (var player:server.getPlayerList().getPlayers()) data.deliver(player);
        });
    }

    static void defer(MinecraftServer server,UUID owner,ItemStack egg) {
        var data=server.overworld().getDataStorage().computeIfAbsent(FACTORY,ID);
        data.pending.computeIfAbsent(owner,ignored->new ArrayList<>()).add(egg.copy());
        data.setDirty();
    }

    private void deliver(ServerPlayer player) {
        var eggs=pending.get(player.getUUID());
        if (eggs==null) return;
        while (!eggs.isEmpty() && putInInventory(player,eggs.getFirst())) {
            eggs.removeFirst();
            setDirty();
        }
        if (eggs.isEmpty()) pending.remove(player.getUUID());
    }

    static boolean putInInventory(ServerPlayer player,ItemStack egg) {
        var inventory=player.getInventory();
        int slot=inventory.getFreeSlot();
        if (slot<0 || !inventory.getItem(slot).isEmpty()) return false;
        // Inventory.add can void an uninserted stack in Creative.
        inventory.setItem(slot,egg);
        if (inventory.getItem(slot)!=egg) return false;
        inventory.setChanged();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        pending.forEach((id,eggs)-> {
            var list=new ListTag();
            for(var egg:eggs) list.add(egg.save(registries));
            tag.put(id.toString(),list);
        });
        return tag;
    }

    static NestRecovery load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new NestRecovery();
        for(String key:tag.getAllKeys()) {
            UUID id;
            try { id=UUID.fromString(key); } catch(IllegalArgumentException invalid) { continue; }
            var eggs=new ArrayList<ItemStack>();
            var list=tag.getList(key,Tag.TAG_COMPOUND);
            for(int i=0;i<list.size();i++) {
                var egg=ItemStack.parseOptional(registries,list.getCompound(i));
                if (!egg.isEmpty()) eggs.add(egg);
            }
            if (!eggs.isEmpty()) data.pending.put(id,eggs);
        }
        return data;
    }
}
