package bat.cobblemondaycare.client.render;

import bat.cobblemondaycare.CobblemonDaycare;
import bat.cobblemondaycare.entity.NurseryNurseEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NurseryNurseRenderer extends HumanoidMobRenderer<NurseryNurseEntity, PlayerModel<NurseryNurseEntity>> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "textures/entity/nurse_joy.png");

    public NurseryNurseRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(NurseryNurseEntity entity) {
        return TEXTURE;
    }
}
