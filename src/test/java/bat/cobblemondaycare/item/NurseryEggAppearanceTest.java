package bat.cobblemondaycare.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NurseryEggAppearanceTest {
    @Test
    void allStockTypesHaveStableModelValuesAndPaths() {
        String[] types = {
            "bug", "dark", "dragon", "electric", "fairy", "fighting", "fire", "flying",
            "ghost", "grass", "ground", "ice", "normal", "poison", "psychic", "rock", "steel", "water"
        };
        for (int index = 0; index < types.length; index++) {
            NurseryEggAppearance appearance = NurseryEggAppearance.fromPrimaryType(types[index]);
            assertEquals(index + 1, appearance.modelData());
            assertEquals(types[index], appearance.slug());
        }
    }

    @Test
    void missingTypeUsesNormalWithoutTreatingUnknownAsStockType() {
        assertEquals(NurseryEggAppearance.NORMAL, NurseryEggAppearance.fromPrimaryType(null));
        assertEquals(NurseryEggAppearance.NORMAL, NurseryEggAppearance.fromPrimaryType("stellar"));
        assertEquals(NurseryEggAppearance.ELECTRIC, NurseryEggAppearance.fromPrimaryType("Electric"));
    }
}
