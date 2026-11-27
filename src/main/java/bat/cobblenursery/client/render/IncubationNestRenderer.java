package bat.cobblenursery.client.render;

import bat.cobblenursery.nest.IncubationNestBlockEntity;
import bat.cobblenursery.nest.IncubationNestBlock;
import bat.cobblenursery.nest.NestOutline;
import com.mojang.math.Axis;
import bat.cobblenursery.registry.NurseryRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Reuses the original typed 3D item models, never a tile atlas or a visible Blockbench guide. */
public final class IncubationNestRenderer implements BlockEntityRenderer<IncubationNestBlockEntity> {
    private final ItemRenderer items;
    private final NestWiggleSound wiggleSound=new NestWiggleSound();
    private final ItemStack[] models=new ItemStack[19];

    public IncubationNestRenderer(BlockEntityRendererProvider.Context context) { items=context.getItemRenderer(); }
    @Override public void render(IncubationNestBlockEntity nest,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        int mask=nest.getBlockState().getValue(IncubationNestBlock.SEATS);
        int turn=NestOutline.turn(nest.getBlockState().getValue(IncubationNestBlock.FACING));
        long now=net.minecraft.Util.getMillis();
        int count=Integer.bitCount(mask);
        for(var base:bat.cobblenursery.nest.NestLayout.poses(mask)) {
            var layout=bat.cobblenursery.nest.NestLayout.sized(base,nest.sizeTier(base.slot()),mask);
            long remaining=nest.clientRemaining(base.slot(),now);
            int seed=nest.motionSeed(base.slot());
            var placement=bat.cobblenursery.nest.NestLayout.moving(layout,count,remaining,seed);
            int i=placement.slot();
            int style=nest.model(i);
            if (style<=0 || style>=models.length) continue;
            wiggleSound.observe(nest,i,remaining,seed,now);
            if (models[style]==null) {
                ItemStack stack=new ItemStack(NurseryRegistry.NURSERY_EGG_ITEM);
                stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(style));
                models[style]=stack;
            }
            pose.pushPose();
            try {
                // NONE keeps the raw authored egg proportions; ItemRenderer centers at .5, and authored feet start at 1/16.
                if (turn!=0) {
                    pose.translate(.5,0,.5);
                    pose.mulPose(Axis.YP.rotationDegrees(-90f*turn));
                    pose.translate(-.5,0,-.5);
                }
                float scale=placement.scale();
                pose.translate(placement.x()/16,placement.sole()/16,placement.z()/16);
                pose.mulPose(placement.rotation());
                pose.translate(0,(.5-1.0/16)*scale,0);
                pose.scale(scale,scale,scale);
                items.renderStatic(models[style],ItemDisplayContext.NONE,light,overlay,pose,buffers,nest.getLevel(),i);
            } finally { pose.popPose(); }
        }
    }
}