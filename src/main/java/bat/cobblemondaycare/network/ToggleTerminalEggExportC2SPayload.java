package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleTerminalEggExportC2SPayload(BlockPos nurseryPos, int yardIndex, boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleTerminalEggExportC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "toggle_yard_egg_outlet_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleTerminalEggExportC2SPayload> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> {
            buf.writeBlockPos(packet.nurseryPos());
            buf.writeVarInt(packet.yardIndex());
            buf.writeBoolean(packet.enabled());
        }, buf -> new ToggleTerminalEggExportC2SPayload(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}