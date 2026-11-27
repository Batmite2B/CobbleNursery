package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryCancelC2SPayload(BlockPos nurseryPos, int processSlot) implements CustomPacketPayload {
    public static final Type<NurseryCancelC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "nursery_cancel_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryCancelC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryCancelC2SPayload::write, NurseryCancelC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryCancelC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.processSlot());
    }

    private static NurseryCancelC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryCancelC2SPayload(buf.readBlockPos(), buf.readVarInt());
    }
}
