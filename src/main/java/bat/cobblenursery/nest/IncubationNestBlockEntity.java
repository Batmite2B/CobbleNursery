package bat.cobblenursery.nest;

import bat.cobblenursery.item.NurseryEggAppearanceResolver;
import bat.cobblenursery.item.NurseryEggItem;
import bat.cobblenursery.registry.NurseryRegistry;
import bat.cobblenursery.terminal.HopperAutomationRules;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.server.level.ServerLevel;
import java.util.Collections;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;
import java.util.UUID;

public final class IncubationNestBlockEntity extends BlockEntity {
    public record Seat(UUID hatchId, long readyAt, ItemStack stack, long insertedAt) {
        public Seat(UUID hatchId, long readyAt, ItemStack stack) {
            this(hatchId, readyAt, stack, System.currentTimeMillis());
        }
        Seat advance(long credit) { return new Seat(hatchId, readyAt - credit, stack, insertedAt); }
        public CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag row = new CompoundTag();
            row.putUUID("hatch",hatchId);
            row.putLong("ready",readyAt);
            row.put("egg",stack.saveOptional(registries));
            return row;
        }
        public static Seat load(CompoundTag row,HolderLookup.Provider registries) {
            if (!row.hasUUID("hatch")) return null;
            var item=ItemStack.parseOptional(registries,row.getCompound("egg"));
            return item.isEmpty() ? null : new Seat(row.getUUID("hatch"),row.getLong("ready"),item);
        }
    }
    private final Seat[] seats = new Seat[NestIncubation.SEATS];
    private final int[] visible = new int[NestIncubation.SEATS];
    private final int[] sizes = {32,32,32,32};
    private final int[] motionSeeds = new int[NestIncubation.SEATS];
    private final long[] visibleRemaining = new long[NestIncubation.SEATS];
    private long receivedAt;
    private long warmthSampleMillis;
    private long warmthSeenTick;
    private int warmthMask;
    private UUID caregiver;
    private UUID owner;
    // Old malformed ownerless seats stay closed; only deliberately anonymous intake opens them.
    private boolean publicIntake;
    private final Intake intake = new Intake();
    record BreakRecovery(UUID owner, java.util.List<ItemStack> eggs) {}
    BreakRecovery breakRecovery;

    public IncubationNestBlockEntity(BlockPos pos, BlockState state) {
        super(NurseryRegistry.INCUBATION_NEST_ENTITY,pos,state);
    }

    public boolean mayAccess(UUID user) {
        return (owner != null || !occupied() || publicIntake) && NestIncubation.mayAccess(owner,user);
    }

    static void registerIntake() {
        ItemStorage.SIDED.registerForBlockEntity((nest, direction) -> nest.automatedIntake(),
                NurseryRegistry.INCUBATION_NEST_ENTITY);
    }
    UUID incubationOwner() { return owner; }
    public boolean occupied() { return seatMask() != 0; }
    public Seat seat(int index) { return seats[index]; }
    public int model(int index) { return visible[index]; }
    public int sizeTier(int index) { return sizes[index]; }
    private int outlineKey=-1;
    private net.minecraft.world.phys.shapes.VoxelShape shapedOutline;
    public net.minecraft.world.phys.shapes.VoxelShape outline(int mask,boolean framed,int turn) {
        int key=mask | (framed ? 16 : 0) | (turn<<5);
        for (int i=0;i<sizes.length;i++) key = key << 6 | sizes[i];
        if (key!=outlineKey || shapedOutline==null) {
            outlineKey=key;
            shapedOutline=NestOutline.rotated(IncubationNestBlock.sizedOutline(mask,this::sizeTier),turn);
            if (framed) shapedOutline=IncubationNestBlock.withFrame(shapedOutline);
        }
        return shapedOutline;
    }
    public long serverRemaining(int index,long now) {
        return seats[index]==null ? 0 : Math.clamp(seats[index].readyAt()-now,-NestLifeVisual.MAX_READY_AGE_MS,NestLifeVisual.MAX_REMAINING_MS);
    }
    public int motionSeed(int index) { return motionSeeds[index]; }
    public long clientRemaining(int index,long monotonicNow) {
        return visibleRemaining[index]-Math.max(0,monotonicNow-receivedAt);
    }

    private void deriveVisual(int index,Seat seat) {
        var egg=NurseryEggItem.readEggData(seat.stack()).orElse(null);
        sizes[index]=egg==null ? NestLifeVisual.MAX_TIER : NestLifeVisual.sizeTier(egg.ivs());
        motionSeeds[index]=NestLifeVisual.seed(seat.hatchId());
        visible[index]=appearance(seat.stack());
    }

    public boolean insert(int index, UUID user, Seat seat) {
        if (!mayAccess(user) || !canAdd(index, seat)) return false;
        UUID eggOwner = NurseryEggItem.automationOwner(seat.stack());
        if (eggOwner != null && (!eggOwner.equals(user) || publicIntake)) return false;
        if (!publicIntake) owner = user;
        seats[index] = seat;
        deriveVisual(index,seat);
        return true;
    }

    private boolean canAdd(int index, Seat seat) {
        if (index < 0 || index >= seats.length || seats[index] != null
                || seat.stack().isEmpty() || seat.stack().getCount() != 1) return false;
        for (Seat current : seats) if (current != null && current.hatchId().equals(seat.hatchId())) return false;
        return true;
    }

    Storage<ItemVariant> automatedIntake() { return intake; }

    private record IntakeSnapshot(Seat[] seats, UUID owner, boolean publicIntake) {}

    private final class Intake extends SnapshotParticipant<IntakeSnapshot> implements Storage<ItemVariant> {
        @Override public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (!(level instanceof ServerLevel) || isRemoved() || maxAmount <= 0
                    || !resource.isOf(NurseryRegistry.NURSERY_EGG_ITEM)) return 0;
            int free = NestLayout.firstFree(seatMask());
            if (free < 0) return 0;
            ItemStack stack = resource.toStack();
            UUID eggOwner = NurseryEggItem.automationOwner(stack);
            if ((owner == null && occupied() && !publicIntake)
                    || !HopperAutomationRules.mayInsert(occupied(), owner, eggOwner)) return 0;
            var egg = NurseryEggItem.readIncubatableData(stack).orElse(null);
            if (egg == null) return 0;
            var seat = IncubationNestBlock.prepareSeat(stack, egg, System.currentTimeMillis());
            if (!canAdd(free, seat)) return 0;

            updateSnapshots(transaction);
            seats[free] = seat;
            owner = eggOwner;
            publicIntake = owner == null;
            return 1;
        }

        @Override protected IntakeSnapshot createSnapshot() {
            return new IntakeSnapshot(seats.clone(), owner, publicIntake);
        }

        @Override protected void readSnapshot(IntakeSnapshot snapshot) {
            System.arraycopy(snapshot.seats(), 0, seats, 0, seats.length);
            owner = snapshot.owner();
            publicIntake = snapshot.publicIntake();
        }

        @Override protected void onFinalCommit() {
            for (int i=0; i<seats.length; i++) if (seats[i]!=null) deriveVisual(i, seats[i]);
            changedAndSync();
        }

        @Override public boolean supportsExtraction() { return false; }
        @Override public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) { return 0; }
        @Override public Iterator<StorageView<ItemVariant>> iterator() { return Collections.emptyIterator(); }
    }

    public boolean clear(int index, Seat expected) {
        if (seats[index] != expected) return false;
        seats[index] = null;
        visible[index] = 0;
        sizes[index] = NestLifeVisual.MAX_TIER;
        if (!occupied()) {
            owner = null;
            publicIntake = false;
            resetWarmth();
        }
        changedAndSync();
        return true;
    }

    public int seatMask() {
        int mask=0;
        for(int i=0;i<seats.length;i++) if(seats[i]!=null) mask |= 1 << i;
        return mask;
    }

    int warmthPercent() { return NestWarmthRules.bonus(warmthMask); }
    int activeWarmthMask() { return warmthMask; }


    private static final int VALIDATE_TICKS = 1200;

    private boolean pending(long now) {
        for (var seat : seats) if (seat != null && seat.readyAt() > now) return true;
        return false;
    }

    private boolean ownerAvailable(ServerLevel level) {
        return owner == null || level.getServer().getPlayerList().getPlayer(owner) != null;
    }

    void refreshWarmth(ServerLevel server, long now) {
        long tick = server.getGameTime();
        if (warmthMask != 0 && tick - warmthSeenTick <= 1 && ownerAvailable(server)) applyWarmth(now);
        int old = warmthMask;
        if (pending(now) && ownerAvailable(server)) {
            caregiver = NestWarmth.findCaregiver(server, worldPosition);
            warmthMask = NestWarmthRules.mask(NestRoomCache.enclosed(server, worldPosition),
                NestWarmth.light(server, worldPosition), caregiver != null);
        } else {
            resetWarmth();
        }
        warmthSampleMillis = now;
        warmthSeenTick = tick;
        if (old != warmthMask) syncWarmth();
    }

    private void resetWarmth() {
        warmthMask = 0;
        warmthSampleMillis = 0;
        warmthSeenTick = 0;
        caregiver = null;
    }

    @Override public void setRemoved() {
        resetWarmth();
        super.setRemoved();
    }

    private void applyWarmth(long now) {
        long prior = warmthSampleMillis;
        warmthSampleMillis = now;
        if (prior <= 0 || now <= prior || now - prior > 2000) return;
        int bonus = NestWarmthRules.bonus(warmthMask);
        boolean changed = false;
        for (int i = 0; i < seats.length; i++) {
            var seat = seats[i];
            if (seat == null || seat.readyAt() <= Math.max(prior, seat.insertedAt())) continue;
            long credit = Math.max(0L, now - Math.max(prior, seat.insertedAt())) * bonus / 100;
            if (credit == 0) continue;
            seats[i] = seat.advance(credit);
            changed = true;
        }
        if (changed) setChanged();
    }

    private void syncWarmth() {
        if (level == null) return;
        var state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    static void tickWarmth(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, IncubationNestBlockEntity nest) {
        if (nest.warmthMask == 0 || !(level instanceof ServerLevel server)) return;
        long tick = level.getGameTime();
        if (tick - nest.warmthSeenTick > 1) {
            nest.resetWarmth();
            nest.syncWarmth();
            return;
        }
        nest.warmthSeenTick = tick;
        int slot = Math.floorMod(tick + Long.hashCode(pos.asLong() * 0x9E3779B97F4A7C15L), VALIDATE_TICKS);
        if (slot % NestWarmthRules.SAMPLE_TICKS != 0) return;
        long now = System.currentTimeMillis();
        int old = nest.warmthMask;
        if (!nest.ownerAvailable(server)) {
            nest.resetWarmth();
            nest.syncWarmth();
            return;
        }
        nest.applyWarmth(now);
        if (!nest.pending(now)) {
            nest.resetWarmth();
        } else if (slot == 0) {
            if ((old & NestWarmthRules.ROOM) != 0 && !NestRoomCache.enclosed(server,pos))
                nest.warmthMask &= ~NestWarmthRules.ROOM;
            if ((old & NestWarmthRules.LIGHT) != 0 && NestWarmth.light(server,pos) < 10)
                nest.warmthMask &= ~NestWarmthRules.LIGHT;
            if ((old & NestWarmthRules.CAREGIVER) != 0 && !NestWarmth.caregiverPresent(server,pos,nest.caregiver)) {
                nest.warmthMask &= ~NestWarmthRules.CAREGIVER;
                nest.caregiver = null;
            }
        }
        if (old != nest.warmthMask || slot % 100 == 0) nest.syncWarmth();
    }

    void changedAndSync() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() != NurseryRegistry.INCUBATION_NEST) return;
        BlockState updated = state.setValue(IncubationNestBlock.SEATS,seatMask());
        if (updated != state) level.setBlock(worldPosition,updated,Block.UPDATE_CLIENTS);
        level.sendBlockUpdated(worldPosition,state,updated,Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if (owner != null) tag.putUUID("owner",owner);
        if (publicIntake) tag.putBoolean("public_intake",true);
        for (int i=0;i<seats.length;i++) {
            Seat seat = seats[i];
            if (seat == null) continue;
            tag.put("seat_"+i,seat.save(registries));
        }
    }

    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        if (tag.contains("display",Tag.TAG_INT_ARRAY)) {
            // Relative time and cosmetic values only. Never send the egg NBT, owner or hatch UUID.
            int[] items=tag.getIntArray("display");
            Arrays.fill(visible,0);
            for (int i=0;i<visible.length && i<items.length;i++) visible[i]=Math.clamp(items[i],0,18);
            var scale=tag.getIntArray("size");
            var seeds=tag.getIntArray("shake");
            var time=tag.getLongArray("time");
            for (int i=0;i<visible.length;i++) {
                sizes[i]=i<scale.length ? Math.clamp(scale[i],0,NestLifeVisual.MAX_TIER) : NestLifeVisual.MAX_TIER;
                motionSeeds[i]=i<seeds.length ? seeds[i] : 0;
                visibleRemaining[i]=i<time.length ? Math.clamp(time[i],-NestLifeVisual.MAX_READY_AGE_MS,NestLifeVisual.MAX_REMAINING_MS) : 0;
            }
            receivedAt=net.minecraft.Util.getMillis();
            return;
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        publicIntake = owner == null && tag.getBoolean("public_intake");
        Arrays.fill(seats,null);
        Arrays.fill(visible,0);
        for (int i=0;i<seats.length;i++) {
            if (!tag.contains("seat_"+i,Tag.TAG_COMPOUND)) continue;
            Seat seat=Seat.load(tag.getCompound("seat_"+i),registries);
            if (seat==null) continue;
            seats[i]=seat;
            deriveVisual(i,seat);
        }
        if (!occupied()) publicIntake = false;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag=new CompoundTag();
        tag.putIntArray("display",visible.clone());
        tag.putIntArray("size",sizes.clone());
        tag.putIntArray("shake",motionSeeds.clone());
        long now=System.currentTimeMillis();
        long[] time=new long[seats.length];
        for (int i=0;i<seats.length;i++) if (seats[i]!=null)
            time[i]=Math.clamp(seats[i].readyAt()-now,-NestLifeVisual.MAX_READY_AGE_MS,NestLifeVisual.MAX_REMAINING_MS);
        tag.putLongArray("time",time);
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static int appearance(ItemStack stack) {
        var model=stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (model!=null && model.value()>=1 && model.value()<=18) return model.value();
        var egg=NurseryEggItem.readEggData(stack).orElse(null);
        return egg == null ? 13 : NurseryEggAppearanceResolver.resolve(egg.childSpeciesId(),egg.childFormId()).modelData();
    }
}