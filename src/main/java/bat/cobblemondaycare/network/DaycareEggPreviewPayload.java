package bat.cobblemondaycare.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record DaycareEggPreviewPayload(
    String childSpeciesId,
    String childFormId,
    String natureId,
    String abilityId,
    String ballId,
    List<String> moveIds,
    int hpRollMin,
    int attackRollMin,
    int defenceRollMin,
    int specialAttackRollMin,
    int specialDefenceRollMin,
    int speedRollMin,
    int hpIv,
    int attackIv,
    int defenceIv,
    int specialAttackIv,
    int specialDefenceIv,
    int speedIv
) {
    public DaycareEggPreviewPayload {
        moveIds = List.copyOf(moveIds);
    }

    public static void write(RegistryFriendlyByteBuf buf, DaycareEggPreviewPayload payload) {
        buf.writeUtf(payload.childSpeciesId());
        buf.writeUtf(payload.childFormId());
        buf.writeUtf(payload.natureId());
        buf.writeUtf(payload.abilityId());
        buf.writeUtf(payload.ballId());
        buf.writeVarInt(payload.moveIds().size());
        for (String moveId : payload.moveIds()) {
            buf.writeUtf(moveId);
        }
        buf.writeVarInt(payload.hpRollMin());
        buf.writeVarInt(payload.attackRollMin());
        buf.writeVarInt(payload.defenceRollMin());
        buf.writeVarInt(payload.specialAttackRollMin());
        buf.writeVarInt(payload.specialDefenceRollMin());
        buf.writeVarInt(payload.speedRollMin());
        buf.writeVarInt(payload.hpIv());
        buf.writeVarInt(payload.attackIv());
        buf.writeVarInt(payload.defenceIv());
        buf.writeVarInt(payload.specialAttackIv());
        buf.writeVarInt(payload.specialDefenceIv());
        buf.writeVarInt(payload.speedIv());
    }

    public static DaycareEggPreviewPayload read(RegistryFriendlyByteBuf buf) {
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
        return new DaycareEggPreviewPayload(
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
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }
}
