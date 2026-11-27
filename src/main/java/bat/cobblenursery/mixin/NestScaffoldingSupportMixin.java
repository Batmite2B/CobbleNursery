package bat.cobblenursery.mixin;

import bat.cobblenursery.nest.IncubationNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScaffoldingBlock.class)
abstract class NestScaffoldingSupportMixin {
    // Vanilla's vertical support check only. A rack is not a horizontal scaffold or a sturdy face for other blocks.
    @Redirect(method = "getDistance", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private static boolean cobblenursery$allowScaffoldAboveNest(BlockState below, BlockGetter level, BlockPos pos, Direction face) {
        return (below.getBlock() instanceof IncubationNestBlock && below.getValue(IncubationNestBlock.FRAMED))
                || below.isFaceSturdy(level, pos, face);
    }
}
