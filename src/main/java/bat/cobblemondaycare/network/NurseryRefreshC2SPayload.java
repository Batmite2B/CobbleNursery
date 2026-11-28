package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryRefreshC2SPayload(BlockPos nurseryPos) implements CustomPacketPayload {
    public static final Type<NurseryRefreshC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "nursery_refresh_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryRefreshC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryRefreshC2SPayload::write, NurseryRefreshC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryRefreshC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
    }

    private static NurseryRefreshC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryRefreshC2SPayload(buf.readBlockPos());
    }
}
