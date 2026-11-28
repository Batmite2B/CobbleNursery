package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ForceReadyDaycareYardC2SPayload(BlockPos nurseryPos, int yardIndex) implements CustomPacketPayload {
    public static final Type<ForceReadyDaycareYardC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "force_ready_daycare_yard_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceReadyDaycareYardC2SPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.nurseryPos());
                buf.writeVarInt(payload.yardIndex());
            },
            buf -> new ForceReadyDaycareYardC2SPayload(buf.readBlockPos(), buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
