package bat.cobblenursery.nursery.model;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record LineageRecord(
    UUID ownerUuid,
    int hpIv,
    int attackIv,
    int defenceIv,
    int specialAttackIv,
    int specialDefenceIv,
    int speedIv
) {
    public static LineageRecord createDefault(UUID ownerUuid) {
        return new LineageRecord(ownerUuid, 0, 0, 0, 0, 0, 0);
    }

    public int getIv(String stat) {
        return switch (stat) {
            case "hp" -> hpIv;
            case "attack" -> attackIv;
            case "defence" -> defenceIv;
            case "special_attack" -> specialAttackIv;
            case "special_defence" -> specialDefenceIv;
            case "speed" -> speedIv;
            default -> 0;
        };
    }

    public LineageRecord withRaisedIv(String stat, int candidateIv) {
        int raised = Math.max(getIv(stat), candidateIv);
        return switch (stat) {
            case "hp" -> new LineageRecord(ownerUuid, raised, attackIv, defenceIv, specialAttackIv, specialDefenceIv, speedIv);
            case "attack" -> new LineageRecord(ownerUuid, hpIv, raised, defenceIv, specialAttackIv, specialDefenceIv, speedIv);
            case "defence" -> new LineageRecord(ownerUuid, hpIv, attackIv, raised, specialAttackIv, specialDefenceIv, speedIv);
            case "special_attack" -> new LineageRecord(ownerUuid, hpIv, attackIv, defenceIv, raised, specialDefenceIv, speedIv);
            case "special_defence" -> new LineageRecord(ownerUuid, hpIv, attackIv, defenceIv, specialAttackIv, raised, speedIv);
            case "speed" -> new LineageRecord(ownerUuid, hpIv, attackIv, defenceIv, specialAttackIv, specialDefenceIv, raised);
            default -> this;
        };
    }

    public Map<String, Integer> applyAsFloor(Map<String, Integer> candidateIvs) {
        Map<String, Integer> adjusted = new LinkedHashMap<>(candidateIvs);
        adjusted.put("hp", Math.max(candidateIvs.getOrDefault("hp", 0), hpIv));
        adjusted.put("attack", Math.max(candidateIvs.getOrDefault("attack", 0), attackIv));
        adjusted.put("defence", Math.max(candidateIvs.getOrDefault("defence", 0), defenceIv));
        adjusted.put("special_attack", Math.max(candidateIvs.getOrDefault("special_attack", 0), specialAttackIv));
        adjusted.put("special_defence", Math.max(candidateIvs.getOrDefault("special_defence", 0), specialDefenceIv));
        adjusted.put("speed", Math.max(candidateIvs.getOrDefault("speed", 0), speedIv));
        return adjusted;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("owner_uuid", ownerUuid);
        tag.putInt("hp_iv", hpIv);
        tag.putInt("attack_iv", attackIv);
        tag.putInt("defence_iv", defenceIv);
        tag.putInt("special_attack_iv", specialAttackIv);
        tag.putInt("special_defence_iv", specialDefenceIv);
        tag.putInt("speed_iv", speedIv);
        return tag;
    }

    public static LineageRecord load(CompoundTag tag) {
        return new LineageRecord(
            tag.getUUID("owner_uuid"),
            tag.getInt("hp_iv"),
            tag.getInt("attack_iv"),
            tag.getInt("defence_iv"),
            tag.getInt("special_attack_iv"),
            tag.getInt("special_defence_iv"),
            tag.getInt("speed_iv")
        );
    }
}
