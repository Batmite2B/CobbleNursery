package bat.cobblemondaycare;

import bat.cobblemondaycare.config.ServerNurserySettings;
import bat.cobblemondaycare.command.NurseryCommands;
import bat.cobblemondaycare.config.HabitatUnlockCosts;
import bat.cobblemondaycare.entity.HealingMachineNurseManager;
import bat.cobblemondaycare.network.NurseryNetworking;
import bat.cobblemondaycare.network.TerminalDisplayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import bat.cobblemondaycare.registry.NurseryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobblemonDaycare implements ModInitializer {
    public static final String MOD_ID = "cobblemondaycare";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Cobblemon Daycare");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var file = FabricLoader.getInstance().getConfigDir().resolve(HabitatUnlockCosts.FILE_NAME);
            ServerNurserySettings.install(ServerNurserySettings.load(file,
                    id -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).isPresent(),
                    warning -> LOGGER.warn("{}: {}", file, warning)));
        });
        NurseryRegistry.register();
        bat.cobblemondaycare.nest.NestHatchSounds.register();
        NurseryNetworking.register();
        TerminalDisplayNetworking.register();
        NurseryCommands.register();
        HealingMachineNurseManager.register();
    }
}
