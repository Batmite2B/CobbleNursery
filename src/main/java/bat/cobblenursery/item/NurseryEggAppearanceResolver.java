package bat.cobblenursery.item;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

public final class NurseryEggAppearanceResolver {
    private NurseryEggAppearanceResolver() {}

    public static NurseryEggAppearance resolve(String speciesId, String formId) {
        ResourceLocation id = speciesId == null ? null : ResourceLocation.tryParse(speciesId);
        Species species = id == null ? null : PokemonSpecies.getByIdentifier(id);
        if (species == null) {
            return NurseryEggAppearance.NORMAL;
        }
        // Match the form lookup used when the egg hatches; Cobblemon falls back to its standard form.
        FormData form = formId == null || formId.isBlank() ? species.getStandardForm() : species.getFormByName(formId);
        ElementalType primaryType = form == null ? null : form.getPrimaryType();
        return NurseryEggAppearance.fromPrimaryType(primaryType == null ? null : primaryType.getName());
    }
}
