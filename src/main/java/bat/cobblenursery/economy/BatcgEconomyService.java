package bat.cobblenursery.economy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BatcgEconomyService implements EconomyService {
    private static final ResourceLocation POKEDOLLAR_ID =
        ResourceLocation.fromNamespaceAndPath("batcg", "pokedollar");

    @Override
    public boolean canAfford(ServerPlayer player, int amount) {
        return countPokedollars(player) >= amount;
    }

    @Override
    public boolean withdraw(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }

        Item currency = getPokedollarItem();
        if (currency == null || !canAfford(player, amount)) {
            return false;
        }

        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(currency)) {
                continue;
            }

            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            if (stack.isEmpty()) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
            remaining -= remove;
        }

        player.getInventory().setChanged();
        return remaining <= 0;
    }

    @Override
    public void deposit(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        Item currency = getPokedollarItem();
        if (currency == null) {
            return;
        }

        ItemStack stack = new ItemStack(currency, amount);
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
    }

    private int countPokedollars(ServerPlayer player) {
        Item currency = getPokedollarItem();
        if (currency == null) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(currency)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private Item getPokedollarItem() {
        return BuiltInRegistries.ITEM.getOptional(POKEDOLLAR_ID).orElse(null);
    }
}
