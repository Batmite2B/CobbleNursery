package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.ParentSnapshot;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AbilityInheritanceService {
    private static final double BONUS_HIDDEN_ABILITY_CHANCE = 0.05D;

    public String resolveInheritedAbility(ParentSnapshot a, ParentSnapshot b, String childSpeciesId, long seed) {
        return resolveInheritedAbility(a, b, childSpeciesId, null, seed);
    }

    public String resolveInheritedAbility(ParentSnapshot a, ParentSnapshot b, String childSpeciesId, String childFormId, long seed) {
        ParentSnapshot relevantParent = selectRelevantParent(a, b);
        AbilitySlots childAbilities = getAbilitySlots(childSpeciesId, childFormId);
        AbilitySlots parentAbilities = getAbilitySlots(relevantParent.speciesId(), relevantParent.formId());
        return resolveInheritedAbility(relevantParent, parentAbilities, childAbilities, seed);
    }

    String resolveInheritedAbility(ParentSnapshot relevantParent, List<String> parentAbilities, List<String> childAbilities, long seed) {
        AbilitySlots parentSlots = new AbilitySlots(
            parentAbilities.subList(0, Math.min(2, parentAbilities.size())),
            parentAbilities.size() >= 3 ? parentAbilities.get(2) : null
        );
        AbilitySlots childSlots = new AbilitySlots(
            childAbilities.subList(0, Math.min(2, childAbilities.size())),
            childAbilities.size() >= 3 ? childAbilities.get(2) : null
        );
        return resolveInheritedAbility(relevantParent, parentSlots, childSlots, seed);
    }

    private String resolveInheritedAbility(ParentSnapshot relevantParent, AbilitySlots parentAbilities, AbilitySlots childAbilities, long seed) {
        if (childAbilities.commonAbilities().isEmpty() && childAbilities.hiddenAbilityId() == null) {
            return relevantParent.abilityId();
        }

        boolean hiddenParent = relevantParent.abilityId().equals(parentAbilities.hiddenAbilityId());
        int parentSlot = resolveParentAbilitySlot(parentAbilities.commonAbilities(), relevantParent.abilityId());
        Random random = new Random(seed ^ 0x243F6A8885A308D3L);

        if (hiddenParent && childAbilities.hiddenAbilityId() != null) {
            if (random.nextDouble() < 0.60D) {
                return childAbilities.hiddenAbilityId();
            }
            if (!childAbilities.commonAbilities().isEmpty()) {
                return childAbilities.commonAbilities().get(random.nextInt(childAbilities.commonAbilities().size()));
            }
            return childAbilities.hiddenAbilityId();
        }

        if (childAbilities.hiddenAbilityId() != null && passesBonusHiddenAbilityRoll(seed)) {
            return childAbilities.hiddenAbilityId();
        }

        if (parentSlot >= 0 && parentSlot < childAbilities.commonAbilities().size() && random.nextDouble() < 0.80D) {
            return childAbilities.commonAbilities().get(parentSlot);
        }

        if (!childAbilities.commonAbilities().isEmpty()) {
            return childAbilities.commonAbilities().get(random.nextInt(childAbilities.commonAbilities().size()));
        }

        return childAbilities.hiddenAbilityId() != null ? childAbilities.hiddenAbilityId() : relevantParent.abilityId();
    }

    private boolean passesBonusHiddenAbilityRoll(long seed) {
        long mixed = seed ^ (seed >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53l;
        mixed ^= (mixed >>> 33);
        int bucket = (int) Math.floorMod(mixed, 100L);
        return bucket < (int) Math.round(BONUS_HIDDEN_ABILITY_CHANCE * 100.0D);
    }

    private ParentSnapshot selectRelevantParent(ParentSnapshot a, ParentSnapshot b) {
        if (a.ditto()) {
            return b;
        }
        if (b.ditto()) {
            return a;
        }
        if (a.isFemale()) {
            return a;
        }
        if (b.isFemale()) {
            return b;
        }
        return a;
    }

    private int resolveParentAbilitySlot(List<String> parentAbilities, String abilityId) {
        for (int index = 0; index < parentAbilities.size(); index++) {
            if (parentAbilities.get(index).equals(abilityId)) {
                return index;
            }
        }
        return -1;
    }

    private AbilitySlots getAbilitySlots(String speciesId, String formId) {
        ResourceLocation identifier = ResourceLocation.tryParse(speciesId);
        if (identifier == null) {
            return new AbilitySlots(List.of(), null);
        }
        Species species = PokemonSpecies.getByIdentifier(identifier);
        if (species == null) {
            return new AbilitySlots(List.of(), null);
        }
        FormData form = formId == null || formId.isBlank()
            ? species.getStandardForm()
            : species.getFormByName(formId);

        List<String> commonAbilities = new ArrayList<>();
        String hiddenAbilityId = null;
        for (PotentialAbility potentialAbility : form.getAbilities()) {
            if (potentialAbility instanceof HiddenAbility) {
                hiddenAbilityId = potentialAbility.getTemplate().getName();
            } else {
                commonAbilities.add(potentialAbility.getTemplate().getName());
            }
        }
        return new AbilitySlots(commonAbilities, hiddenAbilityId);
    }

    private record AbilitySlots(
        List<String> commonAbilities,
        String hiddenAbilityId
    ) {
    }
}
