package bat.cobblenursery.nursery.storage;

import bat.cobblenursery.nursery.model.NurseryPlayerState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NurserySavedData extends SavedData {
    public static final String DATA_ID = "cobblenursery_state";

    private static final Factory<NurserySavedData> FACTORY = new Factory<>(
        NurserySavedData::new,
        NurserySavedData::load,
        DataFixTypes.LEVEL
    );

    private final Map<UUID, NurseryPlayerState> statesByPlayer = new HashMap<>();

    // One authoritative destination per player and patio, independent of loaded chunks.
    public record EggOutlet(String dimension, long blockPos, UUID installation) {}
    private final Map<UUID, Map<Integer, EggOutlet>> eggOutlets = new HashMap<>();

    public int eggOutletMask(UUID owner, EggOutlet outlet) {
        if (outlet == null) return 0;
        var routes = eggOutlets.get(owner);
        if (routes == null) return 0;
        int mask = 0;
        for (var route : routes.entrySet()) {
            if (outlet.equals(route.getValue())) mask |= 1 << route.getKey();
        }
        return mask;
    }

    public boolean setEggOutlet(UUID owner, int yard, EggOutlet outlet, boolean enabled) {
        var player = statesByPlayer.get(owner);
        if (outlet == null || player == null || yard < 0 || yard >= NurseryPlayerState.MAX_YARDS
                || yard >= player.unlockedYardSlots()) return false;
        if (enabled) {
            var routes = eggOutlets.computeIfAbsent(owner, ignored -> new HashMap<>());
            if (!outlet.equals(routes.put(yard, outlet))) setDirty();
        } else {
            var routes = eggOutlets.get(owner);
            // A stale off request from an old terminal must never disable the new destination.
            if (routes != null && routes.remove(yard, outlet)) {
                if (routes.isEmpty()) eggOutlets.remove(owner);
                setDirty();
            }
        }
        return true;
    }

    public void removeEggOutlet(UUID owner, EggOutlet outlet) {
        var routes = eggOutlets.get(owner);
        if (outlet == null || routes == null || !routes.values().removeIf(outlet::equals)) return;
        if (routes.isEmpty()) eggOutlets.remove(owner);
        setDirty();
    }

    public static NurserySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public static NurserySavedData find(MinecraftServer server) {
        return server.overworld().getDataStorage().get(FACTORY, DATA_ID);
    }

    public NurseryPlayerState getOrCreate(UUID playerId) {
        return statesByPlayer.computeIfAbsent(playerId, NurseryPlayerState::createDefault);
    }

    public Optional<NurseryPlayerState> find(UUID playerId) {
        return Optional.ofNullable(statesByPlayer.get(playerId));
    }

    public void put(NurseryPlayerState state) {
        statesByPlayer.put(state.playerId(), state);
        setDirty();
    }

    public Map<UUID, NurseryPlayerState> getStatesByPlayer() {
        return statesByPlayer;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, NurseryPlayerState> entry : statesByPlayer.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("players", playersTag);
        CompoundTag outletsTag = new CompoundTag();
        eggOutlets.forEach((owner, routes) -> {
            CompoundTag ownerTag = new CompoundTag();
            routes.forEach((yard, outlet) -> {
                CompoundTag routeTag = new CompoundTag();
                routeTag.putString("dimension", outlet.dimension());
                routeTag.putLong("pos", outlet.blockPos());
                routeTag.putUUID("installation", outlet.installation());
                ownerTag.put(Integer.toString(yard), routeTag);
            });
            outletsTag.put(owner.toString(), ownerTag);
        });
        tag.put("yard_egg_outlets", outletsTag);
        return tag;
    }

    private static NurserySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        NurserySavedData data = new NurserySavedData();
        CompoundTag playersTag = tag.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            data.statesByPlayer.put(UUID.fromString(key), NurseryPlayerState.load(playersTag.getCompound(key)));
        }
        CompoundTag outletsTag = tag.getCompound("yard_egg_outlets");
        data.statesByPlayer.forEach((owner, state) -> {
            CompoundTag ownerTag = outletsTag.getCompound(owner.toString());
            for (int yard = 0; yard < NurseryPlayerState.MAX_YARDS && yard < state.unlockedYardSlots(); yard++) {
                CompoundTag route = ownerTag.getCompound(Integer.toString(yard));
                if (route.hasUUID("installation") && route.contains("pos", 4) && !route.getString("dimension").isEmpty()) {
                    data.eggOutlets.computeIfAbsent(owner, ignored -> new HashMap<>()).put(yard,
                        new EggOutlet(route.getString("dimension"), route.getLong("pos"), route.getUUID("installation")));
                }
            }
        });
        return data;
    }
}
