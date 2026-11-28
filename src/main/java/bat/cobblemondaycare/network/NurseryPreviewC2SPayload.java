package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NurseryPreviewC2SPayload(BlockPos nurseryPos, int slotA, int slotB) implements CustomPacketPayload {
    public static final Type<NurseryPreviewC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "nursery_preview_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NurseryPreviewC2SPayload> STREAM_CODEC =
        StreamCodec.of(NurseryPreviewC2SPayload::write, NurseryPreviewC2SPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, NurseryPreviewC2SPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.slotA());
        buf.writeVarInt(payload.slotB());
    }

    private static NurseryPreviewC2SPayload read(RegistryFriendlyByteBuf buf) {
        return new NurseryPreviewC2SPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }
}
