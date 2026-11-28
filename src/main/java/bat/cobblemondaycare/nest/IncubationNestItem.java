package bat.cobblemondaycare.nest;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** A nest can also be installed directly inside an existing scaffold. */
public final class IncubationNestItem extends BlockItem {
    public IncubationNestItem(IncubationNestBlock block, Properties properties) {
        super(block, properties);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.SCAFFOLDING)) {
            // Let vanilla handle placement checks, game events and consumption, but target the scaffold itself.
            return place(new ScaffoldPlacement(context));
        }
        return super.useOn(context);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        if (!(context instanceof ScaffoldPlacement)) return super.getPlacementState(context);
        BlockState state = getBlock().getStateForPlacement(context);
        if (state == null) return null;
        state = state.setValue(IncubationNestBlock.FRAMED, true);
        return canPlace(context, state) ? state : null;
    }

    private static final class ScaffoldPlacement extends BlockPlaceContext {
        private ScaffoldPlacement(UseOnContext context) {
            super(context);
            replaceClicked = true;
        }
    }
}

