package bat.cobblemondaycare.nursery.model;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record NurseryProcessRecord(
    UUID processId,
    int slotIndex,
    NurseryProcessStatus status,
    ParentSnapshot parentA,
    ParentSnapshot parentB,
    BreedingItemSnapshot itemA,
    BreedingItemSnapshot itemB,
    long startedAtEpochMs,
    long readyAtEpochMs,
    long breedingSeed,
    GeneratedEggData generatedEgg
) {
    public boolean isReady(long now) {
        return status == NurseryProcessStatus.BREEDING && now >= readyAtEpochMs;
    }

    public NurseryProcessRecord withStatus(NurseryProcessStatus nextStatus) {
        return new NurseryProcessRecord(
            processId,
            slotIndex,
            nextStatus,
            parentA,
            parentB,
            itemA,
            itemB,
            startedAtEpochMs,
            readyAtEpochMs,
            breedingSeed,
            generatedEgg
        );
    }

    public NurseryProcessRecord withGeneratedEgg(GeneratedEggData eggData) {
        return new NurseryProcessRecord(
            processId,
            slotIndex,
            NurseryProcessStatus.READY,
            parentA,
            parentB,
            itemA,
            itemB,
            startedAtEpochMs,
            readyAtEpochMs,
            breedingSeed,
            eggData
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("process_id", processId);
        tag.putInt("slot_index", slotIndex);
        tag.putString("status", status.name());
        tag.put("parent_a", parentA.save());
        tag.put("parent_b", parentB.save());
        tag.put("item_a", itemA.save());
        tag.put("item_b", itemB.save());
        tag.putLong("started_at", startedAtEpochMs);
        tag.putLong("ready_at", readyAtEpochMs);
        tag.putLong("breeding_seed", breedingSeed);
        if (generatedEgg != null) {
            tag.put("generated_egg", generatedEgg.save());
        }
        return tag;
    }

    public static NurseryProcessRecord load(CompoundTag tag) {
        GeneratedEggData generatedEgg = tag.contains("generated_egg")
            ? GeneratedEggData.load(tag.getCompound("generated_egg"))
            : null;

        return new NurseryProcessRecord(
            tag.getUUID("process_id"),
            tag.getInt("slot_index"),
            NurseryProcessStatus.valueOf(tag.getString("status")),
            ParentSnapshot.load(tag.getCompound("parent_a")),
            ParentSnapshot.load(tag.getCompound("parent_b")),
            BreedingItemSnapshot.load(tag.getCompound("item_a")),
            BreedingItemSnapshot.load(tag.getCompound("item_b")),
            tag.getLong("started_at"),
            tag.getLong("ready_at"),
            tag.getLong("breeding_seed"),
            generatedEgg
        );
    }
}
