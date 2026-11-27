package bat.cobblenursery.nest;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative incubation bonuses. No chunk tickets, client scans or per-egg world queries. */
public final class NestWarmth {
    private static final int MAX_HORIZONTAL = 12, MAX_VERTICAL = 8, MAX_CELLS = 2048;
    private static final double CARE_RADIUS = 16.0;
    private static final Direction[] DIRECTIONS = Direction.values();
    private NestWarmth() {}

    static boolean caregiverNear(ServerLevel level, BlockPos pos) {
        return findCaregiver(level,pos) != null;
    }

    static java.util.UUID findCaregiver(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (var e : level.getEntitiesOfClass(PokemonEntity.class,
                new AABB(center, center).inflate(CARE_RADIUS + 0.001), e -> eligible(e, center))) {
            return e.getUUID();
        }
        return null;
    }

    static boolean caregiverPresent(ServerLevel level, BlockPos pos, java.util.UUID id) {
        return id != null && level.getEntity(id) instanceof PokemonEntity e && eligible(e, Vec3.atCenterOf(pos));
    }

    private static boolean eligible(PokemonEntity entity, Vec3 center) {
        if (!entity.isAlive() || entity.isRemoved() || entity.getOwnerUUID() == null
                || entity.distanceToSqr(center) > CARE_RADIUS * CARE_RADIUS) return false;
        var pokemon = entity.getPokemon();
        return NestWarmthRules.qualifies(pokemon.getSpecies().getResourceIdentifier().toString(), pokemon.getAbility().getName());
    }

    static int light(ServerLevel level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos.above());
    }

    public static boolean enclosed(ServerLevel level, BlockPos nest) {
        return trace(level, nest.above()) != null;
    }

    static Trace trace(ServerLevel level, BlockPos center) {
        return trace(center, pos -> {
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return Cell.UNKNOWN;
            var state = level.getBlockState(pos);
            var block = state.getBlock();
            if (block instanceof DoorBlock || block instanceof TrapDoorBlock)
                return state.getValue(BlockStateProperties.OPEN) ? Cell.OPEN : Cell.SEALED;
            return Block.isShapeFullBlock(state.getCollisionShape(level, pos)) ? Cell.SEALED : Cell.OPEN;
        });
    }

    enum Cell { OPEN, SEALED, UNKNOWN }

    record Trace(java.util.Set<Long> read, java.util.Set<Long> open,
                 int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean fits(BlockPos center) {
            return minX >= center.getX()-MAX_HORIZONTAL && maxX <= center.getX()+MAX_HORIZONTAL
                && minZ >= center.getZ()-MAX_HORIZONTAL && maxZ <= center.getZ()+MAX_HORIZONTAL
                && minY >= center.getY()-MAX_VERTICAL && maxY <= center.getY()+MAX_VERTICAL;
        }
    }

    static boolean enclosed(BlockPos center, Function<BlockPos, Cell> read) {
        return trace(center,read) != null;
    }

    private static Trace trace(BlockPos center, Function<BlockPos, Cell> read) {
        if (read.apply(center) != Cell.OPEN) return null;
        boolean roof = false;
        for (int y = 1; y <= MAX_VERTICAL + 1; y++) {
            var overhead = read.apply(center.above(y));
            if (overhead == Cell.UNKNOWN) return null;
            if (overhead == Cell.SEALED) { roof = true; break; }
        }
        if (!roof) return null;
        var queue = new ArrayDeque<BlockPos>();
        var seen = new HashSet<Long>();
        var open = new HashSet<Long>();
        int minX=center.getX(),maxX=minX,minY=center.getY(),maxY=minY,minZ=center.getZ(),maxZ=minZ;
        queue.add(center);
        seen.add(center.asLong());
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            open.add(current.asLong());
            minX=Math.min(minX,current.getX()); maxX=Math.max(maxX,current.getX());
            minY=Math.min(minY,current.getY()); maxY=Math.max(maxY,current.getY());
            minZ=Math.min(minZ,current.getZ()); maxZ=Math.max(maxZ,current.getZ());
            for (var dir : DIRECTIONS) {
                var next = current.relative(dir);
                if (!seen.add(next.asLong())) continue;
                if (seen.size() > MAX_CELLS) return null;
                var cell = read.apply(next);
                if (cell == Cell.UNKNOWN) return null;
                if (cell == Cell.SEALED) continue;
                if (Math.abs(next.getX() - center.getX()) > MAX_HORIZONTAL
                        || Math.abs(next.getZ() - center.getZ()) > MAX_HORIZONTAL
                        || Math.abs(next.getY() - center.getY()) > MAX_VERTICAL) return null;
                queue.addLast(next);
            }
        }
        return new Trace(seen, open, minX,minY,minZ,maxX,maxY,maxZ);
    }
}
