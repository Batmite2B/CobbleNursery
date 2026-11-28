package bat.cobblemondaycare.mixin;

import bat.cobblemondaycare.nest.NestRoomCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class NestRoomInvalidationMixin {
    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void cobblemondaycare$invalidateRoom(BlockPos pos, BlockState before, BlockState after, CallbackInfo ci) {
        NestRoomCache.blockChanged((ServerLevel)(Object)this, pos, before, after);
    }
}