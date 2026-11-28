package bat.cobblemondaycare.client;

import bat.cobblemondaycare.client.screen.DaycareScreen;
import bat.cobblemondaycare.network.OpenDaycareScreenS2CPayload;
import net.minecraft.client.Minecraft;

public final class NurseryClientController {
    private NurseryClientController() {
    }

    public static void handleOpen(OpenDaycareScreenS2CPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DaycareScreen daycareScreen) {
            daycareScreen.applyPayload(payload);
            return;
        }
        var screen = new DaycareScreen(payload);
        minecraft.setScreen(screen);
        if (minecraft.screen == screen) {
            NurseryUiSounds.open();
        }
    }
}
