package bat.cobblemondaycare.client.mixin;

import bat.cobblemondaycare.client.screen.NurseryGhostProjection;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PosableModel.class, remap = false)
abstract class NurseryGhostPosableModelMixin {
    // Cobblemon 1.8.0 and 1.8.4 expose this same final method; match by name and callback arguments.
    // It runs after stock PROFILE animation is prepared, before base model and layers are emitted.
    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void cobblemonDaycare$alignCarriedModel(RenderContext context, PoseStack pose, VertexConsumer vertices,
                                                 int light, int overlay, int color, CallbackInfo callback) {
        NurseryGhostProjection.alignPosedModel((PosableModel) (Object) this, context, pose, light, overlay, color);
    }
}