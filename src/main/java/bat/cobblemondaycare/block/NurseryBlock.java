package bat.cobblemondaycare.block;

import bat.cobblemondaycare.config.NurseryConfig;
import bat.cobblemondaycare.entity.NurseryNurseEntity;
import bat.cobblemondaycare.network.NurseryNetworking;
import bat.cobblemondaycare.registry.NurseryRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class NurseryBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 4.0D, 14.0D, 16.0D, 12.0D);

    public NurseryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.SOUTH));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NurseryNurseEntity nurse = migrateLegacyBlock(level, pos, player);
            if (nurse != null) {
                NurseryNetworking.openNurse(serverPlayer, nurse, pos);
            } else {
                NurseryNetworking.openLegacyBlock(serverPlayer, pos);
            }

        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.cobblemondaycare.legacy.start", "/nursery start <slotA> <slotB>")
            .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.cobblemondaycare.legacy.duration", NurseryConfig.DEFAULT.baseDurationMinutes())
            .withStyle(ChatFormatting.DARK_GRAY));
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
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private NurseryNurseEntity migrateLegacyBlock(Level level, BlockPos pos, Player player) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        var existing = serverLevel.getEntitiesOfClass(NurseryNurseEntity.class, new AABB(pos).inflate(1.0D));
        if (!existing.isEmpty()) {
            level.removeBlock(pos, false);
            return existing.getFirst();
        }

        NurseryNurseEntity nurse = NurseryRegistry.NURSERY_NURSE_ENTITY.create(serverLevel);
        if (nurse == null) {
            return null;
        }

        Vec3 spawnCenter = Vec3.atBottomCenterOf(pos);
        float yaw = player.getYRot() + 180.0F;
        nurse.moveTo(spawnCenter.x, spawnCenter.y, spawnCenter.z, yaw, 0.0F);
        nurse.setYRot(yaw);
        nurse.setYHeadRot(yaw);
        nurse.yBodyRot = yaw;
        nurse.yHeadRot = yaw;
        if (!serverLevel.addFreshEntity(nurse)) {
            return null;
        }
        level.removeBlock(pos, false);
        return nurse;
    }
}
