package bat.cobblemondaycare.nursery.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record BreedingItemSnapshot(
    String itemId,
    int count
) {
    public static final BreedingItemSnapshot EMPTY = new BreedingItemSnapshot("", 0);

    public static BreedingItemSnapshot fromStack(ItemStack stack) {
        if (stack.isEmpty() || stack.getItemHolder().unwrapKey().isEmpty()) {
            return EMPTY;
        }
        ResourceLocation itemId = stack.getItemHolder().unwrapKey().orElseThrow().location();
        return new BreedingItemSnapshot(itemId.toString(), stack.getCount());
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isBlank() || count <= 0;
    }

    public boolean matchesSuffix(String suffix) {
        return !isEmpty() && itemId.endsWith(suffix);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item_id", itemId);
        tag.putInt("count", count);
        return tag;
    }

    public static BreedingItemSnapshot load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }
        return new BreedingItemSnapshot(
            tag.getString("item_id"),
            tag.getInt("count")
        );
    }
}
