package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryClaimC2SPayload(BlockPos nurseryPos, int processSlot) implements CustomPacketPayload {
    public static final Type<NurseryClaimC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "nursery_claim_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryClaimC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryClaimC2SPayload::write, NurseryClaimC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryClaimC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.processSlot());
    }

    private static NurseryClaimC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryClaimC2SPayload(buf.readBlockPos(), buf.readVarInt());
    }
}
