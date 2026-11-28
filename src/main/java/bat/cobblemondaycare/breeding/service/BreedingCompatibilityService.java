package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.config.ServerNurserySettings;
import bat.cobblemondaycare.nursery.model.BreedingCompatibilityResult;
import bat.cobblemondaycare.nursery.model.ParentSnapshot;

public final class BreedingCompatibilityService {
    public BreedingCompatibilityResult evaluate(ParentSnapshot a, ParentSnapshot b) {
        if (a == null || b == null) {
            return BreedingCompatibilityResult.failure("missing_parent");
        }
        if (!a.breedable() || !b.breedable()) {
            return BreedingCompatibilityResult.failure("undiscovered_egg_group");
        }
        if (a.originalPokemonId().equals(b.originalPokemonId())) {
            return BreedingCompatibilityResult.failure("same_pokemon");
        }
        if (a.ditto() && b.ditto()) {
            return BreedingCompatibilityResult.failure("ditto_pair_not_allowed");
        }
        if (a.ditto() || b.ditto()) {
            return ServerNurserySettings.current().breeding().allowDitto()
                    ? BreedingCompatibilityResult.success() : BreedingCompatibilityResult.failure("ditto_disabled");
        }
        if (!hasOppositeSex(a, b)) {
            return BreedingCompatibilityResult.failure("parents_must_have_opposite_genders");
        }
        if (!sharesEggGroup(a, b)) {
            return BreedingCompatibilityResult.failure("no_shared_egg_group");
        }
        return BreedingCompatibilityResult.success();
    }

    private boolean hasOppositeSex(ParentSnapshot a, ParentSnapshot b) {
        return (a.isMale() && b.isFemale()) || (a.isFemale() && b.isMale());
    }

    private boolean sharesEggGroup(ParentSnapshot a, ParentSnapshot b) {
        for (String eggGroup : a.eggGroupIds()) {
            if (b.eggGroupIds().contains(eggGroup)) {
                return true;
            }
        }
        return false;
    }
}
