package bat.cobblenursery.nest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShelterGeometryTest {
    private static final int OPEN = 0, COVER = 1, SOLID = 2;
    private static int barn(int x, int y, int z) {
        if (y == 3 && Math.abs(x) <= 2 && Math.abs(z) <= 2) return SOLID;
        if (y == 0 && (Math.abs(x) == 2 || z == -2) && Math.abs(z) <= 2 && Math.abs(x) <= 2)
            return COVER;
        return OPEN;
    }

    @Test void openFrontAndFenceWallsShelterSeveralNests() {
        assertTrue(ShelterGeometry.check(0, 1, 0, ShelterGeometryTest::barn));
        assertTrue(ShelterGeometry.check(1, 1, 0, ShelterGeometryTest::barn));
    }

    @Test void roofAloneAndTwoWallsAreNotEnough() {
        assertFalse(ShelterGeometry.check(0, 1, 0, (x,y,z) -> y == 3 ? SOLID : OPEN));
        assertFalse(ShelterGeometry.check(0, 1, 0, (x,y,z) -> z == -2 && y == 0 ? OPEN : barn(x,y,z)));
    }

    @Test void roofMissingOverNestFailsEvenWithThreeWalls() {
        assertFalse(ShelterGeometry.check(0, 1, 0, (x,y,z) -> x == 0 && z == 0 && y == 3 ? OPEN : barn(x,y,z)));
    }

    @Test void toleratesWindowButNotOnlyThreeScatteredPosts() {
        assertTrue(ShelterGeometry.check(0, 1, 0, (x,y,z) -> z == -2 && x == 0 && y == 0 ? OPEN : barn(x,y,z)));
        assertFalse(ShelterGeometry.check(0, 1, 0, (x,y,z) -> {
            int block = barn(x,y,z);
            if (block == COVER && x != 0 && z != 0) return OPEN;
            return block;
        }));
    }

    @Test void uncertainUnloadedCellsDoNotActAsWallsOrRoof() {
        assertFalse(ShelterGeometry.check(0, 1, 0, (x,y,z) -> x < 0 || z < 0 ? -1 : barn(x,y,z)));
    }
}
