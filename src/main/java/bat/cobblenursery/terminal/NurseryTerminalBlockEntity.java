package bat.cobblenursery.terminal;

import bat.cobblenursery.config.ServerNurserySettings;
import bat.cobblenursery.nursery.service.NurseryService;
import bat.cobblenursery.nursery.storage.NurserySavedData;
import bat.cobblenursery.registry.NurseryRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

public final class NurseryTerminalBlockEntity extends BlockEntity {
    private UUID placer;
    private UUID installation;

    public NurseryTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(NurseryRegistry.NURSERY_TERMINAL_ENTITY, pos, state);
    }

    /** Called only at placement, never when a player opens an existing/public terminal. */
    public void recordPlacer(UUID player) {
        if (level instanceof ServerLevel && placer == null) {
            placer = player;
            installation = UUID.randomUUID();
            setChanged();
        }
    }

    public boolean canConfigureEggExport(ServerPlayer player) {
        if (!ServerNurserySettings.current().automation().terminalExport()) return false;
        return level == player.serverLevel() && !isRemoved()
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64
                && HopperAutomationRules.mayConfigure(placer, player.getUUID());
    }

    private NurserySavedData.EggOutlet outlet() {
        if (!(level instanceof ServerLevel server) || installation == null) return null;
        return new NurserySavedData.EggOutlet(server.dimension().location().toString(), worldPosition.asLong(), installation);
    }

    public int eggExportMask(ServerPlayer player) {
        if (!canConfigureEggExport(player)) return 0;
        var saved = NurserySavedData.find(player.server);
        return saved == null ? 0 : saved.eggOutletMask(placer, outlet());
    }

    public boolean setEggExport(ServerPlayer player, int yard, boolean enabled) {
        if (!canConfigureEggExport(player)) return false;
        var saved = NurserySavedData.get(player.server);
        var data = saved.getOrCreate(player.getUUID());
        if (yard < 0 || yard >= data.unlockedYardSlots()
                || yard >= bat.cobblenursery.nursery.model.NurseryPlayerState.MAX_YARDS) return false;
        if (installation == null && enabled) {
            // Legacy enabled flags never activate a route: the owner must opt in again.
            installation = UUID.randomUUID();
            setChanged();
        }
        return saved.setEggOutlet(placer, yard, outlet(), enabled);
    }

    public void removeEggOutlets() {
        if (!(level instanceof ServerLevel server) || placer == null || installation == null) return;
        var saved = NurserySavedData.find(server.getServer());
        if (saved != null) saved.removeEggOutlet(placer, outlet());
    }

    public static void tickExport(Level level, BlockPos pos, BlockState state, NurseryTerminalBlockEntity terminal) {
        if (!ServerNurserySettings.current().automation().terminalExport()
                || !(level instanceof ServerLevel server) || terminal.placer == null || terminal.installation == null
                || ((level.getGameTime() + pos.asLong()) & 7L) != 0) return;
        var under = pos.below();
        var hopperState = level.getBlockState(under);
        if (!hopperState.is(Blocks.HOPPER) || !hopperState.getValue(HopperBlock.ENABLED)
                || !(level.getBlockEntity(under) instanceof HopperBlockEntity hopper)) return;

        NurseryService.getInstance().exportEggToHopper(server.getServer(), terminal.placer, terminal.outlet(), egg -> {
            try (var transaction = Transaction.openOuter()) {
                if (InventoryStorage.of(hopper, Direction.UP).insert(ItemVariant.of(egg), 1, transaction) != 1) return false;
                transaction.commit();
            }
            return true;
        });
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (placer != null) {
            tag.putUUID("egg_export_placer", placer);
            if (installation != null) tag.putUUID("egg_outlet_installation", installation);
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (level != null && level.isClientSide()) return;
        placer = tag.hasUUID("egg_export_placer") ? tag.getUUID("egg_export_placer") : null;
        installation = placer != null && tag.hasUUID("egg_outlet_installation") ? tag.getUUID("egg_outlet_installation") : null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
