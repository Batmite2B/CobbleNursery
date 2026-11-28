package bat.cobblemondaycare.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record NurseryPreviewPayload(
    int slotA,
    int slotB,
    boolean compatible,
    String reason,
    boolean hasEggData,
    String childSpeciesId,
    String childFormId,
    String natureId,
    String abilityId,
    String ballId,
    List<String> inheritedMoveIds,
    int hpIv,
    int attackIv,
    int defenceIv,
    int specialAttackIv,
    int specialDefenceIv,
    int speedIv
) {
    public NurseryPreviewPayload {
        inheritedMoveIds = List.copyOf(inheritedMoveIds);
    }

    public static void write(RegistryFriendlyByteBuf buf, NurseryPreviewPayload payload) {
        buf.writeVarInt(payload.slotA());
        buf.writeVarInt(payload.slotB());
        buf.writeBoolean(payload.compatible());
        buf.writeUtf(payload.reason());
        buf.writeBoolean(payload.hasEggData());
        buf.writeUtf(payload.childSpeciesId());
        buf.writeUtf(payload.childFormId());
        buf.writeUtf(payload.natureId());
        buf.writeUtf(payload.abilityId());
        buf.writeUtf(payload.ballId());
        buf.writeVarInt(payload.inheritedMoveIds().size());
        for (String moveId : payload.inheritedMoveIds()) {
            buf.writeUtf(moveId);
        }
        buf.writeVarInt(payload.hpIv());
        buf.writeVarInt(payload.attackIv());
        buf.writeVarInt(payload.defenceIv());
        buf.writeVarInt(payload.specialAttackIv());
        buf.writeVarInt(payload.specialDefenceIv());
        buf.writeVarInt(payload.speedIv());
    }

    public static NurseryPreviewPayload read(RegistryFriendlyByteBuf buf) {
        int slotA = buf.readVarInt();
        int slotB = buf.readVarInt();
        boolean compatible = buf.readBoolean();
        String reason = buf.readUtf();
        boolean hasEggData = buf.readBoolean();
        String childSpeciesId = buf.readUtf();
        String childFormId = buf.readUtf();
        String natureId = buf.readUtf();
        String abilityId = buf.readUtf();
        String ballId = buf.readUtf();
        int moveCount = buf.readVarInt();
        List<String> moveIds = new ArrayList<>(moveCount);
        for (int i = 0; i < moveCount; i++) {
            moveIds.add(buf.readUtf());
        }
        return new NurseryPreviewPayload(
            slotA,
            slotB,
            compatible,
            reason,
            hasEggData,
            childSpeciesId,
            childFormId,
            natureId,
            abilityId,
            ballId,
            moveIds,
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }
}
