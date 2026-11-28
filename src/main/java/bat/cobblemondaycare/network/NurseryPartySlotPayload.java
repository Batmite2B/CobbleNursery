package bat.cobblemondaycare.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record NurseryPartySlotPayload(
    int slotIndex,
    boolean occupied,
    String speciesId,
    String formId,
    String heldItemId,
    int level
) {
    public static void write(RegistryFriendlyByteBuf buf, NurseryPartySlotPayload payload) {
        buf.writeVarInt(payload.slotIndex());
        buf.writeBoolean(payload.occupied());
        buf.writeUtf(payload.speciesId());
        buf.writeUtf(payload.formId());
        buf.writeUtf(payload.heldItemId());
        buf.writeVarInt(payload.level());
    }

    public static NurseryPartySlotPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryPartySlotPayload(
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readVarInt()
        );
    }
}
