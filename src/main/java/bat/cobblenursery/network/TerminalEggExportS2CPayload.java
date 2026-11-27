package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalEggExportS2CPayload(BlockPos nurseryPos, boolean available, int enabledYards) implements CustomPacketPayload {
    public static final Type<TerminalEggExportS2CPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "yard_egg_outlets_state_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalEggExportS2CPayload> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> {
            buf.writeBlockPos(packet.nurseryPos());
            buf.writeBoolean(packet.available());
            buf.writeByte(packet.enabledYards());
        }, buf -> new TerminalEggExportS2CPayload(buf.readBlockPos(), buf.readBoolean(), buf.readUnsignedByte() & 15));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}