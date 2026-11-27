package bat.cobblenursery.nest;

import bat.cobblenursery.item.NurseryEggItem;
import bat.cobblenursery.item.NurseryEggHatchingService;
import bat.cobblenursery.registry.NurseryRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class IncubationNestBlock extends Block implements EntityBlock {
    public static final IntegerProperty SEATS = IntegerProperty.create("seats",0,15);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FRAMED = BooleanProperty.create("framed");
    private static final VoxelShape BASE = box(1,0,1,15,3.8,15);
    private static final VoxelShape FRAME = Shapes.or(
            box(0,14,0,16,16,16), box(0,0,0,2,14,2), box(0,0,14,2,14,16),
            box(14,0,0,16,14,2), box(14,0,14,16,14,16));
    private static final VoxelShape FRAMED_COLLISION = Shapes.or(BASE, FRAME);
    // Collision remains low and solid; ray selection follows the straw and rim,
    // leaving the low visible portions of the eggs individually targetable.
    private static final VoxelShape SELECTION_BASE = NestOutline.BASE;
    private static final VoxelShape[] OUTLINES = new VoxelShape[64];
    private static final VoxelShape[] FRAMED_OUTLINES = new VoxelShape[64];
    private static final String STAMP = "CobbleNurseryIncubation";
    static {
        for (int bits=0;bits<16;bits++) {
            var north=NestOutline.occupied(bits,slot->NestLifeVisual.MAX_TIER);
            for (int turn=0;turn<4;turn++) {
                int index=bits | (turn<<4);
                OUTLINES[index]=NestOutline.rotated(north,turn);
                FRAMED_OUTLINES[index]=withFrame(OUTLINES[index]);
            }
        }
    }

    public IncubationNestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEATS,0).setValue(FRAMED,false).setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(SEATS,FRAMED,FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) {
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state,Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new IncubationNestBlockEntity(pos,state); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        int mask=state.getValue(SEATS);
        boolean framed=state.getValue(FRAMED);
        int turn=NestOutline.turn(state.getValue(FACING));
        return level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest
                ? nest.outline(mask,framed,turn) : (framed ? FRAMED_OUTLINES : OUTLINES)[mask | (turn<<4)];
    }
    static VoxelShape sizedOutline(int mask,java.util.function.IntUnaryOperator tier) {
        return NestOutline.occupied(mask,tier);
    }
    static VoxelShape withFrame(VoxelShape nest) { return Shapes.or(nest,FRAME); }

    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(FRAMED) ? FRAMED_COLLISION : BASE;
    }

    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) {
        // The rack is not a sturdy surface for generic blocks; scaffolding has its own support rule.
        return BASE;
    }
    // Placement and later support changes use the same rule.
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasPlacementSupport(level, pos);
    }

    static boolean hasPlacementSupport(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        if (support.getBlock() instanceof IncubationNestBlock) return support.getValue(FRAMED);
        if (support.is(Blocks.HOPPER)) return true;
        return support.is(Blocks.SCAFFOLDING)
                ? support.getValue(ScaffoldingBlock.DISTANCE) < ScaffoldingBlock.STABILITY_MAX_DISTANCE
                : support.isFaceSturdy(level, below, Direction.UP);
    }

    // Schedule only when the support changes; avoid removing the block before recovering its contents.
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && level instanceof net.minecraft.server.level.ServerLevel server
                && !hasPlacementSupport(server, pos)) server.scheduleTick(pos, this, 1);
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
            BlockPos pos, net.minecraft.util.RandomSource random) {
        if (hasPlacementSupport(level, pos)) return;
        UUID owner = null;
        var drops = new java.util.ArrayList<ItemStack>(NestIncubation.SEATS);
        if (level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest) {
            owner = nest.incubationOwner();
            var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
            for (int i = 0; i < NestIncubation.SEATS; i++) {
                var seat = nest.seat(i);
                if (seat == null) continue;
                try {
                    if (player != null && NurseryEggHatchingService.alreadyDelivered(player, seat.hatchId())) continue;
                } catch (RuntimeException storageUnavailable) {
                    bat.cobblenursery.CobbleNursery.LOGGER.warn("Keeping a nest egg after a failed delivery check", storageUnavailable);
                }
                drops.add(stampedEgg(seat));
            }
        }
        if (!level.destroyBlock(pos, true)) return;
        for (var egg : drops) {
            var item = new net.minecraft.world.entity.item.ItemEntity(level,
                    pos.getX()+.5, pos.getY()+.45, pos.getZ()+.5, egg);
            if (owner != null) item.setTarget(owner);
            item.setDefaultPickUpDelay();
            if (!level.addFreshEntity(item) && owner != null) NestRecovery.defer(level.getServer(), owner, egg);
        }
    }

    @Override protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                            BiConsumer<ItemStack,BlockPos> drops) {
        if (!occupied(level,pos,state)) super.onExplosionHit(state,level,pos,explosion,drops);
    }
    private static boolean occupied(BlockGetter level,BlockPos pos,BlockState state) {
        return state.getValue(SEATS)!=0 || level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest && nest.occupied();
    }
    public static void protectOccupiedBreaks() {
        IncubationNestBlockEntity.registerIntake();
        NestRecovery.register();
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity) -> {
            if (!(state.getBlock() instanceof IncubationNestBlock) || !occupied(level,pos,state)) return true;
            if (!(player instanceof ServerPlayer server) || !(entity instanceof IncubationNestBlockEntity nest)
                    || !nest.occupied() || !nest.mayAccess(player.getUUID())) {
                message(player,"private");
                return false;
            }
            var eggs = new java.util.ArrayList<ItemStack>();
            try {
                for (int index=0;index<NestIncubation.SEATS;index++) {
                    var seat=nest.seat(index);
                    if (seat==null) continue;
                    if (NurseryEggHatchingService.alreadyDelivered(server,seat.hatchId())) continue;
                    if (NurseryEggItem.readIncubatableData(seat.stack()).isEmpty()) {
                        message(player,"break_storage");
                        return false;
                    }
                    eggs.add(stampedEgg(seat));
                }
            } catch(RuntimeException storageUnavailable) {
                message(player,"break_storage");
                return false;
            }
            // No inventory, seat, or world mutation until vanilla and protection listeners succeed.
            nest.breakRecovery=new IncubationNestBlockEntity.BreakRecovery(player.getUUID(),java.util.List.copyOf(eggs));
            return true;
        });
        PlayerBlockBreakEvents.CANCELED.register((level,player,pos,state,entity) -> {
            if (entity instanceof IncubationNestBlockEntity nest) nest.breakRecovery=null;
        });
        PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity) -> {
            if (!(player instanceof ServerPlayer server) || !(entity instanceof IncubationNestBlockEntity nest)) return;
            var plan=nest.breakRecovery;
            nest.breakRecovery=null;
            if (plan==null || !plan.owner().equals(player.getUUID()) || level.getBlockState(pos).is(state.getBlock())) return;

            boolean deferred=false;
            for (var egg:plan.eggs()) {
                if (NestRecovery.putInInventory(server,egg)) continue;
                var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.45,pos.getZ()+.5,egg);
                drop.setTarget(plan.owner());
                drop.setDefaultPickUpDelay();
                if (!level.addFreshEntity(drop)) {
                    NestRecovery.defer(server.server,plan.owner(),egg);
                    deferred=true;
                }
            }
            if (deferred) message(player,"break_deferred");
        });
    }

    static ItemStack stampedEgg(IncubationNestBlockEntity.Seat seat) {
        var recovered=seat.stack().copy();
        var outer=recovered.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var stamp=new CompoundTag();
        stamp.putUUID("hatch",seat.hatchId());
        stamp.putLong("ready",seat.readyAt());
        stamp.putLong("issued",System.currentTimeMillis());
        outer.put(STAMP,stamp);
        recovered.set(DataComponents.CUSTOM_DATA,CustomData.of(outer));
        return recovered;
    }


    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                         Player player,InteractionHand hand,BlockHitResult hit) {
        if (stack.is(Items.SCAFFOLDING)) {
            if (state.getValue(FRAMED) && hit.getDirection() == Direction.UP) {
                // Let the vanilla scaffold item place the block above.
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide() && !state.getValue(FRAMED) && player.mayBuild()
                    && level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest) {
                if (!nest.mayAccess(player.getUUID())) {
                    message(player,"private");
                } else if (level.setBlock(pos,state.setValue(FRAMED,true),Block.UPDATE_ALL)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SCAFFOLDING_PLACE,
                            net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!stack.is(NurseryRegistry.NURSERY_EGG_ITEM))
            return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide() && player instanceof ServerPlayer server) deposit(level,pos,server,stack);
        // Do not let a rejected deposit turn into an ordinary item-use action.
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer server) useSeat(level,pos,hit,server);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static void deposit(Level level,BlockPos pos,ServerPlayer player,ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest)) return;
        if (!nest.mayAccess(player.getUUID())) { message(player,"private"); return; }
        int index=nest.firstAvailableSeat();
        if (index<0) { message(player,"full"); return; }
        var egg=NurseryEggItem.readIncubatableData(held).orElse(null);
        if (egg==null) { message(player,"invalid"); return; }
        var eggOwner=NurseryEggItem.automationOwner(held);
        if (eggOwner!=null && !eggOwner.equals(player.getUUID())) { message(player,"private"); return; }
        long now=System.currentTimeMillis();
        var seat=prepareSeat(held,egg,now);
        if (nest.insert(index,player.getUUID(),seat)) {
            held.shrink(1);
            nest.changedAndSync();
            player.getInventory().setChanged();
            status(player,seat.readyAt(),now,nest);
        } else if (eggOwner!=null) {
            message(player,"private");
        }
    }

    static IncubationNestBlockEntity.Seat prepareSeat(ItemStack held,
                bat.cobblenursery.nursery.model.GeneratedEggData egg, long now) {
        long ready=now+NestIncubation.durationSeconds(egg.ivs())*1000L;
        UUID hatchId=UUID.randomUUID();
        CompoundTag stamp=held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(STAMP);
        if (stamp.hasUUID("hatch")) {
            long saved=stamp.getLong("ready");
            if (NestIncubation.acceptsRecoveredDeadline(egg.ivs(),saved,now,stamp.getLong("issued"))) { hatchId=stamp.getUUID("hatch"); ready=saved; }
        }
        return new IncubationNestBlockEntity.Seat(hatchId,ready,held.copyWithCount(1));
    }

    private static void useSeat(Level level,BlockPos pos,BlockHitResult hit,ServerPlayer player) {
        if (!(level.getBlockEntity(pos) instanceof IncubationNestBlockEntity nest)) return;
        if (!nest.mayAccess(player.getUUID())) { message(player,"private"); return; }
        if (!player.isShiftKeyDown() && level instanceof net.minecraft.server.level.ServerLevel server)
            nest.refreshWarmth(server,System.currentTimeMillis());
        var eye=player.getEyePosition();
        var direction=hit.getLocation().subtract(eye).normalize();
        var reach=player.blockInteractionRange();
        var end=eye.add(direction.scale(reach));
        int turn=NestOutline.turn(nest.getBlockState().getValue(FACING));
        var origin=net.minecraft.world.phys.Vec3.atLowerCornerOf(pos);
        var localEye=NestOutline.point(eye.subtract(origin),-turn);
        var localEnd=NestOutline.point(end.subtract(origin),-turn);
        var base=SELECTION_BASE.clip(localEye,localEnd,BlockPos.ZERO);
        double obstruction=base==null ? 1 : localEye.distanceTo(base.getLocation())/reach;
        boolean hitsFrameFirst=false;
        if (nest.getBlockState().getValue(FRAMED)) {
            var frame=FRAME.clip(eye,end,pos);
            if (frame!=null) {
                double frameDistance=eye.distanceTo(frame.getLocation())/reach;
                hitsFrameFirst=frameDistance<=obstruction;
                obstruction=Math.min(obstruction,frameDistance);
            }
        }
        long clickTime=System.currentTimeMillis();
        int index=NestLayout.hitSizedRay(nest.seatMask(),localEye,localEnd,obstruction,
                nest::sizeTier,slot->nest.serverRemaining(slot,clickTime),nest::motionSeed);
        if (index<0 && hitsFrameFirst && player.isShiftKeyDown() && player.mayBuild()) {
            var state=nest.getBlockState();
            if (level.setBlock(pos,state.setValue(FRAMED,false),Block.UPDATE_ALL)) {
                if (!player.getAbilities().instabuild) popResource(level,pos,new ItemStack(Items.SCAFFOLDING));
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SCAFFOLDING_BREAK,
                        net.minecraft.sounds.SoundSource.BLOCKS,1F,1F);
            }
            return;
        }
        if (index<0) { message(player,"empty"); return; }
        var seat=nest.seat(index);
        if (seat==null) { message(player,"empty"); return; }
        var data=NurseryEggItem.readIncubatableData(seat.stack()).orElse(null);
        if (data==null) { message(player,"invalid"); return; }
        long now=System.currentTimeMillis();
        if (player.isShiftKeyDown()) {
            try {
                if (NurseryEggHatchingService.alreadyDelivered(player,seat.hatchId())) { nest.clear(index,seat); return; }
            } catch(RuntimeException unavailable) { message(player,"storage"); return; }
            int slot=player.getInventory().getFreeSlot();
            if (slot<0 || !player.getInventory().getItem(slot).isEmpty()) { message(player,"inventory"); return; }
            ItemStack recovered=stampedEgg(seat);
            player.getInventory().setItem(slot,recovered);
            if (player.getInventory().getItem(slot)==recovered && nest.clear(index,seat)) {
                player.getInventory().setChanged();
                message(player,"recovered");
            }
            return;
        }
        if (now<seat.readyAt()) { status(player,seat.readyAt(),now,nest); return; }
        var result=NurseryEggHatchingService.hatchIncubated(player,data,seat.hatchId());
        if (result.success() && nest.clear(index,seat)) {
            if (level instanceof net.minecraft.server.level.ServerLevel server) NestHatchSounds.play(server,pos);
            message(player,"hatched",result.pokemon().getSpecies().getTranslatedName());
        } else message(player,"storage");
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide() || type != NurseryRegistry.INCUBATION_NEST_ENTITY) return null;
        return (world, pos, current, entity) -> {
            if (entity instanceof IncubationNestBlockEntity nest) IncubationNestBlockEntity.tickWarmth(world, pos, current, nest);
        };
    }

    private static void status(Player player,long deadline,long now,
                               IncubationNestBlockEntity nest) {
        player.displayClientMessage(NestStatusText.format(deadline, now, nest.activeWarmthMask()), true);
    }

    private static void message(Player player,String key,Object... args) {
        player.displayClientMessage(Component.translatable("gui.cobblenursery.nest."+key,args),true);
    }
}