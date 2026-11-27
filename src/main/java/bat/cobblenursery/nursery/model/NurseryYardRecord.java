package bat.cobblenursery.nursery.model;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public record NurseryYardRecord(
    int yardIndex,
    ParentSnapshot parentA,
    ParentSnapshot parentB,
    BreedingItemSnapshot itemA,
    BreedingItemSnapshot itemB,
    boolean active,
    boolean pausedForEggSpace,
    long cycleStartedAtEpochMs,
    long cycleReadyAtEpochMs,
    long cycleSeed,
    int completedCycles,
    List<NurseryEggTrayEntry> eggs
) {
    public NurseryYardRecord {
        eggs = List.copyOf(eggs);
    }

    public static NurseryYardRecord empty(int yardIndex) {
        return new NurseryYardRecord(
            yardIndex,
            null,
            null,
            BreedingItemSnapshot.EMPTY,
            BreedingItemSnapshot.EMPTY,
            false,
            false,
            0L,
            0L,
            0L,
            0,
            List.of()
        );
    }

    public boolean hasAnyParent() {
        return parentA != null || parentB != null;
    }

    public boolean hasPair() {
        return parentA != null && parentB != null;
    }

    public int eggCount() {
        return eggs.size();
    }

    public NurseryYardRecord withAssignedParent(ParentSnapshot parent, BreedingItemSnapshot item) {
        if (parentA == null) {
            return new NurseryYardRecord(
                yardIndex,
                parent,
                parentB,
                item,
                itemB,
                active,
                pausedForEggSpace,
                cycleStartedAtEpochMs,
                cycleReadyAtEpochMs,
                cycleSeed,
                completedCycles,
                eggs
            );
        }
        if (parentB == null) {
            return new NurseryYardRecord(
                yardIndex,
                parentA,
                parent,
                itemA,
                item,
                active,
                pausedForEggSpace,
                cycleStartedAtEpochMs,
                cycleReadyAtEpochMs,
                cycleSeed,
                completedCycles,
                eggs
            );
        }
        return this;
    }

    public NurseryYardRecord withParents(
        ParentSnapshot nextParentA,
        ParentSnapshot nextParentB,
        BreedingItemSnapshot nextItemA,
        BreedingItemSnapshot nextItemB
    ) {
        return new NurseryYardRecord(
            yardIndex,
            nextParentA,
            nextParentB,
            nextItemA,
            nextItemB,
            false,
            false,
            0L,
            0L,
            0L,
            completedCycles,
            eggs
        );
    }

    public NurseryYardRecord withParentAt(boolean slotB, ParentSnapshot parent, BreedingItemSnapshot item) {
        return new NurseryYardRecord(yardIndex, slotB ? parentA : parent, slotB ? parent : parentB,
                slotB ? itemA : item, slotB ? item : itemB,
                false, false, 0L, 0L, 0L, completedCycles, eggs);
    }

    public NurseryYardRecord clearParents() {
        return new NurseryYardRecord(
            yardIndex,
            null,
            null,
            BreedingItemSnapshot.EMPTY,
            BreedingItemSnapshot.EMPTY,
            false,
            false,
            0L,
            0L,
            0L,
            completedCycles,
            eggs
        );
    }

    public NurseryYardRecord startCycle(long startedAtEpochMs, long readyAtEpochMs, long seed) {
        return new NurseryYardRecord(
            yardIndex,
            parentA,
            parentB,
            itemA,
            itemB,
            true,
            false,
            startedAtEpochMs,
            readyAtEpochMs,
            seed,
            completedCycles,
            eggs
        );
    }

    public NurseryYardRecord pauseForEggSpace() {
        return new NurseryYardRecord(
            yardIndex,
            parentA,
            parentB,
            itemA,
            itemB,
            false,
            true,
            cycleStartedAtEpochMs,
            cycleReadyAtEpochMs,
            cycleSeed,
            completedCycles,
            eggs
        );
    }

    public NurseryYardRecord stopCycle() {
        return new NurseryYardRecord(
            yardIndex,
            parentA,
            parentB,
            itemA,
            itemB,
            false,
            false,
            0L,
            0L,
            cycleSeed,
            completedCycles,
            eggs
        );
    }

    public NurseryYardRecord markCycleCompleted() {
        return new NurseryYardRecord(
            yardIndex,
            parentA,
            parentB,
            itemA,
            itemB,
            active,
            pausedForEggSpace,
            cycleStartedAtEpochMs,
            cycleReadyAtEpochMs,
            cycleSeed,
            completedCycles + 1,
            eggs
        );
    }

    public NurseryYardRecord addEgg(NurseryEggTrayEntry egg) {
        List<NurseryEggTrayEntry> nextEggs = new ArrayList<>(eggs);
        nextEggs.add(egg);
        return withEggs(nextEggs);
    }

    public NurseryYardRecord withEggs(List<NurseryEggTrayEntry> nextEggs) {
        return new NurseryYardRecord(
            yardIndex,
            parentA,
            parentB,
            itemA,
            itemB,
            active,
            pausedForEggSpace,
            cycleStartedAtEpochMs,
            cycleReadyAtEpochMs,
            cycleSeed,
            completedCycles,
            nextEggs
        );
    }

    public NurseryYardRecord clearEggs() {
        return withEggs(List.of());
    }

    public boolean isCycleReady(long now) {
        return active && cycleReadyAtEpochMs > 0L && now >= cycleReadyAtEpochMs;
    }

    public float cycleProgressAt(long now) {
        if (!active || cycleReadyAtEpochMs <= cycleStartedAtEpochMs) return 0F;
        return (float)Math.clamp(((double)now - cycleStartedAtEpochMs)
                / ((double)cycleReadyAtEpochMs - cycleStartedAtEpochMs), 0.0, 1.0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("yard_index", yardIndex);
        if (parentA != null) {
            tag.put("parent_a", parentA.save());
        }
        if (parentB != null) {
            tag.put("parent_b", parentB.save());
        }
        tag.put("item_a", itemA.save());
        tag.put("item_b", itemB.save());
        tag.putBoolean("active", active);
        tag.putBoolean("paused_for_egg_space", pausedForEggSpace);
        tag.putLong("cycle_started_at", cycleStartedAtEpochMs);
        tag.putLong("cycle_ready_at", cycleReadyAtEpochMs);
        tag.putLong("cycle_seed", cycleSeed);
        tag.putInt("completed_cycles", completedCycles);

        CompoundTag eggsTag = new CompoundTag();
        for (int i = 0; i < eggs.size(); i++) {
            eggsTag.put(String.valueOf(i), eggs.get(i).save());
        }
        tag.put("eggs", eggsTag);
        return tag;
    }

    public static NurseryYardRecord load(CompoundTag tag) {
        List<NurseryEggTrayEntry> eggs = new ArrayList<>();
        if (tag.contains("eggs")) {
            CompoundTag eggsTag = tag.getCompound("eggs");
            for (String key : eggsTag.getAllKeys()) {
                eggs.add(NurseryEggTrayEntry.load(eggsTag.getCompound(key)));
            }
        }

        return new NurseryYardRecord(
            tag.getInt("yard_index"),
            tag.contains("parent_a") ? ParentSnapshot.load(tag.getCompound("parent_a")) : null,
            tag.contains("parent_b") ? ParentSnapshot.load(tag.getCompound("parent_b")) : null,
            BreedingItemSnapshot.load(tag.getCompound("item_a")),
            BreedingItemSnapshot.load(tag.getCompound("item_b")),
            tag.getBoolean("active"),
            tag.getBoolean("paused_for_egg_space"),
            tag.getLong("cycle_started_at"),
            tag.getLong("cycle_ready_at"),
            tag.getLong("cycle_seed"),
            tag.getInt("completed_cycles"),
            eggs
        );
    }
}
