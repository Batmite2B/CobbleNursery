package bat.cobblenursery.item;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class NurseryHatchMovesTest {
    private static final List<String> WOOPER_DEFAULTS = List.of("watergun","tailwhip","mudsport");

    @Test void wooperEquipsThreeInheritedMovesBeforeFillingWithOneDefault() {
        assertEquals(List.of("recover","ancientpower","curse","watergun"),
                NurseryHatchMoves.equipped(List.of("recover","ancientpower","curse"), WOOPER_DEFAULTS,
                        Function.identity(), Function.identity()));
    }

    @Test void fourInheritedUseAllSlotsWhileNoInheritedPreservesDefaults() {
        var egg = List.of("recover","ancientpower","curse","stockpile");
        assertEquals(egg, NurseryHatchMoves.equipped(egg, WOOPER_DEFAULTS, Function.identity(), Function.identity()));
        assertEquals(WOOPER_DEFAULTS, NurseryHatchMoves.equipped(List.of(), WOOPER_DEFAULTS,
                Function.identity(), Function.identity()));
    }

    @Test void overlapDoesNotConsumeExtraSlotAndInvalidEntriesDoNotBecomeDummyMoves() {
        Set<String> valid = Set.of("recover","mudsport","curse");
        assertEquals(List.of("mudsport","recover","curse","watergun"),
                NurseryHatchMoves.equipped(List.of("mudsport","unknown","recover","mudsport","illegal","curse"),
                        WOOPER_DEFAULTS, name -> valid.contains(name) ? name : null, Function.identity()));
    }
}