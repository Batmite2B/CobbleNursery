package bat.cobblemondaycare.mixin;

import bat.cobblemondaycare.world.PokeCenterTerminal;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplateManager.class)
abstract class PokeCenterStructureMixin {
    @Inject(method="loadFromResource", at=@At("RETURN"), cancellable=true)
    private void cobblemondaycare$addPublicTerminal(ResourceLocation id,
            CallbackInfoReturnable<Optional<StructureTemplate>> cir) {
        if (!PokeCenterTerminal.isTarget(id)) return;
        var original=cir.getReturnValue().orElse(null);
        if (original==null) return;
        var serialized=original.save(new CompoundTag());
        if (!PokeCenterTerminal.add(id,serialized)) return;
        var replacement=((StructureTemplateManager)(Object)this).readStructure(serialized);
        replacement.setAuthor(original.getAuthor());
        cir.setReturnValue(Optional.of(replacement));
    }
}