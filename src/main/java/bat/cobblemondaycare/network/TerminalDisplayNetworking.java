package bat.cobblemondaycare.network;

import bat.cobblemondaycare.nursery.service.NurseryService;
import bat.cobblemondaycare.terminal.TerminalDisplayState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

public final class TerminalDisplayNetworking {
    private static final Map<ServerPlayer, TerminalDisplayState> LAST_SENT = new WeakHashMap<>();
    private TerminalDisplayNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TerminalDisplayS2CPayload.TYPE, TerminalDisplayS2CPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SENT.remove(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    sync(player);
                }
            }
        });
    }

    private static void sync(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, TerminalDisplayS2CPayload.TYPE)) {
            return;
        }
        TerminalDisplayState current = NurseryService.getInstance().getTerminalDisplayState(player);
        if (!current.equals(LAST_SENT.get(player))) {
            ServerPlayNetworking.send(player, new TerminalDisplayS2CPayload(current));
            LAST_SENT.put(player, current);
        }
    }
}
