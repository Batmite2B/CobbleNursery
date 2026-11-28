package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import bat.cobblemondaycare.terminal.TerminalDisplayState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalDisplayS2CPayload(TerminalDisplayState state) implements CustomPacketPayload {
    public static final Type<TerminalDisplayS2CPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "terminal_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalDisplayS2CPayload> STREAM_CODEC =
        StreamCodec.of((buf, p) -> buf.writeByte(p.state().packed()),
                       buf -> new TerminalDisplayS2CPayload(TerminalDisplayState.unpack(buf.readUnsignedByte())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
