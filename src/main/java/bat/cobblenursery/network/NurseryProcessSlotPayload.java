package bat.cobblenursery.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record NurseryProcessSlotPayload(
    int slotIndex,
    String status,
    String parentSummary,
    long remainingMs
) {
    public static void write(RegistryFriendlyByteBuf buf, NurseryProcessSlotPayload payload) {
        buf.writeVarInt(payload.slotIndex());
        buf.writeUtf(payload.status());
        buf.writeUtf(payload.parentSummary());
        buf.writeLong(payload.remainingMs());
    }

    public static NurseryProcessSlotPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryProcessSlotPayload(
            buf.readVarInt(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readLong()
        );
    }
}
