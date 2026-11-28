package bat.cobblemondaycare.item;

import bat.cobblemondaycare.nursery.model.GeneratedEggData;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokeball.PokeBall;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;


import java.util.LinkedHashMap;
import java.util.Map;

public final class NurseryEggHatchingService {
    private NurseryEggHatchingService() {
    }

    public static HatchResult hatchIncubated(ServerPlayer player, GeneratedEggData eggData, java.util.UUID hatchId) {
        try {
            return hatchOrReconcile(player,eggData,hatchId);
        } catch (RuntimeException failed) {
            bat.cobblemondaycare.CobblemonDaycare.LOGGER.warn("Incubation hatch could not confirm delivery for {}", hatchId, failed);
            return HatchResult.failure("Could not deliver the incubated Pokémon.");
        }
    }

    public static boolean alreadyDelivered(ServerPlayer player,java.util.UUID hatchId) {
        return findDelivered(player,hatchId) != null;
    }

    private static Pokemon findDelivered(ServerPlayer player,java.util.UUID hatchId) {
        var party=Cobblemon.INSTANCE.getStorage().getParty(player);
        var pc=party.getOverflowPC(player.server.registryAccess());
        var inParty=confirmedInStore(party,hatchId);
        var inPc=pc==null ? null : confirmedInStore(pc,hatchId);
        if (inParty!=null && inPc!=null) throw new IllegalStateException("Duplicate incubated Pokémon in player storage");
        return inParty!=null ? inParty : inPc;
    }

    private static Pokemon confirmedInStore(com.cobblemon.mod.common.api.storage.PokemonStore<?> store,java.util.UUID hatchId) {
        Pokemon occupying=null;
        for (Pokemon pokemon : store) if (pokemon!=null && hatchId.equals(pokemon.getUuid())) {
            if (occupying!=null) throw new IllegalStateException("Duplicate incubated Pokémon slots");
            occupying=pokemon;
        }
        return bat.cobblemondaycare.nest.NestIncubation.requireCommitted(occupying,store.get(hatchId),
                coordinatesMatch(store,occupying));
    }

    private static boolean coordinatesMatch(com.cobblemon.mod.common.api.storage.PokemonStore<?> store,Pokemon pokemon) {
        if (pokemon==null) return false;
        var address=pokemon.getStoreCoordinates().get();
        return address!=null && address.getStore()==store && address.get()==pokemon;
    }

    private static HatchResult hatchOrReconcile(ServerPlayer player, GeneratedEggData eggData, java.util.UUID hatchId) {
        Pokemon existing=findDelivered(player,hatchId);
        if (existing!=null) return HatchResult.success(existing);
        ResourceLocation speciesId = ResourceLocation.tryParse(eggData.childSpeciesId());
        if (speciesId == null) {
            return HatchResult.failure("Invalid species id in nursery egg.");
        }

        Species species = PokemonSpecies.getByIdentifier(speciesId);
        if (species == null) {
            return HatchResult.failure("Could not resolve species for nursery egg.");
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setUuid(hatchId);
        pokemon.setSpecies(species);
        pokemon.setForm(resolveForm(species, eggData.childFormId()));
        pokemon.setLevel(1);
        pokemon.initialize();

        applyNature(pokemon, eggData);
        applyAbility(pokemon, eggData);
        applyBall(pokemon, eggData);
        applyIvs(pokemon, eggData);
        applyMoveset(pokemon, eggData);
        pokemon.setShiny(eggData.shiny());
        pokemon.validateMoveset(true);

        try {
            Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
        } catch (RuntimeException callbackFailure) {
            // A storage notification can throw after insertion; never retry before reconciliation.
            Pokemon partial=findDelivered(player,hatchId);
            if (partial!=null) return HatchResult.success(partial);
            throw callbackFailure;
        }
        Pokemon delivered=findDelivered(player,hatchId);
        return delivered == null ? HatchResult.failure("Could not place the hatched Pokémon in party or PC.")
                : HatchResult.success(delivered);
    }

    private static FormData resolveForm(Species species, String childFormId) {
        if (childFormId == null || childFormId.isBlank()) {
            return species.getStandardForm();
        }
        return species.getFormByName(childFormId);
    }

    private static void applyNature(Pokemon pokemon, GeneratedEggData eggData) {
        ResourceLocation natureId = ResourceLocation.tryParse(eggData.natureId());
        pokemon.setNature(natureId != null ? Natures.getNature(natureId) : Natures.getNature(eggData.natureId()));
    }

    private static void applyAbility(Pokemon pokemon, GeneratedEggData eggData) {
        pokemon.setAbility$common(Abilities.getOrDummy(eggData.inheritedAbilityId()).create(false, Priority.LOWEST));
        pokemon.updateAbility(pokemon.getAbility());
    }

    private static void applyBall(Pokemon pokemon, GeneratedEggData eggData) {
        ResourceLocation ballId = ResourceLocation.tryParse(eggData.inheritedBallId());
        PokeBall pokeBall = ballId != null ? PokeBalls.getPokeBall(ballId) : null;
        pokemon.setCaughtBall(pokeBall != null ? pokeBall : PokeBalls.getPokeBall());
    }

    private static void applyIvs(Pokemon pokemon, GeneratedEggData eggData) {
        IVs ivs = IVs.createRandomIVs(0);
        Map<com.cobblemon.mod.common.api.pokemon.stats.Stat, Integer> ordered = new LinkedHashMap<>();
        ordered.put(Stats.HP, eggData.ivs().getOrDefault("hp", 0));
        ordered.put(Stats.ATTACK, eggData.ivs().getOrDefault("attack", 0));
        ordered.put(Stats.DEFENCE, eggData.ivs().getOrDefault("defence", 0));
        ordered.put(Stats.SPECIAL_ATTACK, eggData.ivs().getOrDefault("special_attack", 0));
        ordered.put(Stats.SPECIAL_DEFENCE, eggData.ivs().getOrDefault("special_defence", 0));
        ordered.put(Stats.SPEED, eggData.ivs().getOrDefault("speed", 0));
        ordered.forEach(ivs::set);
        pokemon.setIvs$common(ivs);
    }

    private static void applyMoveset(Pokemon pokemon, GeneratedEggData eggData) {
        MoveSet moveSet = pokemon.getMoveSet();
        var learnset = pokemon.getForm().getMoves();
        var equipped = NurseryHatchMoves.equipped(eggData.inheritedMoveIds(), moveSet.getMoves(), id -> {
            var template = Moves.getByName(id);
            if (template == null || !com.cobblemon.mod.common.api.pokemon.moves.LearnsetQuery.Companion
                    .getANY().canLearn(template, learnset)) return null;
            return template.create();
        }, Move::getName);
        moveSet.clear();
        equipped.forEach(moveSet::add);
    }

    public record HatchResult(
        boolean success,
        String message,
        Pokemon pokemon
    ) {
        public static HatchResult success(Pokemon pokemon) {
            return new HatchResult(true, "Nursery egg hatched successfully.", pokemon);
        }

        public static HatchResult failure(String message) {
            return new HatchResult(false, message, null);
        }
    }
}
