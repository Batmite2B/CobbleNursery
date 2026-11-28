package bat.cobblemondaycare.nursery.model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ParentSnapshot(
    UUID originalPokemonId,
    String speciesId,
    String formId,
    String gender,
    String natureId,
    String abilityId,
    String ballId,
    boolean breedable,
    boolean ditto,
    int hpIv,
    int attackIv,
    int defenceIv,
    int specialAttackIv,
    int specialDefenceIv,
    int speedIv,
    List<String> eggGroupIds,
    List<String> moveIds,
    CompoundTag pokemonData
) {
    public static ParentSnapshot fromPokemon(RegistryAccess registries, Pokemon pokemon) {
        List<String> eggGroups = new ArrayList<>();
        for (EggGroup eggGroup : pokemon.getSpecies().getEggGroups()) {
            eggGroups.add(eggGroup.name());
        }
        List<String> moveIds = new ArrayList<>();
        pokemon.getMoveSet().getMoves().forEach(move -> moveIds.add(move.getName()));

        boolean isDitto = pokemon.getSpecies().getName().equalsIgnoreCase("ditto")
            || eggGroups.contains(EggGroup.DITTO.name());
        boolean isBreedable = !eggGroups.contains(EggGroup.UNDISCOVERED.name());

        CompoundTag pokemonData = pokemon.saveToNBT(registries, new CompoundTag());

        return new ParentSnapshot(
            pokemon.getUuid(),
            pokemon.getSpecies().getResourceIdentifier().toString(),
            pokemon.getForm().getName(),
            pokemon.getGender().name(),
            pokemon.getNature().getName().toString(),
            pokemon.getAbility().getName(),
            pokemon.getCaughtBall().getName().toString(),
            isBreedable,
            isDitto,
            pokemon.getIvs().getOrDefault(Stats.HP),
            pokemon.getIvs().getOrDefault(Stats.ATTACK),
            pokemon.getIvs().getOrDefault(Stats.DEFENCE),
            pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK),
            pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE),
            pokemon.getIvs().getOrDefault(Stats.SPEED),
            eggGroups,
            moveIds,
            pokemonData
        );
    }

    public boolean isMale() {
        return Gender.MALE.name().equals(gender);
    }

    public boolean isFemale() {
        return Gender.FEMALE.name().equals(gender);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("original_pokemon_id", originalPokemonId);
        tag.putString("species_id", speciesId);
        tag.putString("form_id", formId);
        tag.putString("gender", gender);
        tag.putString("nature_id", natureId);
        tag.putString("ability_id", abilityId);
        tag.putString("ball_id", ballId);
        tag.putBoolean("breedable", breedable);
        tag.putBoolean("ditto", ditto);
        tag.putInt("hp_iv", hpIv);
        tag.putInt("attack_iv", attackIv);
        tag.putInt("defence_iv", defenceIv);
        tag.putInt("special_attack_iv", specialAttackIv);
        tag.putInt("special_defence_iv", specialDefenceIv);
        tag.putInt("speed_iv", speedIv);

        CompoundTag eggGroupTag = new CompoundTag();
        for (int i = 0; i < eggGroupIds.size(); i++) {
            eggGroupTag.putString(String.valueOf(i), eggGroupIds.get(i));
        }
        tag.put("egg_groups", eggGroupTag);

        CompoundTag moveTag = new CompoundTag();
        for (int i = 0; i < moveIds.size(); i++) {
            moveTag.putString(String.valueOf(i), moveIds.get(i));
        }
        tag.put("move_ids", moveTag);
        tag.put("pokemon_data", pokemonData.copy());
        return tag;
    }

    public static ParentSnapshot load(CompoundTag tag) {
        CompoundTag eggGroupTag = tag.getCompound("egg_groups");
        List<String> eggGroups = new ArrayList<>();
        for (String key : eggGroupTag.getAllKeys()) {
            eggGroups.add(eggGroupTag.getString(key));
        }
        CompoundTag moveTag = tag.getCompound("move_ids");
        List<String> moveIds = new ArrayList<>();
        for (String key : moveTag.getAllKeys()) {
            moveIds.add(moveTag.getString(key));
        }

        return new ParentSnapshot(
            tag.getUUID("original_pokemon_id"),
            tag.getString("species_id"),
            tag.getString("form_id"),
            tag.getString("gender"),
            tag.getString("nature_id"),
            tag.getString("ability_id"),
            tag.getString("ball_id"),
            tag.getBoolean("breedable"),
            tag.getBoolean("ditto"),
            tag.getInt("hp_iv"),
            tag.getInt("attack_iv"),
            tag.getInt("defence_iv"),
            tag.getInt("special_attack_iv"),
            tag.getInt("special_defence_iv"),
            tag.getInt("speed_iv"),
            eggGroups,
            moveIds,
            tag.getCompound("pokemon_data")
        );
    }
}
