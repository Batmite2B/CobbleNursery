package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryForceReadyC2SPayload(BlockPos nurseryPos, int processSlot) implements CustomPacketPayload {
    public static final Type<NurseryForceReadyC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "nursery_force_ready_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryForceReadyC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryForceReadyC2SPayload::write, NurseryForceReadyC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryForceReadyC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.processSlot());
    }

    private static NurseryForceReadyC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryForceReadyC2SPayload(buf.readBlockPos(), buf.readVarInt());
    }
}
