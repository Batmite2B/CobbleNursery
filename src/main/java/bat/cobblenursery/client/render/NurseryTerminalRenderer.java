package bat.cobblenursery.client.render;

import bat.cobblenursery.CobbleNursery;
import bat.cobblenursery.block.NurseryTerminalBlock;
import bat.cobblenursery.client.NurseryClientNetworking;
import bat.cobblenursery.terminal.NurseryTerminalBlockEntity;
import bat.cobblenursery.terminal.TerminalDisplayState;
import bat.cobblenursery.terminal.TerminalDisplayMotion;
import bat.cobblenursery.terminal.TerminalStatusAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

public final class NurseryTerminalRenderer implements BlockEntityRenderer<NurseryTerminalBlockEntity> {
    private static final ResourceLocation OFF = texture("nursery_terminal_display.png");
    private static final ResourceLocation ON = texture("nursery_terminal_display_on.png");
    private static final RenderType OFF_LAYER = RenderType.entityCutoutNoCull(OFF);
    private static final RenderType ON_LAYER = RenderType.entityCutoutNoCull(ON);
    // Pixel-space numbers follow the exact approved North-facing display element in the Java model.
    private static final float LEFT = 12.96F / 16, RIGHT = 3.04F / 16;
    private static final float BOTTOM = 6.50F / 16, TOP = 13.14F / 16;
    private static final float FRONT = 4.232F / 16;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final Map<NurseryTerminalBlockEntity, TerminalDisplayMotion> motion = new WeakHashMap<>();

    public NurseryTerminalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public void render(NurseryTerminalBlockEntity block, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var player = Minecraft.getInstance().player;
        TerminalDisplayState state = NurseryClientNetworking.terminalState();
        // The opaque base model is already the idle screen. No overlay is emitted until we know the viewer.
        if (player == null || state == null || block.getLevel() != player.level()) {
            return;
        }
        long now = Util.getMillis();
        TerminalDisplayMotion.Frame frame = motion.computeIfAbsent(block, b -> new TerminalDisplayMotion())
            .frame(player.position().distanceToSqr(Vec3.atCenterOf(block.getBlockPos())), now);
        if (frame == TerminalDisplayMotion.Frame.OFF && state.status() == TerminalDisplayState.Status.IDLE) {
            return;
        }
        pose.pushPose();
        pose.translate(.5, 0, .5);
        float turn = switch (block.getBlockState().getValue(NurseryTerminalBlock.FACING)) {
            case EAST -> -90F;
            case SOUTH -> 180F;
            case WEST -> 90F;
            default -> 0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.translate(-.5, 0, -.5);
        pose.translate(.5, 9.8 / 16, 5.15 / 16);
        pose.mulPose(Axis.XP.rotationDegrees(22.5F));
        pose.translate(-.5, -9.8 / 16, -5.15 / 16);
        if (frame != TerminalDisplayMotion.Frame.OFF) {
            for (int i = 0; i < 4; i++) {
                boolean unlocked = state.unlocked(i);
                float u0 = (i % 2) * .5F, v0 = (i / 2) * .5F;
                quad(pose.last(), buffers.getBuffer(unlocked ? ON_LAYER : OFF_LAYER),
                     u0, v0, u0 + .5F, v0 + .5F, unlocked ? -1 : 0, unlocked ? FULL_BRIGHT : light, unlocked && frame == TerminalDisplayMotion.Frame.TRANSITION ? 112 : 255);
            }
        } else {
            boolean full = state.status() == TerminalDisplayState.Status.FULL;
            if (full) {
                icon(pose.last(), buffers.getBuffer(OFF_LAYER), 27, 13, 37, 35, 2,
                    1.0, TerminalStatusAnimation.alertTint(now));
            } else {
                icon(pose.last(), buffers.getBuffer(OFF_LAYER), 21, 15, 43, 33, 1,
                    TerminalStatusAnimation.heartScale(now), 255);
            }

        }
        pose.popPose();
    }

    private static void icon(PoseStack.Pose pose, VertexConsumer buffer, int x0, int y0, int x1, int y1,
                             int row, double scale, int tint) {
        iconVertex(pose, buffer, x0, y0, row, scale, tint);
        iconVertex(pose, buffer, x0, y1, row, scale, tint);
        iconVertex(pose, buffer, x1, y1, row, scale, tint);
        iconVertex(pose, buffer, x1, y0, row, scale, tint);
    }

    private static void iconVertex(PoseStack.Pose pose, VertexConsumer buffer, int px, int py, int row,
                                   double scale, int tint) {
        float u = (float) (.5 + (px / 64.0 - .5) * scale);
        float v = (float) (.5 + (py / 48.0 - .5) * scale);
        buffer.addVertex(pose, LEFT + u * (RIGHT - LEFT), TOP + v * (BOTTOM - TOP), FRONT)
            .setColor(tint, tint, tint, 255).setUv(px / 64F, (row * 48 + py) / 192F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 0, -1);
    }

    private static ResourceLocation texture(String filename) {
        return ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/block/" + filename);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float u0, float v0,
                             float u1, float v1, int atlasRow, int light, int tint) {
        vertex(pose, buffer, u0, v0, atlasRow, light, tint);
        vertex(pose, buffer, u0, v1, atlasRow, light, tint);
        vertex(pose, buffer, u1, v1, atlasRow, light, tint);
        vertex(pose, buffer, u1, v0, atlasRow, light, tint);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float u, float v, int row, int light, int tint) {
        float x = LEFT + u * (RIGHT - LEFT);
        float y = TOP + v * (BOTTOM - TOP);
        float texV = row < 0 ? v : (row + v) / 4F;
        buffer.addVertex(pose, x, y, FRONT).setColor(tint, tint, tint, 255)
            .setUv(u, texV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(pose, 0, 0, -1);
    }
}
