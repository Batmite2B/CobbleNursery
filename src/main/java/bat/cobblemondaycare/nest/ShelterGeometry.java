package bat.cobblemondaycare.nest;

final class ShelterGeometry {
    static final int OPEN = 0, COVER = 1, SOLID = 2, RACK = 3;
    private static final int ROOF_HEIGHT = 8, SIDE_DISTANCE = 12, MAX_RACK_DROP = 8;
    private static final int[][] SIDES = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    @FunctionalInterface
    interface Blocks {
        int at(int x, int y, int z);
    }

    static boolean check(int x, int y, int z, Blocks blocks) {
        int roof = roof(x, y, z, blocks);
        if (roof < 0) return false;
        int neighboringRoof = 0;
        for (var side : SIDES)
            if (roof(x + side[0], y, z + side[1], blocks) >= 0) neighboringRoof++;
        if (neighboringRoof < 2) return false;
        if (hasThreeSides(x, y, z, roof, blocks)) return true;

        // The roof is checked at this nest. Only a continuous rack lets it borrow
        // side protection from the base, instead of treating a floor below as walls.
        int bottom = y;
        while (y - bottom < MAX_RACK_DROP && blocks.at(x, bottom - 2, z) == RACK) {
            bottom--;
        }
        if (bottom == y || (y - bottom == MAX_RACK_DROP && blocks.at(x, bottom - 2, z) == RACK)) return false;
        return hasThreeSides(x, bottom, z, roof + y - bottom, blocks);
    }

    private static boolean hasThreeSides(int x, int y, int z, int roof, Blocks blocks) {
        int covered = 0;
        for (var side : SIDES)
            if (protectedSide(x,y,z,roof,side[0],side[1],blocks) && ++covered == 3) return true;
        return false;
    }

    private static int roof(int x, int y, int z, Blocks blocks) {
        for (int h = 1; h <= ROOF_HEIGHT; h++) {
            int cell = blocks.at(x,y+h,z);
            if (cell < 0) return -1;
            if (cell == SOLID) return h;
        }
        return -1;
    }

    private static boolean protectedSide(int x, int y, int z, int roof, int dx, int dz, Blocks blocks) {
        for (int distance = 1; distance <= SIDE_DISTANCE; distance++) {
            int columns = 0;
            for (int offset = -1; offset <= 1; offset++) {
                int sx = x + distance * dx + offset * dz;
                int sz = z + distance * dz + offset * dx;
                boolean covered = false;
                // Count a low barrier (e.g. fence) or a wall at the air cell above the nest.
                for (int h = -1; h < Math.min(roof, 2); h++) {
                    int cell = blocks.at(sx, y+h, sz);
                    if (cell == COVER || cell == SOLID) { covered = true; break; }
                }
                if (covered && ++columns == 2) return true;
            }
        }
        return false;
    }

    private ShelterGeometry() {}
}
