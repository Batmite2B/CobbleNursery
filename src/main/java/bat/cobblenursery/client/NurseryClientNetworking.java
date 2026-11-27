package bat.cobblenursery.client;

import bat.cobblenursery.network.OpenDaycareScreenS2CPayload;
import bat.cobblenursery.network.TerminalDisplayS2CPayload;
import bat.cobblenursery.terminal.TerminalDisplayState;
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
        ClientPlayNetworking.registerGlobalReceiver(bat.cobblenursery.network.TerminalEggExportS2CPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().screen instanceof bat.cobblenursery.client.screen.DaycareScreen daycare) {
                        daycare.applyEggExportState(payload);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(OpenDaycareScreenS2CPayload.TYPE, (payload, context) ->
            context.client().execute(() -> NurseryClientController.handleOpen(payload))
        );
    }
}
