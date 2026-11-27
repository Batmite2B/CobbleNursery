package bat.cobblenursery.nursery.storage;

import bat.cobblenursery.nursery.model.LineageRecord;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LineageSavedData extends SavedData {
    // The old species-scoped file is intentionally left in place and never read.
    public static final String DATA_ID = "cobblenursery_breeding_iv_minimums";

    private static final Factory<LineageSavedData> FACTORY = new Factory<>(
        LineageSavedData::new,
        LineageSavedData::load,
        DataFixTypes.LEVEL
    );

    private final Map<UUID, LineageRecord> minimumsByPlayer = new HashMap<>();

    public static LineageSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public static LineageSavedData find(MinecraftServer server) {
        return server.overworld().getDataStorage().get(FACTORY, DATA_ID);
    }

    public Optional<LineageRecord> find(UUID ownerUuid) {
        return Optional.ofNullable(minimumsByPlayer.get(ownerUuid));
    }

    public void put(LineageRecord record) {
        var previous = minimumsByPlayer.put(record.ownerUuid(), record);
        if (!record.equals(previous)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        minimumsByPlayer.forEach((owner, record) -> playersTag.put(owner.toString(), record.save()));
        tag.put("players", playersTag);
        return tag;
    }

    static LineageSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LineageSavedData data = new LineageSavedData();
        CompoundTag playersTag = tag.getCompound("players");
        for (String playerKey : playersTag.getAllKeys()) {
            var playerTag = playersTag.getCompound(playerKey);
            if (!playerTag.hasUUID("owner_uuid")) {
                continue;
            }
            var record = LineageRecord.load(playerTag);
            if (record.ownerUuid().toString().equals(playerKey)) {
                data.minimumsByPlayer.put(record.ownerUuid(), record);
            }
        }
        return data;
    }
}
