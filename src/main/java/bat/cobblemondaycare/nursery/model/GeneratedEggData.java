package bat.cobblemondaycare.nursery.model;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record GeneratedEggData(
    String childSpeciesId,
    String childFormId,
    String natureId,
    String inheritedAbilityId,
    String inheritedBallId,
    List<String> inheritedMoveIds,
    Map<String, Integer> ivs,
    boolean shiny
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("child_species_id", childSpeciesId);
        tag.putString("child_form_id", childFormId);
        tag.putString("nature_id", natureId);
        tag.putString("inherited_ability_id", inheritedAbilityId);
        tag.putString("inherited_ball_id", inheritedBallId);
        tag.putBoolean("shiny", shiny);
        CompoundTag movesTag = new CompoundTag();
        for (int i = 0; i < inheritedMoveIds.size(); i++) {
            movesTag.putString(String.valueOf(i), inheritedMoveIds.get(i));
        }
        tag.put("inherited_move_ids", movesTag);

        CompoundTag ivTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : ivs.entrySet()) {
            ivTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("ivs", ivTag);
        return tag;
    }

    public static GeneratedEggData load(CompoundTag tag) {
        CompoundTag ivTag = tag.getCompound("ivs");
        CompoundTag movesTag = tag.getCompound("inherited_move_ids");
        List<String> moveIds = new ArrayList<>();
        for (String key : movesTag.getAllKeys()) {
            moveIds.add(movesTag.getString(key));
        }
        return new GeneratedEggData(
            tag.getString("child_species_id"),
            tag.getString("child_form_id"),
            tag.getString("nature_id"),
            tag.getString("inherited_ability_id"),
            tag.getString("inherited_ball_id"),
            moveIds,
            Map.of(
                "hp", ivTag.getInt("hp"),
                "attack", ivTag.getInt("attack"),
                "defence", ivTag.getInt("defence"),
                "special_attack", ivTag.getInt("special_attack"),
                "special_defence", ivTag.getInt("special_defence"),
                "speed", ivTag.getInt("speed")
            ),
            tag.getBoolean("shiny")
        );
    }
}
