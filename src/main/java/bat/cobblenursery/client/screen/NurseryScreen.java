package bat.cobblenursery.client.screen;


import bat.cobblenursery.network.*;
import bat.cobblenursery.text.NurseryMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NurseryScreen extends Screen {
    private static final int WINDOW_W = 420;
    private static final int WINDOW_H = 278;
    private static final int PARTY_X = 14;
    private static final int PARTY_Y = 42;
    private static final int PARTY_W = 162;
    private static final int PARTY_ROW_H = 22;
    private static final int PREVIEW_X = 184;
    private static final int PREVIEW_Y = 42;
    private static final int PREVIEW_W = 222;
    private static final int PREVIEW_H = 124;
    private static final int PROCESS_X = 14;
    private static final int PROCESS_Y = 182;
    private static final int PROCESS_W = 392;
    private static final int PROCESS_ROW_H = 26;

    private OpenNurseryScreenS2CPayload payload;
    private NurseryPreviewPayload preview;
    private int leftPos;
    private int topPos;
    private int selectedSlotA = -1;
    private int selectedSlotB = -1;
    private String message;
    private boolean messageSuccess;

    public NurseryScreen(OpenNurseryScreenS2CPayload payload) {
        super(Component.literal("CobbleNursery"));
        this.payload = payload;
        this.preview = payload.preview();
        this.message = payload.message();
        this.messageSuccess = payload.messageSuccess();
        syncSelectionFromPreview();
    }

    public void applyPayload(OpenNurseryScreenS2CPayload payload) {
        this.payload = payload;
        this.preview = payload.preview();
        this.message = payload.message();
        this.messageSuccess = payload.messageSuccess();
        syncSelectionFromPreview();
        if (this.minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WINDOW_W) / 2;
        this.topPos = (this.height - WINDOW_H) / 2;
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        for (NurseryPartySlotPayload slot : payload.partySlots()) {
            int index = slot.slotIndex();
            int y = this.topPos + PARTY_Y + index * PARTY_ROW_H;
            this.addRenderableWidget(Button.builder(partySlotLabel(slot), btn -> {
                    togglePartySelection(index);
                })
                .bounds(this.leftPos + PARTY_X, y, PARTY_W, 20)
                .build());
        }

        this.addRenderableWidget(Button.builder(tr("preview_pair"), btn -> requestPreview())
            .bounds(this.leftPos + PREVIEW_X, this.topPos + 134, 96, 20)
            .build());

        Button startButton = Button.builder(tr("start"), btn -> startSelectedPair())
            .bounds(this.leftPos + PREVIEW_X + 104, this.topPos + 134, 118, 20)
            .build();
        startButton.active = canStartSelectedPair();
        this.addRenderableWidget(startButton);

        this.addRenderableWidget(Button.builder(tr("refresh"), btn -> ClientPlayNetworking.send(new NurseryRefreshC2SPayload(payload.nurseryPos())))
            .bounds(this.leftPos + PREVIEW_X, this.topPos + 158, 72, 20)
            .build());

        for (NurseryProcessSlotPayload slot : payload.processSlots()) {
            int index = slot.slotIndex();
            int rowY = this.topPos + PROCESS_Y + index * PROCESS_ROW_H;

            Button claimButton = Button.builder(tr("claim"), btn ->
                    ClientPlayNetworking.send(new NurseryClaimC2SPayload(payload.nurseryPos(), index)))
                .bounds(this.leftPos + 252, rowY, 48, 20)
                .build();
            claimButton.active = "READY".equals(slot.status());
            this.addRenderableWidget(claimButton);

            Button cancelButton = Button.builder(tr("cancel"), btn ->
                    ClientPlayNetworking.send(new NurseryCancelC2SPayload(payload.nurseryPos(), index)))
                .bounds(this.leftPos + 306, rowY, 54, 20)
                .build();
            cancelButton.active = "BREEDING".equals(slot.status()) || "READY".equals(slot.status());
            this.addRenderableWidget(cancelButton);

            if (payload.canForceReady()) {
                Button finishButton = Button.builder(tr("finish"), btn ->
                        ClientPlayNetworking.send(new NurseryForceReadyC2SPayload(payload.nurseryPos(), index)))
                    .bounds(this.leftPos + 366, rowY, 40, 20)
                    .build();
                finishButton.active = "BREEDING".equals(slot.status());
                this.addRenderableWidget(finishButton);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawWindow(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawForeground(graphics);
    }

    private void drawWindow(GuiGraphics graphics) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + WINDOW_W, this.topPos + WINDOW_H, 0xE01E1F24);
        graphics.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + WINDOW_W - 2, this.topPos + WINDOW_H - 2, 0xF0D8D2C4);
        graphics.fill(this.leftPos + PARTY_X - 4, this.topPos + PARTY_Y - 6, this.leftPos + PARTY_X + PARTY_W + 4, this.topPos + PARTY_Y + 6 + PARTY_ROW_H * 6, 0xAA2F3842);
        graphics.fill(this.leftPos + PREVIEW_X - 4, this.topPos + PREVIEW_Y - 6, this.leftPos + PREVIEW_X + PREVIEW_W + 4, this.topPos + PREVIEW_Y + PREVIEW_H, 0xAA2F3842);
        graphics.fill(this.leftPos + PROCESS_X - 4, this.topPos + PROCESS_Y - 6, this.leftPos + PROCESS_X + PROCESS_W + 4, this.topPos + PROCESS_Y + 6 + PROCESS_ROW_H * Math.max(1, payload.processSlots().size()), 0xAA2F3842);
    }

    private void drawForeground(GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.leftPos + 14, this.topPos + 12, 0x1E1F24, false);
        graphics.drawString(this.font, tr("parents"), this.leftPos + PARTY_X, this.topPos + 28, 0xFFFFFF, false);
        graphics.drawString(this.font, tr("preview"), this.leftPos + PREVIEW_X, this.topPos + 28, 0xFFFFFF, false);
        graphics.drawString(this.font, tr("processes"), this.leftPos + PROCESS_X, this.topPos + 168, 0xFFFFFF, false);

        graphics.drawString(this.font,
            tr("cost", payload.startCost())
                .withStyle(payload.canAfford() ? ChatFormatting.DARK_GREEN : ChatFormatting.RED),
            this.leftPos + PREVIEW_X,
            this.topPos + 50,
            0xFFFFFF,
            false
        );

        graphics.drawString(this.font,
            tr("selected", selectedSummary()),
            this.leftPos + PREVIEW_X,
            this.topPos + 68,
            0xF2E5B8,
            false
        );

        int previewTextY = this.topPos + 86;
        if (this.preview == null) {
            drawWrapped(graphics, tr("choose"), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xD8D8D8);
        } else {
            drawWrapped(graphics, tr("compat", NurseryMessages.component(this.preview.reason())), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, this.preview.compatible() ? 0x7CFF86 : 0xFF8484);
            previewTextY += 22;
            if (this.preview.hasEggData()) {
                drawWrapped(graphics, tr("egg", nativeLabel("species", this.preview.childSpeciesId()).append(formSuffix(this.preview.childFormId()))), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xFFFFFF);
                previewTextY += 12;
                drawWrapped(graphics, tr("nature", nativeLabel("nature", this.preview.natureId())), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xCDE8FF);
                previewTextY += 12;
                drawWrapped(graphics, tr("ability", nativeLabel("ability", this.preview.abilityId())), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xB8FFB8);
                previewTextY += 12;
                drawWrapped(graphics, tr("ball", nativeLabel("item", this.preview.ballId())), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xFFD17A);
                previewTextY += 12;
                if (!this.preview.inheritedMoveIds().isEmpty()) {
                    drawWrapped(graphics, tr("moves", humanizeList(this.preview.inheritedMoveIds())), this.leftPos + PREVIEW_X, previewTextY, PREVIEW_W - 8, 0xF5AEFF);
                    previewTextY += 22;
                }
                drawWrapped(
                    graphics,
                    tr("ivs", this.preview.hpIv(), this.preview.attackIv(), this.preview.defenceIv(), this.preview.specialAttackIv(), this.preview.specialDefenceIv(), this.preview.speedIv()),
                    this.leftPos + PREVIEW_X,
                    previewTextY,
                    PREVIEW_W - 8,
                    0xD8D8D8
                );
            }
        }

        for (NurseryProcessSlotPayload slot : payload.processSlots()) {
            int rowY = this.topPos + PROCESS_Y + slot.slotIndex() * PROCESS_ROW_H + 6;
            var label = tr("slot", slot.slotIndex() + 1, humanizeStatus(slot.status()));
            if (!slot.parentSummary().isBlank()) {
                label.append(" | ").append(parentSummary(slot.parentSummary()));
            }
            if ("BREEDING".equals(slot.status()) && slot.remainingMs() > 0L) {
                label.append(" | " + remainingMinutes(slot.remainingMs()) + " min");
            }
            graphics.drawString(this.font, label, this.leftPos + 18, rowY, 0xFFFFFF, false);
        }

        if (!message.isBlank()) {
            graphics.drawString(this.font, NurseryMessages.component(message), this.leftPos + 14, this.topPos + WINDOW_H - 16, messageSuccess ? 0x64E572 : 0xFF7D7D, false);
        }
    }

    private void requestPreview() {
        if (selectedSlotA < 0 || selectedSlotB < 0) {
            return;
        }
        ClientPlayNetworking.send(new NurseryPreviewC2SPayload(payload.nurseryPos(), selectedSlotA, selectedSlotB));
    }

    private void startSelectedPair() {
        if (!canStartSelectedPair()) {
            return;
        }
        ClientPlayNetworking.send(new NurseryStartC2SPayload(payload.nurseryPos(), selectedSlotA, selectedSlotB));
    }

    private boolean canStartSelectedPair() {
        return this.preview != null
            && this.preview.compatible()
            && this.preview.hasEggData()
            && matchesSelection(this.preview, selectedSlotA, selectedSlotB);
    }

    private void togglePartySelection(int slotIndex) {
        if (this.selectedSlotA == slotIndex) {
            this.selectedSlotA = -1;
        } else if (this.selectedSlotB == slotIndex) {
            this.selectedSlotB = -1;
        } else if (this.selectedSlotA < 0) {
            this.selectedSlotA = slotIndex;
        } else if (this.selectedSlotB < 0) {
            this.selectedSlotB = slotIndex;
        } else {
            this.selectedSlotA = this.selectedSlotB;
            this.selectedSlotB = slotIndex;
        }
        this.preview = null;
        this.message = "";
        rebuildWidgets();
    }

    private void syncSelectionFromPreview() {
        if (this.preview != null && this.preview.slotA() >= 0 && this.preview.slotB() >= 0) {
            this.selectedSlotA = this.preview.slotA();
            this.selectedSlotB = this.preview.slotB();
        }
    }

    private boolean matchesSelection(NurseryPreviewPayload preview, int a, int b) {
        return (preview.slotA() == a && preview.slotB() == b) || (preview.slotA() == b && preview.slotB() == a);
    }

    private Component partySlotLabel(NurseryPartySlotPayload slot) {
        String prefix = slot.slotIndex() == selectedSlotA || slot.slotIndex() == selectedSlotB ? "> " : "";
        if (!slot.occupied()) {
            return Component.literal(prefix).append(tr("slot_empty", slot.slotIndex() + 1));
        }
        var label = Component.literal(prefix).append(tr("slot_mon", slot.slotIndex() + 1, nativeLabel("species", slot.speciesId()), slot.level()));
        return slot.heldItemId().isBlank() ? label : label.append(" | ").append(nativeLabel("item", slot.heldItemId()));
    }

    private Component selectedSummary() {
        List<String> selected = new ArrayList<>();
        if (selectedSlotA >= 0) {
            selected.add(String.valueOf(selectedSlotA + 1));
        }
        if (selectedSlotB >= 0) {
            selected.add(String.valueOf(selectedSlotB + 1));
        }
        return selected.isEmpty() ? tr("none") : Component.literal(String.join(" + ", selected));
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        int line = 0;
        for (var seq : this.font.split(text, width)) {
            graphics.drawString(this.font, seq, x, y + (line * 10), color, false);
            line++;
            if (line >= 3) {
                break;
            }
        }
    }

    private Component humanizeStatus(String raw) {
        return switch (raw) {
            case "EMPTY" -> tr("empty");
            case "BREEDING" -> tr("breeding");
            case "READY" -> tr("ready");
            case "CLAIMED" -> tr("claimed");
            case "CANCELLED" -> tr("cancelled");
            default -> Component.literal(raw);
        };
    }

    private long remainingMinutes(long remainingMs) {
        return remainingMs <= 0 ? 0 : (remainingMs + 59_999L) / 60_000L;
    }

    private Component parentSummary(String raw) {
        var parts = raw.split(" \\+ ", -1);
        return parts.length == 2 ? nativeLabel("species", parts[0]).append(" + ").append(nativeLabel("species", parts[1]))
            : Component.literal(raw);
    }

    private String formSuffix(String formId) {
        return isStandardForm(formId) ? "" : " (" + humanizeId(formId) + ")";
    }

    private boolean isStandardForm(String formId) {
        if (formId == null || formId.isBlank()) {
            return true;
        }
        String normalized = formId.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.isBlank() || normalized.equals("normal");
    }

    private Component humanizeList(List<String> ids) {
        var values = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) values.append(", ");
            values.append(nativeLabel("move", ids.get(i)));
        }
        return values;
    }

    private net.minecraft.network.chat.MutableComponent nativeLabel(String kind, String rawId) {
        if (rawId == null || rawId.isBlank()) return Component.empty();
        var id = ResourceLocation.tryParse(rawId);
        String name = id == null ? rawId : id.getPath();
        String key = kind.equals("item") ? "item." + (rawId.contains(":") && id != null ? id.getNamespace() : "cobblemon") + "." + name
            : "cobblemon." + kind + "." + name + (kind.equals("species") ? ".name" : "");
        return Component.translatableWithFallback(key, humanizeId(rawId));
    }
    private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.cobblenursery.legacy." + key, args);
    }

    private String humanizeId(String rawId) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(rawId);
        String source = resourceLocation != null ? resourceLocation.getPath() : rawId;
        String[] parts = source.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? rawId : builder.toString();
    }
}
