package bat.cobblemondaycare.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DaycareSettingsPayloadTest {
    @Test void currencyAndShinyOddsRoundTripWithTheScreen() {
        var original = new OpenDaycareScreenS2CPayload(BlockPos.ZERO, false, List.of(), List.of(), List.of(),
                1, 27, "minecraft:emerald", 73, "feedback", false);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            OpenDaycareScreenS2CPayload.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, OpenDaycareScreenS2CPayload.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }
}
