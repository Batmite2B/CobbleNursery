package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.ParentSnapshot;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EggMoveInheritanceService {
    private static final int MAX_INHERITED_MOVES = 4;

    public List<String> resolveInheritedMoves(ParentSnapshot a, ParentSnapshot b, String childSpeciesId) {
        return resolveInheritedMoves(a, b, childSpeciesId, null);
    }

    public List<String> resolveInheritedMoves(ParentSnapshot a, ParentSnapshot b, String childSpeciesId, String childFormId) {
        Set<String> parentMoves = new LinkedHashSet<>();
        parentMoves.addAll(a.moveIds());
        parentMoves.addAll(b.moveIds());

        return combineInheritedMoves(
            resolveInheritedEggMoves(a, b, childSpeciesId, childFormId),
            resolveInheritedLevelUpMoves(a, b, childSpeciesId, childFormId)
        );
    }

    public List<String> resolveInheritedEggMoves(ParentSnapshot a, ParentSnapshot b, String childSpeciesId) {
        return resolveInheritedEggMoves(a, b, childSpeciesId, null);
    }

    public List<String> resolveInheritedEggMoves(ParentSnapshot a, ParentSnapshot b, String childSpeciesId, String childFormId) {
        Set<String> parentMoves = new LinkedHashSet<>();
        parentMoves.addAll(a.moveIds());
        parentMoves.addAll(b.moveIds());

        List<String> childEggMoves = getEggMoveNames(childSpeciesId, childFormId);
        return resolveInheritedEggMoves(parentMoves, childEggMoves);
    }

    public List<String> resolveInheritedLevelUpMoves(ParentSnapshot a, ParentSnapshot b, String childSpeciesId, String childFormId) {
        Set<String> sharedParentMoves = new LinkedHashSet<>(a.moveIds());
        sharedParentMoves.retainAll(b.moveIds());
        List<String> childLevelUpMoves = getLevelUpMoveNames(childSpeciesId, childFormId);
        return resolveInheritedLevelUpMoves(sharedParentMoves, childLevelUpMoves);
    }

    List<String> resolveInheritedEggMoves(Set<String> parentMoves, List<String> childEggMoves) {
        List<String> inheritedMoves = new ArrayList<>();
        for (String eggMove : childEggMoves) {
            if (parentMoves.contains(eggMove)) {
                inheritedMoves.add(eggMove);
            }
            if (inheritedMoves.size() >= MAX_INHERITED_MOVES) {
                break;
            }
        }
        return inheritedMoves;
    }

    List<String> resolveInheritedLevelUpMoves(Set<String> sharedParentMoves, List<String> childLevelUpMoves) {
        List<String> inheritedMoves = new ArrayList<>();
        for (String moveId : childLevelUpMoves) {
            if (sharedParentMoves.contains(moveId)) {
                inheritedMoves.add(moveId);
            }
            if (inheritedMoves.size() >= MAX_INHERITED_MOVES) {
                break;
            }
        }
        return inheritedMoves;
    }

    /** Accept only already inherited moves; legacy v3 previews also carry shared level-up moves. */
    public static List<String> projectEggMoves(List<String> inherited, String species, String form) {
        if (inherited.isEmpty()) return List.of();
        return projectEggMoves(inherited, getEggMoveNames(species, form));
    }

    static List<String> projectEggMoves(List<String> inherited, List<String> childEggMoves) {
        Set<String> validEggMoves = new LinkedHashSet<>(childEggMoves);
        return inherited.stream().filter(validEggMoves::contains).distinct().limit(MAX_INHERITED_MOVES).toList();
    }

    private static List<String> getEggMoveNames(String childSpeciesId, String childFormId) {
        ResourceLocation identifier = ResourceLocation.tryParse(childSpeciesId);
        if (identifier == null) {
            return List.of();
        }
        Species species = PokemonSpecies.getByIdentifier(identifier);
        if (species == null) {
            return List.of();
        }
        FormData form = childFormId == null || childFormId.isBlank()
            ? species.getStandardForm()
            : species.getFormByName(childFormId);
        List<String> moveNames = new ArrayList<>();
        for (MoveTemplate eggMove : form.getMoves().getEggMoves()) {
            moveNames.add(eggMove.getName());
        }
        return moveNames;
    }

    private List<String> getLevelUpMoveNames(String childSpeciesId, String childFormId) {
        ResourceLocation identifier = ResourceLocation.tryParse(childSpeciesId);
        if (identifier == null) {
            return List.of();
        }
        Species species = PokemonSpecies.getByIdentifier(identifier);
        if (species == null) {
            return List.of();
        }
        FormData form = childFormId == null || childFormId.isBlank()
            ? species.getStandardForm()
            : species.getFormByName(childFormId);
        List<String> moveNames = new ArrayList<>();
        for (MoveTemplate move : form.getMoves().getLevelUpMovesUpTo(100)) {
            moveNames.add(move.getName());
        }
        return moveNames;
    }

    List<String> combineInheritedMoves(List<String> eggMoves, List<String> levelUpMoves) {
        List<String> combined = new ArrayList<>(eggMoves);
        mergeInheritedMoves(combined, levelUpMoves);
        return combined;
    }

    private void mergeInheritedMoves(List<String> target, List<String> additions) {
        for (String moveId : additions) {
            if (target.size() >= MAX_INHERITED_MOVES) {
                break;
            }
            if (!target.contains(moveId)) {
                target.add(moveId);
            }
        }
    }
}
