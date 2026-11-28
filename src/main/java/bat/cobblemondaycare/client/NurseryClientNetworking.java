package bat.cobblemondaycare.client;

import bat.cobblemondaycare.network.OpenDaycareScreenS2CPayload;
import bat.cobblemondaycare.network.TerminalDisplayS2CPayload;
import bat.cobblemondaycare.terminal.TerminalDisplayState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class NurseryClientNetworking {
    private static TerminalDisplayState terminalState;

    public static TerminalDisplayState terminalState() {
        return terminalState;
    }

    private NurseryClientNetworking() {
    }

    public static void register() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> terminalState = null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> terminalState = null);
        ClientPlayNetworking.registerGlobalReceiver(TerminalDisplayS2CPayload.TYPE, (payload, context) -> {
            // Fabric invokes play payload receivers on the client thread.
            terminalState = payload.state();
        });
        ClientPlayNetworking.registerGlobalReceiver(bat.cobblemondaycare.network.TerminalEggExportS2CPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().screen instanceof bat.cobblemondaycare.client.screen.DaycareScreen daycare) {
                        daycare.applyEggExportState(payload);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenDaycareScreenS2CPayload.TYPE, (payload, context) ->
            context.client().execute(() -> NurseryClientController.handleOpen(payload))
        );
    }
}
