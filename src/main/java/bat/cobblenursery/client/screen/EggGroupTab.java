package bat.cobblenursery.client.screen;

import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EggGroupTab {
    // The native PC information sheet: Nature at 0, Ability at 17, Moves at 34.
    private static final int WIDTH = 63;
    public static final int HEIGHT = 17;
    private static final int ABILITY_V = 17;
    private static final int SHEET_HEIGHT = 69;
    private static final ResourceLocation ART = ResourceLocation.fromNamespaceAndPath(
            "cobblemon", "textures/gui/pc/info_box.png");
    private EggGroupTab() {}

    public static void draw(GuiGraphics graphics, int x, int y, Species species) {
        // Align to the Ability panel's x=9 rather than the outer border at x=1.
        x += 8;
        // The left panel art ends two GUI pixels before the nominal canvas edge.
        y -= 2;
        graphics.blit(ART, x, y, 0, ABILITY_V, WIDTH, HEIGHT, WIDTH, SHEET_HEIGHT);
        int center = x + WIDTH / 2;
        drawLine(graphics, Component.translatable("gui.cobblenursery.pc.egg_group")
                .withStyle(ChatFormatting.BOLD), center, y + 1.5, false);
                var value = Component.empty();
        // ParentSnapshot uses the species-level groups for this mod's compatibility rules.
        if (species == null || species.getEggGroups().isEmpty()) {
            value.append("—");
        } else if (species.getEggGroups().contains(EggGroup.UNDISCOVERED)) {
            value.append(Component.translatable("gui.cobblenursery.pc.cannot_breed"));
        } else {
            for (var group : species.getEggGroups().stream().sorted().toList()) {
                if (!value.getSiblings().isEmpty()) value.append(" · ");
                value.append(groupName(group));
            }
        }
        drawLine(graphics, value, center, y + 9, true);
    }

    private static void drawLine(GuiGraphics graphics, Component value, int center, double y, boolean shadow) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(.5F, (WIDTH - 6F) / (font.width(value) + 1F));
        RenderHelperKt.drawScaledText(graphics, null, value.copy(), center, y, scale, 1F,
                Integer.MAX_VALUE, 0xFFFFFFFF, true, shadow, null, null);
    }
    private static Component groupName(EggGroup group) {
        return Component.translatable("cobblemon.egg_group." + group.name().toLowerCase(Locale.ROOT));
    }
}