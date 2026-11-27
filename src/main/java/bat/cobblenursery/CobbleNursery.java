package bat.cobblenursery;

import bat.cobblenursery.command.NurseryCommands;
import bat.cobblenursery.config.HabitatUnlockCosts;
import bat.cobblenursery.entity.HealingMachineNurseManager;
import bat.cobblenursery.network.NurseryNetworking;
import bat.cobblenursery.network.TerminalDisplayNetworking;
import bat.cobblenursery.nursery.service.NurseryService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import bat.cobblenursery.registry.NurseryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobbleNursery implements ModInitializer {
    public static final String MOD_ID = "cobblenursery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing CobbleNursery");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var file = FabricLoader.getInstance().getConfigDir().resolve(HabitatUnlockCosts.FILE_NAME);
            NurseryService.getInstance().setHabitatUnlockCosts(
                    HabitatUnlockCosts.load(file, warning -> LOGGER.warn("{}: {}", file, warning))
            );
        });
        NurseryRegistry.register();
        bat.cobblenursery.nest.NestHatchSounds.register();
        NurseryNetworking.register();
        TerminalDisplayNetworking.register();
        NurseryCommands.register();
        HealingMachineNurseManager.register();
    }
}
