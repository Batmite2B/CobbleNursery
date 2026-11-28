package bat.cobblemondaycare.client.screen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class HopperControlLayoutTest {
    @Test void footerControlSitsLeftOfStartAndOutsideTheStatusBar() {
        var toggle=NurseryPcLayout.hopperExport();
        var start=NurseryPcLayout.action();
        assertTrue(start.x() - (toggle.x() + toggle.width()) >= 12);
        assertEquals(start.y() + 10, toggle.y());
        assertTrue(toggle.x() >= 76 && toggle.x() + toggle.width() <= 122);
        assertTrue(toggle.y() + toggle.height() <= NurseryPcLayout.HEIGHT - 8);
        assertTrue(toggle.y()>=NurseryPcLayout.status().y()+NurseryPcLayout.status().height());
        assertTrue(NurseryPcLayout.buttonHit(toggle,toggle.x()+1,toggle.y()+1));
        assertFalse(NurseryPcLayout.buttonHit(toggle,start.x(),start.y()+8));
        assertEquals(0,NurseryPcLayout.hopperAtlasRow(false));
        assertEquals(19,NurseryPcLayout.hopperAtlasRow(true));
    }
}