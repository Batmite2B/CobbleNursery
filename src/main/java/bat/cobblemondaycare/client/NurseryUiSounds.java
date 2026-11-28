package bat.cobblemondaycare.client;

import bat.cobblemondaycare.CobblemonDaycare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** UI-only, never broadcast to another player or associated with a world position. */
public final class NurseryUiSounds {
    private static final SoundEvent OPEN = cue("ui.nursery_open");
    private static final SoundEvent CLICK = cue("ui.nursery_click");

    private NurseryUiSounds() {}

    public static void open() { play(OPEN); }
    public static void click() { play(CLICK); }

    private static SoundEvent cue(String id) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, id));
    }

    private static void play(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }
}
