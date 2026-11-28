package bat.cobblemondaycare.breeding.service;

import bat.cobblemondaycare.nursery.model.BreedingItemSnapshot;
import bat.cobblemondaycare.nursery.model.GeneratedEggData;
import bat.cobblemondaycare.nursery.model.ParentSnapshot;

public final class BreedingResultFactory {
    private final ChildSpeciesResolver childSpeciesResolver = new ChildSpeciesResolver();
    private final IvInheritanceService ivInheritanceService = new IvInheritanceService();
    private final NatureInheritanceService natureInheritanceService = new NatureInheritanceService();
    private final BallInheritanceService ballInheritanceService = new BallInheritanceService();
    private final AbilityInheritanceService abilityInheritanceService = new AbilityInheritanceService();
    private final EggMoveInheritanceService eggMoveInheritanceService = new EggMoveInheritanceService();
    private final ShinyChanceService shinyChanceService = new ShinyChanceService();

    public GeneratedEggData generate(ParentSnapshot a, ParentSnapshot b, BreedingItemSnapshot itemA,
                                     BreedingItemSnapshot itemB, long seed) {
        ResolvedChildIdentity childIdentity = childSpeciesResolver.resolveChildIdentity(a, b, itemA, itemB, seed);
        String nature = natureInheritanceService.resolveNature(a, b, itemA, itemB, seed);
        String ability = abilityInheritanceService.resolveInheritedAbility(
            a,
            b,
            childIdentity.childSpeciesId(),
            childIdentity.childFormId(),
            seed
        );
        String inheritedBall = ballInheritanceService.resolveInheritedBall(a, b, seed);
        var inheritedMoves = eggMoveInheritanceService.resolveInheritedMoves(
            a,
            b,
            childIdentity.childSpeciesId(),
            childIdentity.childFormId()
        );
        boolean shiny = shinyChanceService.rollShiny(seed);
        return new GeneratedEggData(
            childIdentity.childSpeciesId(),
            childIdentity.childFormId(),
            nature,
            ability,
            inheritedBall,
            inheritedMoves,
            ivInheritanceService.generateInheritedIvs(a, b, itemA, itemB, seed),
            shiny
        );
    }
}
