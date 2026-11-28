package bat.cobblemondaycare.economy;

import net.minecraft.server.level.ServerPlayer;

public interface EconomyService {
    boolean canAfford(ServerPlayer player, int amount);

    boolean withdraw(ServerPlayer player, int amount);

    void deposit(ServerPlayer player, int amount);
}
