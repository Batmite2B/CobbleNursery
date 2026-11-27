package bat.cobblenursery.nursery.model;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record NurseryEggTrayEntry(
    UUID entryId,
    int sourceYardIndex,
    GeneratedEggData eggData,
    long createdAtEpochMs
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("entry_id", entryId);
        tag.putInt("source_yard_index", sourceYardIndex);
        tag.put("egg_data", eggData.save());
        tag.putLong("created_at", createdAtEpochMs);
        return tag;
    }

    public static NurseryEggTrayEntry load(CompoundTag tag) {
        return new NurseryEggTrayEntry(
            tag.getUUID("entry_id"),
            tag.getInt("source_yard_index"),
            GeneratedEggData.load(tag.getCompound("egg_data")),
            tag.getLong("created_at")
        );
    }
}
