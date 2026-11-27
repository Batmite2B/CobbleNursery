package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryStartC2SPayload(BlockPos nurseryPos, int slotA, int slotB) implements CustomPacketPayload {
    public static final Type<NurseryStartC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "nursery_start_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryStartC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryStartC2SPayload::write, NurseryStartC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryStartC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.slotA());
        buf.writeVarInt(payload.slotB());
    }

    private static NurseryStartC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryStartC2SPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }
}
