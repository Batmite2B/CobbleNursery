package bat.cobblenursery.nest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/** Positive room proofs only; a block change invalidates the proof, not the incubation clock. */
public final class NestRoomCache {
    private static final int MAX_ROOMS = 512, MAX_CELLS = 65536;
    private static final Map<ServerLevel, Index> LEVELS = new WeakHashMap<>();
    private NestRoomCache() {}

    static boolean enclosed(ServerLevel level, BlockPos nest) {
        var index = LEVELS.computeIfAbsent(level, ignored -> new Index());
        var center = nest.above();
        var old = index.air.get(center.asLong());
        if (old != null && old.usable(level, center)) return true;
        var trace = NestWarmth.trace(level, center);
        if (trace == null) return false;
        var chunks = new HashSet<ChunkPos>();
        for (long key : trace.read()) chunks.add(new ChunkPos(BlockPos.of(key)));
        var room = new Room(center, trace, chunks);
        while (!index.rooms.isEmpty() && (index.rooms.size() >= MAX_ROOMS
                || index.cells + trace.read().size() > MAX_CELLS)) index.remove(index.rooms.iterator().next());
        index.rooms.add(room);
        index.cells += trace.read().size();
        for (long key : trace.open()) index.air.put(key, room);
        for (long key : trace.read()) index.watched.computeIfAbsent(key, ignored -> new HashSet<>()).add(room);
        return true;
    }

    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (before == after) return;
        if (before.getBlock() == after.getBlock() && before.getBlock() instanceof IncubationNestBlock
                && before.getValue(IncubationNestBlock.FRAMED).equals(after.getValue(IncubationNestBlock.FRAMED))) return;
        var index = LEVELS.get(level);
        if (index == null || index.rooms.isEmpty()) return;
        index.invalidate(pos.asLong());
        for (var side : Direction.values()) index.invalidate(pos.relative(side).asLong());
    }

    private static final class Room {
        final BlockPos center;
        final NestWarmth.Trace trace;
        final Set<ChunkPos> chunks;
        Room(BlockPos center, NestWarmth.Trace trace, Set<ChunkPos> chunks) {
            this.center=center; this.trace=trace; this.chunks=chunks;
        }
        boolean usable(ServerLevel level, BlockPos pos) {
            return Math.abs(pos.getX()-center.getX()) <= 2 && Math.abs(pos.getY()-center.getY()) <= 2
                && Math.abs(pos.getZ()-center.getZ()) <= 2 && trace.fits(pos)
                && chunks.stream().allMatch(chunk -> level.hasChunk(chunk.x, chunk.z));
        }
    }

    private static final class Index {
        final Map<Long, Room> air = new HashMap<>();
        final Map<Long, Set<Room>> watched = new HashMap<>();
        final Set<Room> rooms = new LinkedHashSet<>();
        int cells;
        void invalidate(long key) {
            var found = watched.get(key);
            if (found != null) for (var room : List.copyOf(found)) remove(room);
        }
        void remove(Room room) {
            if (!rooms.remove(room)) return;
            cells -= room.trace.read().size();
            for (long key : room.trace.open()) air.remove(key, room);
            for (long key : room.trace.read()) {
                var entries = watched.get(key);
                if (entries == null) continue;
                entries.remove(room);
                if (entries.isEmpty()) watched.remove(key);
            }
        }
    }
}