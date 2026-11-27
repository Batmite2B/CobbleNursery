package bat.cobblenursery.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public record DaycareEggTrayPayload(
    UUID entryId,
    int sourceYardIndex,
    String childSpeciesId,
    String childFormId,
    String inheritedBallId,
    long createdAtEpochMs
) {
    public static void write(RegistryFriendlyByteBuf buf, DaycareEggTrayPayload payload) {
        buf.writeUUID(payload.entryId());
        buf.writeVarInt(payload.sourceYardIndex());
        buf.writeUtf(payload.childSpeciesId());
        buf.writeUtf(payload.childFormId());
        buf.writeUtf(payload.inheritedBallId());
        buf.writeLong(payload.createdAtEpochMs());
    }

    public static DaycareEggTrayPayload read(RegistryFriendlyByteBuf buf) {
        return new DaycareEggTrayPayload(
            buf.readUUID(),
            buf.readVarInt(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readLong()
        );
    }
}
