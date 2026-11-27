package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DaycareRefreshC2SPayload(BlockPos nurseryPos) implements CustomPacketPayload {
    public static final Type<DaycareRefreshC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "daycare_refresh_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DaycareRefreshC2SPayload> STREAM_CODEC =
        StreamCodec.of((buf, payload) -> buf.writeBlockPos(payload.nurseryPos()), buf -> new DaycareRefreshC2SPayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
