package bat.cobblemondaycare.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record DaycareYardPayload(
    int yardIndex,
    boolean unlocked,
    int unlockCost,
    boolean canAffordUnlock,
    String visualState,
    DaycareParentPayload parentA,
    DaycareParentPayload parentB,
    boolean compatible,
    String compatibilityReason,
    DaycareEggPreviewPayload eggPreview,
    int inheritedIvCount,
    String inheritedNatureId,
    long remainingMs,
    float progress,
    int completedCycles,
    int eggCount,
    int eggCapacity
) {
    public static void write(RegistryFriendlyByteBuf buf, DaycareYardPayload payload) {
        buf.writeVarInt(payload.yardIndex());
        buf.writeBoolean(payload.unlocked());
        buf.writeVarInt(payload.unlockCost());
        buf.writeBoolean(payload.canAffordUnlock());
        buf.writeUtf(payload.visualState());
        buf.writeBoolean(payload.parentA() != null);
        if (payload.parentA() != null) {
            DaycareParentPayload.write(buf, payload.parentA());
        }
        buf.writeBoolean(payload.parentB() != null);
        if (payload.parentB() != null) {
            DaycareParentPayload.write(buf, payload.parentB());
        }
        buf.writeBoolean(payload.compatible());
        buf.writeUtf(payload.compatibilityReason());
        buf.writeBoolean(payload.eggPreview() != null);
        if (payload.eggPreview() != null) {
            DaycareEggPreviewPayload.write(buf, payload.eggPreview());
        }
        buf.writeVarInt(payload.inheritedIvCount());
        buf.writeUtf(payload.inheritedNatureId());
        buf.writeLong(payload.remainingMs());
        buf.writeFloat(payload.progress());
        buf.writeVarInt(payload.completedCycles());
        buf.writeVarInt(payload.eggCount());
        buf.writeVarInt(payload.eggCapacity());
    }

    public static DaycareYardPayload read(RegistryFriendlyByteBuf buf) {
        int yardIndex = buf.readVarInt();
        boolean unlocked = buf.readBoolean();
        int unlockCost = buf.readVarInt();
        boolean canAffordUnlock = buf.readBoolean();
        String visualState = buf.readUtf();
        DaycareParentPayload parentA = buf.readBoolean() ? DaycareParentPayload.read(buf) : null;
        DaycareParentPayload parentB = buf.readBoolean() ? DaycareParentPayload.read(buf) : null;
        boolean compatible = buf.readBoolean();
        String compatibilityReason = buf.readUtf();
        DaycareEggPreviewPayload eggPreview = buf.readBoolean() ? DaycareEggPreviewPayload.read(buf) : null;
        int inheritedIvCount = buf.readVarInt();
        String inheritedNatureId = buf.readUtf();
        long remainingMs = buf.readLong();
        float progress = buf.readFloat();
        int completedCycles = buf.readVarInt();
        int eggCount = buf.readVarInt();
        int eggCapacity = buf.readVarInt();
        return new DaycareYardPayload(
            yardIndex,
            unlocked,
            unlockCost,
            canAffordUnlock,
            visualState,
            parentA,
            parentB,
            compatible,
            compatibilityReason,
            eggPreview,
            inheritedIvCount,
            inheritedNatureId,
            remainingMs,
            progress,
            completedCycles,
            eggCount,
            eggCapacity
        );
    }
}
