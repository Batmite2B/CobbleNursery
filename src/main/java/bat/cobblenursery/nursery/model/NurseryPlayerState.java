package bat.cobblenursery.nursery.model;

import bat.cobblenursery.config.NurseryConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record NurseryPlayerState(
    UUID playerId,
    int unlockedProcessSlots,
    List<NurseryProcessRecord> processes,
    int unlockedYardSlots,
    int selectedYardIndex,
    List<NurseryYardRecord> yards
) {
    public static final int MAX_YARDS = 4;

    public static NurseryPlayerState createDefault(UUID playerId) {
        return new NurseryPlayerState(
            playerId,
            NurseryConfig.DEFAULT.baseProcessSlots(),
            new ArrayList<>(),
            1,
            0,
            defaultYards()
        );
    }

    public NurseryPlayerState upsertProcess(NurseryProcessRecord record) {
        List<NurseryProcessRecord> next = new ArrayList<>(processes);
        next.removeIf(existing -> existing.processId().equals(record.processId()) || existing.slotIndex() == record.slotIndex());
        next.add(record);
        next.sort(Comparator.comparingInt(NurseryProcessRecord::slotIndex));
        return new NurseryPlayerState(playerId, unlockedProcessSlots, next, unlockedYardSlots, selectedYardIndex, yards);
    }

    public Optional<NurseryProcessRecord> findProcessBySlot(int slotIndex) {
        return processes.stream()
            .filter(process -> process.slotIndex() == slotIndex)
            .findFirst();
    }

    public int firstAvailableProcessSlot() {
        for (int slotIndex = 0; slotIndex < unlockedProcessSlots; slotIndex++) {
            NurseryProcessRecord process = findProcessBySlot(slotIndex).orElse(null);
            if (process == null || isReusable(process)) {
                return slotIndex;
            }
        }
        return -1;
    }

    public boolean isReusable(NurseryProcessRecord process) {
        return process.status() == NurseryProcessStatus.EMPTY
            || process.status() == NurseryProcessStatus.CANCELLED
            || process.status() == NurseryProcessStatus.CLAIMED;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player_id", playerId);
        tag.putInt("unlocked_process_slots", unlockedProcessSlots);
        tag.putInt("unlocked_yard_slots", unlockedYardSlots);
        tag.putInt("selected_yard_index", selectedYardIndex);

        CompoundTag processTag = new CompoundTag();
        for (int i = 0; i < processes.size(); i++) {
            processTag.put(String.valueOf(i), processes.get(i).save());
        }
        tag.put("processes", processTag);

        CompoundTag yardTag = new CompoundTag();
        for (int i = 0; i < yards.size(); i++) {
            yardTag.put(String.valueOf(i), yards.get(i).save());
        }
        tag.put("yards", yardTag);
        return tag;
    }

    public static NurseryPlayerState load(CompoundTag tag) {
        List<NurseryProcessRecord> processes = new ArrayList<>();
        CompoundTag processTag = tag.getCompound("processes");
        for (String key : processTag.getAllKeys()) {
            processes.add(NurseryProcessRecord.load(processTag.getCompound(key)));
        }

        List<NurseryYardRecord> yards = defaultYards();
        if (tag.contains("yards")) {
            CompoundTag yardTag = tag.getCompound("yards");
            yards = new ArrayList<>();
            for (String key : yardTag.getAllKeys()) {
                yards.add(NurseryYardRecord.load(yardTag.getCompound(key)));
            }
            yards.sort(Comparator.comparingInt(NurseryYardRecord::yardIndex));
            while (yards.size() < MAX_YARDS) {
                yards.add(NurseryYardRecord.empty(yards.size()));
            }
        }

        List<NurseryEggTrayEntry> legacyEggTray = new ArrayList<>();
        if (tag.contains("egg_tray")) {
            CompoundTag eggTrayTag = tag.getCompound("egg_tray");
            for (String key : eggTrayTag.getAllKeys()) {
                legacyEggTray.add(NurseryEggTrayEntry.load(eggTrayTag.getCompound(key)));
            }
            yards = migrateLegacyEggTray(yards, legacyEggTray);
        }

        return new NurseryPlayerState(
            tag.getUUID("player_id"),
            tag.getInt("unlocked_process_slots"),
            processes,
            tag.contains("unlocked_yard_slots") ? tag.getInt("unlocked_yard_slots") : 1,
            tag.contains("selected_yard_index") ? tag.getInt("selected_yard_index") : 0,
            yards
        );
    }

    public NurseryPlayerState withSelectedYard(int nextSelectedYardIndex) {
        int clamped = Math.max(0, Math.min(MAX_YARDS - 1, nextSelectedYardIndex));
        return new NurseryPlayerState(playerId, unlockedProcessSlots, processes, unlockedYardSlots, clamped, yards);
    }

    public NurseryPlayerState withUnlockedYardSlots(int nextUnlockedYardSlots) {
        int clamped = Math.max(1, Math.min(MAX_YARDS, nextUnlockedYardSlots));
        return new NurseryPlayerState(playerId, unlockedProcessSlots, processes, clamped, selectedYardIndex, yards);
    }

    public Optional<NurseryYardRecord> findYard(int yardIndex) {
        return yards.stream().filter(yard -> yard.yardIndex() == yardIndex).findFirst();
    }

    public NurseryPlayerState upsertYard(NurseryYardRecord record) {
        List<NurseryYardRecord> next = new ArrayList<>(yards);
        next.removeIf(existing -> existing.yardIndex() == record.yardIndex());
        next.add(record);
        next.sort(Comparator.comparingInt(NurseryYardRecord::yardIndex));
        while (next.size() < MAX_YARDS) {
            next.add(NurseryYardRecord.empty(next.size()));
        }
        return new NurseryPlayerState(playerId, unlockedProcessSlots, processes, unlockedYardSlots, selectedYardIndex, next);
    }

    public List<NurseryEggTrayEntry> selectedYardEggs() {
        return findYard(selectedYardIndex)
            .map(NurseryYardRecord::eggs)
            .orElse(List.of());
    }

    private static List<NurseryYardRecord> defaultYards() {
        List<NurseryYardRecord> yards = new ArrayList<>();
        for (int yardIndex = 0; yardIndex < MAX_YARDS; yardIndex++) {
            yards.add(NurseryYardRecord.empty(yardIndex));
        }
        return yards;
    }

    private static List<NurseryYardRecord> migrateLegacyEggTray(List<NurseryYardRecord> yards, List<NurseryEggTrayEntry> legacyEggTray) {
        List<NurseryYardRecord> migrated = new ArrayList<>(yards);
        for (NurseryEggTrayEntry egg : legacyEggTray) {
            if (egg.sourceYardIndex() < 0 || egg.sourceYardIndex() >= migrated.size()) {
                continue;
            }
            NurseryYardRecord yard = migrated.get(egg.sourceYardIndex());
            if (yard.eggCount() >= NurseryConfig.DEFAULT.yardEggTrayCapacity()) {
                continue;
            }
            migrated.set(egg.sourceYardIndex(), yard.addEgg(egg));
        }
        return migrated;
    }
}
