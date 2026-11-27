package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockDaycareYardC2SPayload(BlockPos nurseryPos, int yardIndex) implements CustomPacketPayload {
    public static final Type<UnlockDaycareYardC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "unlock_daycare_yard_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockDaycareYardC2SPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.nurseryPos());
                        buf.writeVarInt(payload.yardIndex());
                    },
                    buf -> new UnlockDaycareYardC2SPayload(buf.readBlockPos(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}