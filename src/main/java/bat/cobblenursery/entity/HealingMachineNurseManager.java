package bat.cobblenursery.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import bat.cobblenursery.registry.NurseryRegistry;

public final class HealingMachineNurseManager {
    private static final ResourceLocation HEALING_MACHINE_ID = ResourceLocation.fromNamespaceAndPath("cobblemon", "healing_machine");
    private static final Map<ServerLevel, Set<BlockPos>> PENDING_SPAWNS = new IdentityHashMap<>();

    private HealingMachineNurseManager() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            Block healingMachineBlock = getHealingMachineBlock();
            if (healingMachineBlock == Blocks.AIR || !player.getItemInHand(hand).is(healingMachineBlock.asItem())) {
                return InteractionResult.PASS;
            }

            BlockPos placementPos = resolvePlacementPos(level.getBlockState(hitResult.getBlockPos()), hitResult.getBlockPos(), hitResult.getDirection());
            PENDING_SPAWNS.computeIfAbsent((ServerLevel) level, ignored -> new HashSet<>()).add(placementPos.immutable());
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Set<BlockPos> pending = PENDING_SPAWNS.remove(world);
            if (pending == null || pending.isEmpty()) {
                return;
            }

            for (BlockPos machinePos : pending) {
                trySpawnNurse(world, machinePos);
            }
        });
    }

    public static boolean isHealingMachine(BlockState state) {
        Block healingMachineBlock = getHealingMachineBlock();
        return healingMachineBlock != Blocks.AIR && state.is(healingMachineBlock);
    }

    private static void trySpawnNurse(ServerLevel world, BlockPos machinePos) {
        BlockState state = world.getBlockState(machinePos);
        if (!isHealingMachine(state) || hasLinkedNurse(world, machinePos)) {
            return;
        }

        Direction front = resolveFront(state).getOpposite();
        BlockPos nursePos = machinePos.relative(front);
        if (!isClearSpawnSpot(world, nursePos)) {
            return;
        }

        NurseryNurseEntity nurse = NurseryRegistry.NURSERY_NURSE_ENTITY.create(world);
        if (nurse == null) {
            return;
        }

        nurse.setAnchorPos(machinePos);
        float yaw = front.getOpposite().toYRot();
        Vec3 spawnCenter = Vec3.atBottomCenterOf(nursePos);
        nurse.moveTo(spawnCenter.x, spawnCenter.y, spawnCenter.z, yaw, 0.0F);
        nurse.setYRot(yaw);
        nurse.setYHeadRot(yaw);
        nurse.yBodyRot = yaw;
        nurse.yHeadRot = yaw;

        if (!world.noCollision(nurse)) {
            nurse.discard();
            return;
        }

        world.addFreshEntity(nurse);
    }

    private static boolean hasLinkedNurse(ServerLevel world, BlockPos machinePos) {
        return !world.getEntitiesOfClass(
            NurseryNurseEntity.class,
            new AABB(machinePos).inflate(2.5D),
            nurse -> machinePos.equals(nurse.getAnchorPos())
        ).isEmpty();
    }

    private static boolean isClearSpawnSpot(ServerLevel world, BlockPos nursePos) {
        return world.getBlockState(nursePos).canBeReplaced()
            && world.getBlockState(nursePos.above()).canBeReplaced()
            && world.getBlockState(nursePos.below()).isFaceSturdy(world, nursePos.below(), Direction.UP);
    }

    private static Direction resolveFront(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction direction = state.getValue(BlockStateProperties.FACING);
            if (direction.getAxis().isHorizontal()) {
                return direction;
            }
        }
        return Direction.NORTH;
    }

    private static BlockPos resolvePlacementPos(BlockState clickedState, BlockPos clickedPos, Direction clickedFace) {
        return clickedState.canBeReplaced() ? clickedPos : clickedPos.relative(clickedFace);
    }

    private static Block getHealingMachineBlock() {
        return BuiltInRegistries.BLOCK.get(HEALING_MACHINE_ID);
    }
}
