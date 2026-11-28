package bat.cobblemondaycare.block;

import bat.cobblemondaycare.network.NurseryNetworking;
import bat.cobblemondaycare.terminal.NurseryTerminalBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Permanent daycare endpoint, separate from the legacy nurse migration block. */
public final class NurseryTerminalBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BASE = Block.box(0.75, 0, 0.61, 15.25, 5.55, 15.25);
    private static final VoxelShape[] SHAPES = {
        Shapes.or(BASE, Block.box(1.85, 5.55, 3.20, 14.15, 15.62, 14.10)),          // North
        Shapes.or(BASE, Block.box(1.90, 5.55, 1.85, 12.80, 15.62, 14.15)),         // East
        Shapes.or(BASE, Block.box(1.85, 5.55, 1.90, 14.15, 15.62, 12.80)),          // South
        Shapes.or(BASE, Block.box(3.20, 5.55, 1.85, 14.10, 15.62, 14.15))          // West
    };

    public NurseryTerminalBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        }];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        open(pos, player);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        open(pos, player);
        return level.isClientSide() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NurseryTerminalBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof NurseryTerminalBlockEntity terminal) {
            terminal.recordPlacer(player.getUUID());
        }
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide() || type != bat.cobblemondaycare.registry.NurseryRegistry.NURSERY_TERMINAL_ENTITY) return null;
        return (world, pos, current, entity) -> {
            if (entity instanceof NurseryTerminalBlockEntity terminal) {
                NurseryTerminalBlockEntity.tickExport(world, pos, current, terminal);
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof NurseryTerminalBlockEntity terminal) {
            terminal.removeEggOutlets();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    private static void open(BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var level = serverPlayer.serverLevel();
            var state = level.getBlockState(pos);
            // Compatibility: terminals already placed before this display upgrade had no renderer marker.
            if (state.getBlock() instanceof NurseryTerminalBlock terminal && level.getBlockEntity(pos) == null) {
                level.setBlockEntity(terminal.newBlockEntity(pos, state));
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
            NurseryNetworking.openTerminal(serverPlayer, pos);
        }
    }
}
