package bat.cobblemondaycare.breeding.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EggMoveInheritanceServiceTest {
    private final EggMoveInheritanceService service = new EggMoveInheritanceService();

    @Test
    void inheritsMatchingEggMovesFromEitherParent() {
        List<String> moves = service.resolveInheritedEggMoves(
            new LinkedHashSet<>(Set.of("wish", "curse", "tackle")),
            List.of("wish", "yawn", "curse")
        );

        assertEquals(List.of("wish", "curse"), moves);
    }

    @Test
    void capsInheritedEggMovesAtFour() {
        List<String> moves = service.resolveInheritedEggMoves(
            new LinkedHashSet<>(Set.of("a", "b", "c", "d", "e")),
            List.of("a", "b", "c", "d", "e")
        );

        assertEquals(List.of("a", "b", "c", "d"), moves);
    }

    @Test
    void inheritsSharedLevelUpMovesWhenBothParentsKnowThem() {
        List<String> moves = service.resolveInheritedLevelUpMoves(
            new LinkedHashSet<>(Set.of("quick_attack", "wish", "fake_out")),
            List.of("growl", "quick_attack", "fake_out", "thunderbolt")
        );

        assertEquals(List.of("quick_attack", "fake_out"), moves);
    }

    @Test
    void combinedInheritancePrioritizesEggMovesThenLevelUpMoves() {
        List<String> moves = service.combineInheritedMoves(
            List.of("wish"),
            List.of("quick_attack", "fake_out", "tackle")
        );

        assertTrue(moves.contains("wish"));
        assertTrue(moves.contains("quick_attack"));
        assertEquals(List.of("wish", "quick_attack", "fake_out", "tackle"), moves);
    }
    @Test
    void previewUsesOnlyActualEggMoveInheritanceWithoutPickingUnlearnedCandidates() {
        var inherited = service.combineInheritedMoves(
                service.resolveInheritedEggMoves(Set.of("tackle","wish","curse"), List.of("wish","yawn","curse")),
                List.of("tackle","curse","growl"));
        assertEquals(List.of("wish","curse"), EggMoveInheritanceService.projectEggMoves(
                inherited, List.of("wish","yawn","curse")));
        assertEquals(List.of(), EggMoveInheritanceService.projectEggMoves(
                List.of("tackle","growl"), List.of("wish","yawn")));
        assertEquals(List.of("curse","wish","a","b"), EggMoveInheritanceService.projectEggMoves(
                List.of("curse","wish","curse","a","b","c","tackle"), List.of("wish","a","curse","b","c")));
    }

}
