package bat.cobblenursery.network;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class TerminalEggExportPayloadTest {
    @Test void requestAndStatusRoundTripWithoutPlayerIdentityOrV3Changes() {
        var pos=new BlockPos(-400,80,129);
        var request=new ToggleTerminalEggExportC2SPayload(pos,3,true);
        var state=new TerminalEggExportS2CPayload(pos,true,9);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            ToggleTerminalEggExportC2SPayload.STREAM_CODEC.encode(buf,request);
            TerminalEggExportS2CPayload.STREAM_CODEC.encode(buf,state);
            assertEquals(request,ToggleTerminalEggExportC2SPayload.STREAM_CODEC.decode(buf));
            assertEquals(state,TerminalEggExportS2CPayload.STREAM_CODEC.decode(buf));
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }
}