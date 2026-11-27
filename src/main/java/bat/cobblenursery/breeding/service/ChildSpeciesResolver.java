package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.ParentSnapshot;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ChildSpeciesResolver {
    private static final String MANAPHY = "cobblemon:manaphy";
    private static final String PHIONE = "cobblemon:phione";
    private static final String ILLUMISE = "cobblemon:illumise";
    private static final String VOLBEAT = "cobblemon:volbeat";
    private static final String NIDORAN_F = "cobblemon:nidoranf";
    private static final String NIDORAN_M = "cobblemon:nidoranm";
    private static final String STANDARD_FORM = "Normal";

    public String resolveBaseChildSpecies(ParentSnapshot a, ParentSnapshot b) {
        if (a.ditto()) {
            return b.speciesId();
        }
        if (b.ditto()) {
            return a.speciesId();
        }
        if (a.isFemale()) {
            return a.speciesId();
        }
        if (b.isFemale()) {
            return b.speciesId();
        }
        return a.speciesId();
    }

    public ResolvedChildIdentity resolveChildIdentity(ParentSnapshot a, ParentSnapshot b, BreedingItemSnapshot itemA,
                                                      BreedingItemSnapshot itemB, long seed) {
        SelectedParent selectedParent = selectRelevantParent(a, b, itemA, itemB);
        String baseSpeciesId = resolveParentBaseSpeciesId(selectedParent.parent().speciesId(), seed);
        String childSpeciesId = walkToBabySpecies(baseSpeciesId, this::resolvePreEvolutionSpeciesId);
        String childFormId = resolveInheritedFormId(
            selectedParent.parent().formId(),
            selectedParent.item().matchesSuffix("everstone"),
            getChildFormNames(childSpeciesId),
            getStandardFormName(childSpeciesId)
        );
        return new ResolvedChildIdentity(childSpeciesId, childFormId);
    }

    String resolveParentBaseSpeciesId(String parentSpeciesId, long seed) {
        return switch (parentSpeciesId) {
            case MANAPHY -> PHIONE;
            case ILLUMISE -> chooseSpecies(seed ^ 0x9E3779B97F4A7C15L, ILLUMISE, VOLBEAT);
            case NIDORAN_F -> chooseSpecies(seed ^ 0xC6BC279692B5CC83L, NIDORAN_F, NIDORAN_M);
            default -> parentSpeciesId;
        };
    }

    String walkToBabySpecies(String speciesId, Function<String, String> preEvolutionResolver) {
        String current = speciesId;
        while (true) {
            String previous = preEvolutionResolver.apply(current);
            if (previous == null || previous.isBlank()) {
                return current;
            }
            current = previous;
        }
    }

    String resolveInheritedFormId(String parentFormId, boolean hasEverstone, List<String> childFormIds,
                                  String standardFormId) {
        String effectiveStandardFormId = standardFormId == null || standardFormId.isBlank()
            ? STANDARD_FORM
            : standardFormId;
        if (!hasEverstone) {
            return effectiveStandardFormId;
        }

        String normalizedParentForm = normalizeFormId(parentFormId);
        if (normalizedParentForm.isBlank() || normalizedParentForm.equals(normalizeFormId(effectiveStandardFormId))) {
            return effectiveStandardFormId;
        }

        for (String childFormId : childFormIds) {
            if (normalizeFormId(childFormId).equals(normalizedParentForm)) {
                return childFormId;
            }
        }

        return effectiveStandardFormId;
    }

    private SelectedParent selectRelevantParent(ParentSnapshot a, ParentSnapshot b, BreedingItemSnapshot itemA,
                                                BreedingItemSnapshot itemB) {
        if (a.ditto()) {
            return new SelectedParent(b, itemB);
        }
        if (b.ditto()) {
            return new SelectedParent(a, itemA);
        }
        if (a.isFemale()) {
            return new SelectedParent(a, itemA);
        }
        if (b.isFemale()) {
            return new SelectedParent(b, itemB);
        }
        return new SelectedParent(a, itemA);
    }

    private String resolvePreEvolutionSpeciesId(String speciesId) {
        Species species = getSpecies(speciesId);
        if (species == null || species.getPreEvolution() == null || species.getPreEvolution().getSpecies() == null) {
            return null;
        }
        return species.getPreEvolution().getSpecies().getResourceIdentifier().toString();
    }

    private List<String> getChildFormNames(String speciesId) {
        Species species = getSpecies(speciesId);
        if (species == null) {
            return List.of(STANDARD_FORM);
        }
        List<String> formNames = new ArrayList<>();
        String standardFormName = species.getStandardForm().getName();
        formNames.add(standardFormName);
        for (FormData form : species.getForms()) {
            if (formNames.stream().noneMatch(existing -> normalizeFormId(existing).equals(normalizeFormId(form.getName())))) {
                formNames.add(form.getName());
            }
        }
        return formNames;
    }

    private String getStandardFormName(String speciesId) {
        Species species = getSpecies(speciesId);
        return species == null ? STANDARD_FORM : species.getStandardForm().getName();
    }

    private Species getSpecies(String speciesId) {
        ResourceLocation identifier = ResourceLocation.tryParse(speciesId);
        if (identifier == null) {
            return null;
        }
        return PokemonSpecies.getByIdentifier(identifier);
    }

    private String normalizeFormId(String formId) {
        if (formId == null) {
            return "";
        }
        return formId.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private String chooseSpecies(long seed, String first, String second) {
        long mixed = seed ^ (seed >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53l;
        mixed ^= (mixed >>> 33);
        return (mixed & 1L) == 0L ? first : second;
    }

    private record SelectedParent(
        ParentSnapshot parent,
        BreedingItemSnapshot item
    ) {
    }
}
