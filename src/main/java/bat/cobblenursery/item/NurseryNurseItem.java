package bat.cobblenursery.item;

import bat.cobblenursery.entity.NurseryNurseEntity;
import bat.cobblenursery.registry.NurseryRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class NurseryNurseItem extends Item {
    public NurseryNurseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        NurseryNurseEntity nurse = NurseryRegistry.NURSERY_NURSE_ENTITY.create(serverLevel);
        if (nurse == null) {
            return InteractionResult.FAIL;
        }

        Vec3 spawnCenter = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        float yaw = context.getHorizontalDirection().toYRot() + 180.0F;

        nurse.moveTo(spawnCenter.x, spawnCenter.y, spawnCenter.z, yaw, 0.0F);
        nurse.setYRot(yaw);
        nurse.setYHeadRot(yaw);
        nurse.yBodyRot = yaw;
        nurse.yHeadRot = yaw;
        serverLevel.addFreshEntity(nurse);

        ItemStack stack = context.getItemInHand();
        if (!context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
