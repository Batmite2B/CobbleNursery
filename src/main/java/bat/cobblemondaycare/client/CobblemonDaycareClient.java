package bat.cobblemondaycare.client;

import bat.cobblemondaycare.client.render.NurseryNurseRenderer;
import bat.cobblemondaycare.client.render.NurseryTerminalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import bat.cobblemondaycare.registry.NurseryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;

public final class CobblemonDaycareClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(NurseryRegistry.NURSERY_BLOCK, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(NurseryRegistry.INCUBATION_NEST, RenderType.cutout());
        EntityRendererRegistry.register(NurseryRegistry.NURSERY_NURSE_ENTITY, NurseryNurseRenderer::new);
        BlockEntityRenderers.register(NurseryRegistry.NURSERY_TERMINAL_ENTITY, NurseryTerminalRenderer::new);
        BlockEntityRenderers.register(NurseryRegistry.INCUBATION_NEST_ENTITY,
                bat.cobblemondaycare.client.render.IncubationNestRenderer::new);
        NurseryClientNetworking.register();
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof com.cobblemon.mod.common.client.gui.pc.PCGUI pc) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register(
                        (rendered, graphics, mouseX, mouseY, tickDelta) -> {
                            var pokemon = pc.getPreviewPokemon$common();
                            int left = (pc.width - com.cobblemon.mod.common.client.gui.pc.PCGUI.BASE_WIDTH) / 2;
                            int bottom = (pc.height + com.cobblemon.mod.common.client.gui.pc.PCGUI.BASE_HEIGHT) / 2;
                            if (bottom - 1 + bat.cobblemondaycare.client.screen.EggGroupTab.HEIGHT <= pc.height) {
                                bat.cobblemondaycare.client.screen.EggGroupTab.draw(graphics, left + 1,
                                        bottom - 1, pokemon == null ? null : pokemon.getSpecies());
                            }
                        });
            }
        });
    }
}
