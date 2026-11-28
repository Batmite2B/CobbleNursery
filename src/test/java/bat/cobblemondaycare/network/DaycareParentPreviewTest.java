package bat.cobblemondaycare.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DaycareParentPreviewTest {
    @Test void v3RoundTripPreservesNondefaultOwnerFieldsAndPacketAlignment() {
        var preview = new DaycareParentPreview("Café", 73, "minecraft:carved_pumpkin",
            List.of("shiny", "alolan"), List.of("icebeam", "nastyplot"), List.of(31, 0, 8, 25, 12, 9),
            List.of(252, 0, 4, 0, 180, 74));
        var payload = new DaycareParentPayload(UUID.randomUUID(), "cobblemon:vulpix", "alolan", "FEMALE",
            "snowcloak", "cobblemon:timid", "cobblemon:premier_ball", "cobblemon:everstone", List.of("FIELD"), preview);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DaycareParentPayload.write(buf, payload);
            buf.writeInt(0x12345678);
            assertEquals(payload, DaycareParentPayload.read(buf));
            assertEquals(0x12345678, buf.readInt());
            assertFalse(buf.isReadable());
        } finally {
            buf.release();
        }
    }

    @Test void excessPresentationListCannotReachTheRenderer() {
        assertThrows(IllegalArgumentException.class, () -> new DaycareParentPreview("", 3, "",
            List.of(), List.of("1", "2", "3", "4", "5"), List.of(0, 0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0, 0)));
    }
}