package bat.cobblenursery.nest;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class NestHatchSounds {
    private static final SoundEvent[] CRACKS = new SoundEvent[3];
    private NestHatchSounds() {}

    public static void register() {
        for (int i = 0; i < CRACKS.length; i++) {
            var id = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "nest.hatch_" + (i + 1));
            CRACKS[i] = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        }
    }

    static void play(ServerLevel level, BlockPos pos) {
        int variant = level.getRandom().nextInt(CRACKS.length);
        level.playSound(null, pos, CRACKS[variant], SoundSource.BLOCKS, 1F, 1F);
    }
}

