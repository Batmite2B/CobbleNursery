package bat.cobblenursery.client.screen;

import bat.cobblenursery.CobbleNursery;
import bat.cobblenursery.client.NurseryUiSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.sounds.SoundManager;
import bat.cobblenursery.config.NurseryConfig;
import bat.cobblenursery.network.*;
import bat.cobblenursery.nursery.model.NurseryTransfer;
import bat.cobblenursery.registry.NurseryRegistry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.gui.ExitButton;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.IconButton;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockActiveAnimation;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import bat.cobblenursery.item.NurseryEggAppearance;
import bat.cobblenursery.item.NurseryEggAppearanceResolver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DaycareScreen extends Screen {

    private static final ResourceLocation TRADE_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/trade/trade_button.png");
    private static final ResourceLocation TRADE_BUTTON_DISABLED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/trade/trade_button_disabled.png");

    private static final int TRADE_BUTTON_TEXTURE_WIDTH = 53;
    private static final int TRADE_BUTTON_TEXTURE_HEIGHT = 14;


    private static final int DETAILS_INFO_START_Y = 105;
    private static final int DETAILS_INFO_LINE_GAP = 9;
    private static final int DETAILS_INFO_LABEL_X = 228;
    private static final int DETAILS_INFO_VALUE_X = 250;
    private static final int DETAILS_INFO_REASON_MAX_CHARS = 22;


    private static final int DETAILS_RESULT_START_Y = 140;
    private static final int DETAILS_RESULT_LINE_GAP = 9;
    private static final int DETAILS_RESULT_LABEL_X = DETAILS_INFO_LABEL_X;
    private static final int DETAILS_RESULT_VALUE_X = 253;
    private static final int DETAILS_ROLLS_INFO_Y_OFFSET = -13;
    private static final int DETAILS_MOVES_START_Y = 164;
    private static final int DETAILS_MOVES_SECTION_GAP = 9;




    private static final int COLOR_PANEL_TITLE = 0xFFF4F2F6;
    private static final int COLOR_PANEL_SUBTITLE = 0xFFB8B2BA;
    private static final int COLOR_PANEL_VALUE = 0xFFF2EEF2;
    private static final int COLOR_PANEL_REASON = 0xFFD9D3DA;
    private static final int COLOR_PANEL_NATURE = COLOR_PANEL_VALUE;
    private static final int COLOR_PANEL_ABILITY = 0xFFF7E6B6;
    private static final int COLOR_PANEL_BALL = 0xFFBDD8FF;



    private static final ResourceLocation DAYCARE_BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare_base.png");
    private static final ResourceLocation DAYCARE_BASE_ROLLS_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare_base_rolls.png");
    private static final ResourceLocation[] YARD_HEART_TEXTURES = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare/egg_heart_01.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare/egg_heart_02.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare/egg_heart_03.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare/egg_heart_04.png"),
            ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/daycare/egg_heart_05.png")
    };
    private static final ResourceLocation PARTY_GENDER_MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/gender_icon_male.png");
    private static final ResourceLocation PARTY_GENDER_FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/gender_icon_female.png");
    private static final ResourceLocation PARTY_POINTER_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/pc_pointer.png");


    private static final ResourceLocation[] YARD_WALLPAPER_TEXTURES = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/wallpaper/biome/wallpaper_biome_forest.png"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/wallpaper/biome/wallpaper_biome_ocean.png"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/wallpaper/biome/wallpaper_biome_cave.png"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/wallpaper/biome/wallpaper_biome_the_end.png")
    };

    private static final int BIOME_WALLPAPER_WIDTH = 174;
    private static final int BIOME_WALLPAPER_HEIGHT = 155;
    private static final int YARD_HEART_WIDTH = 6;
    private static final int YARD_HEART_HEIGHT = 24;


    private static final int YARD_NAME_TAG_Y_OFFSET = 1;
    private static final int YARD_NAME_TAG_MAX_CHARS = 10;
    private static final int GUI_SCALE = DaycareLayout.DEFAULT_SCALE;
    private static final float PARTY_RENDER_SCALE = 1.04F;
    private static final float YARD_RENDER_SCALE = (float) NurseryPcLayout.STAGE_MODEL_SCALE;
    private static final float DETAILS_RENDER_SCALE = 1.06F;
    private static final float PC_PARTY_RENDER_POSE_SCALE = 2.5F;
    private static final float PC_PARTY_RENDER_SCALE = 4.5F;
    private static final float PC_PARTY_RENDER_SIZE_RECOVERY = 0.45F;
    private static final float YARD_YAW_RIGHT = -18.0F;
    private static final float YARD_YAW_LEFT = 18.0F;
    private static final float YARD_TILT_IDLE = 7.0F;
    private static final float YARD_TILT_WALK = 10.0F;
    private static final float YARD_TILT_SLEEP = 24.0F;
    private static final double YARD_SIDE_PADDING = 18.0D;
    private static final double YARD_CENTER_PADDING = 10.0D;
    private static final double YARD_WALK_SPEED = 0.018D;


    private static final ResourceLocation PC_PORTRAIT_BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/portrait_background.png");

    private static final int PC_PORTRAIT_BACKGROUND_WIDTH = 66;
    private static final int PC_PORTRAIT_BACKGROUND_HEIGHT = 66;


    private OpenDaycareScreenS2CPayload payload;
    private DaycareLayout layout;
    private int leftPos;
    private int topPos;
    private int selectedPartySlot = -1;
    private String failureMessage = "";
    private int failureVisibleUntil;
    private int textMouseX;
    private int textMouseY;
    private float currentFramePartial;
    private NurseryPcLayout.Frame pcFrame;
    enum Inspector { INFO, IV, EV }
    private record Controls(int selection, int unlockedMask, int price, boolean canBuy, boolean canStart, boolean canStop, boolean canClaim) {}
    private Inspector inspector = Inspector.INFO;
    private ModelWidget pcPortrait;
    private final NurseryHabitatFx habitatFx = new NurseryHabitatFx();
    private final NurseryHabitatFx.CryClock habitatCry = new NurseryHabitatFx.CryClock(new java.util.Random());
    private NurseryHabitatFx.Frame unlockFrame;
    private YardCry runningCry;
    private int unlockConfirmationYard = -1;
    private int unlockConfirmationPrice = -1;
    private static ResourceLocation pcArt(String name) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/" + name + ".png");
    }
    private static final ResourceLocation NURSERY_BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleNursery.MOD_ID, "textures/gui/nursery_base.png");
    private static final ResourceLocation TAB_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleNursery.MOD_ID, "textures/gui/nursery_hover_tab.png");
    private boolean helpOpen;
    private int helpPage;
    private long helpEggAnimationStart;
    private static final String HELP_MOTHER = "cobblemon:togekiss";
    private static final String HELP_FATHER = "cobblemon:raichu";
    private static final String HELP_CHILD = "cobblemon:togepi";
    private static final ResourceLocation HELP_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/nursery_help.png");
    private static final ResourceLocation HELP_SIDE_LEFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/nursery_help_side_left.png");
    private static final ResourceLocation HELP_SIDE_RIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/nursery_help_side_right.png");
    private static final ResourceLocation HELP_EGG_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "textures/gui/nursery_help_egg_slot.png");
    private static final ResourceLocation START_HOVER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleNursery.MOD_ID, "textures/gui/nursery_start_hover.png");
    private record Grip(boolean fromParty, int yard, int slot, UUID id, String species, String form) {}
    private Grip armed;
    private boolean pickupRecoilAttempted;
    private PokemonRenderState pickupGhost;
    private NurseryCursorInteraction.Sway pickupSway;
    private Grip pendingMove;
    private int pendingMoveUntil;
    private NurseryTransfer pendingLanding;

    private int marqueeCellX = Integer.MIN_VALUE;
    private int marqueeCellY = Integer.MIN_VALUE;
    private long marqueeStartedAt;
    private boolean showRightPanelRolls;
    private int refreshTicker;
    private HopperExportButton hopperExportButton;
    private boolean hopperExportAvailable;
    private int hopperExportMask;

    private boolean hopperExportEnabled() {
        return (hopperExportMask & (1 << payload.selectedYardIndex())) != 0;
    }

    private boolean showHopperExport() {
        var yard = selectedYard();
        return hopperExportAvailable && yard != null && yard.unlocked();
    }
    private int ticksElapsed;
    private int selectPointerOffsetY;
    private boolean selectPointerOffsetIncrement;
    private final Map<String, PokemonRenderState> renderStates = new HashMap<>();
    // Tutorial models are unrelated to server yard snapshots and retain their own animation clocks.
    private final Map<String, PokemonRenderState> helpRenderStates = new HashMap<>();
    private final Map<String, YardActorState> yardActorStates = new HashMap<>();
    private List<NurseryEggAppearance> trayEggAppearances = List.of();
    private final Map<Integer, NurseryEggAppearance> firstYardEggAppearances = new HashMap<>();

    public DaycareScreen(OpenDaycareScreenS2CPayload payload) {
        super(Component.translatable("gui.cobblenursery.pc.title"));
        this.payload = payload;
        refreshEggAppearances();
        String failure = DaycareUiText.failureOnly(payload.message(), payload.messageSuccess());
        if (pendingMove != null && (!gripIsCurrent(pendingMove) || !failure.isEmpty())) pendingMove = null;
        if (!failure.isEmpty()) {
            failureMessage = bat.cobblenursery.text.NurseryMessages.component(failure).getString();
            failureVisibleUntil = ticksElapsed + 100;
        }
    }

    public void applyPayload(OpenDaycareScreenS2CPayload payload) {
        Controls previousControls = controls();
        var oldYard = selectedYard();
        boolean endpointChanged = !this.payload.nurseryPos().equals(payload.nurseryPos());
        boolean habitatChanged = this.payload.selectedYardIndex() != payload.selectedYardIndex();
        if (endpointChanged) {
            hopperExportAvailable = false;
            hopperExportMask = 0;
        }
        this.payload = payload;
        var newYard = selectedYard();
        if (habitatChanged) { habitatFx.leave(); stopYardCry(); }
        else if (newYard != null && oldYard != null) {
            habitatFx.confirm(newYard.yardIndex(), oldYard.unlocked(), newYard.unlocked(), payload.messageSuccess(),
                    payload.message(), net.minecraft.Util.getMillis(), java.util.concurrent.ThreadLocalRandom.current());
        }
        if (habitatChanged) {
            yardActorStates.values().forEach(actor -> actor.landing = null);
            inspected = null;
            unlockConfirmationYard = -1;
        }
        if (habitatChanged || (armed != null && !gripIsCurrent(armed))) {
            clearGesture();
        }
        refreshEggAppearances();
        String failure = DaycareUiText.failureOnly(payload.message(), payload.messageSuccess());
        syncConfirmedLanding(payload, habitatChanged, !failure.isEmpty());
        if (pendingMove != null && (!gripIsCurrent(pendingMove) || !failure.isEmpty())) pendingMove = null;
        if (!failure.isEmpty()) {
            failureMessage = bat.cobblenursery.text.NurseryMessages.component(failure).getString();
            failureVisibleUntil = ticksElapsed + 100;
        }
        if (selectedPartySlot >= 0 && payload.partySlots().stream().noneMatch(slot -> slot.slotIndex() == selectedPartySlot && slot.occupied())) {
            selectedPartySlot = -1;
        }
        pruneRenderStates();
        if (minecraft != null && (endpointChanged || !previousControls.equals(controls()))) {
            rebuildWidgets();
        } else if (habitatChanged && !helpOpen && pcFrame != null) {
            rebuildWidgets();
        }
    }

    public void applyEggExportState(TerminalEggExportS2CPayload state) {
        if (!payload.nurseryPos().equals(state.nurseryPos())) return;
        boolean addOrRemove = hopperExportAvailable != state.available();
        hopperExportAvailable = state.available();
        hopperExportMask = state.available() ? state.enabledYards() : 0;
        if (addOrRemove && pcFrame != null && !helpOpen) rebuildWidgets();
        else syncHopperButton();
    }

    private void syncHopperButton() {
        if (hopperExportButton == null) return;
        hopperExportButton.visible = showHopperExport();
        hopperExportButton.active = hopperExportButton.visible;
        hopperExportButton.setMessage(translated(hopperExportEnabled() ? "hopper_disable" : "hopper_enable"));
    }

    @Override
    protected void init() {
        leftPos = topPos = 0;
        layout = new DaycareLayout(0, 0, 1);
        clearGesture();
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        pcFrame = NurseryPcLayout.fit(width, height);
        super.repositionElements();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        hopperExportButton = null;
        if (pcFrame == null) return;
        if (helpOpen) {
            if (helpPage > 0) addHelpNavigation(true);
            if (helpPage < 2) addHelpNavigation(false);
            addRenderableWidget(new NurseryPcButton(NurseryPcLayout.helpDone(), helpText("done"), false,
                    NurseryPcLayout.ButtonArt.HELP_DONE, false, () -> false, button -> setHelp(false)));
            return;
        }
        addRenderableWidget(new NurseryPcButton(NurseryPcLayout.helpButton(), Component.literal("?"), false,
                NurseryPcLayout.ButtonArt.HELP, false, () -> armed != null, button -> setHelp(true)));
        if (showHopperExport()) {
            hopperExportButton = addRenderableWidget(new HopperExportButton(NurseryPcLayout.hopperExport()));
            syncHopperButton();
        }
        var exit = NurseryPcLayout.exit();
        addRenderableWidget(new ExitButton(exit.x(), exit.y(), button -> closeFromExitButton()));
        for (DaycareYardPayload yard : payload.yards()) {
            int index = yard.yardIndex();
            DaycareLayout.Rect tab = NurseryPcLayout.tab(index);
            addRenderableWidget(new NurseryPcButton(tab, yard.unlocked() ? Component.literal(ROMAN_TABS[index]) : translated("habitat_" + (index + 1)),
                    index == payload.selectedYardIndex(), NurseryPcLayout.ButtonArt.TAB, !yard.unlocked(), () -> armed != null, button -> {
                clearGesture();
                failureVisibleUntil = 0;
                unlockConfirmationYard = -1;
                ClientPlayNetworking.send(new SelectDaycareYardC2SPayload(payload.nurseryPos(), index));
            }));
        }


        DaycareYardPayload yard = selectedYard();
        if (yard != null) mountPcActions(yard);

        // A version-dependent cosmetic control must not prevent mounting the tabs and actions.
        mountPcInfoArrow();
    }

    private void mountPcActions(DaycareYardPayload yard) {
        int index = yard.yardIndex();
        if (!yard.unlocked()) {
            boolean confirming = unlockConfirming(yard);
            addPcButton(NurseryPcLayout.action(), confirming ? "confirm" : "unlock", false,
                    priorHabitatOpen(index) && yard.canAffordUnlock(), () -> {
                        if (!unlockConfirming(yard)) {
                            unlockConfirmationYard = index;
                            unlockConfirmationPrice = yard.unlockCost();
                            rebuildWidgets();
                            return;
                        }
                        unlockConfirmationYard = -1;
                        habitatFx.request(index, net.minecraft.Util.getMillis());
                        ClientPlayNetworking.send(new UnlockDaycareYardC2SPayload(payload.nurseryPos(), index));
                        rebuildWidgets();
                    });
            return;
        }
        if (canStop(yard)) {
            addPcButton(NurseryPcLayout.action(), "stop", false, minecraft != null && ClientPlayNetworking.canSend(StopDaycareYardC2SPayload.TYPE),
                    () -> ClientPlayNetworking.send(new StopDaycareYardC2SPayload(payload.nurseryPos(), index)));
        } else {
            addPcButton(NurseryPcLayout.action(), "start", false, canStart(yard),
                    () -> ClientPlayNetworking.send(new StartDaycareYardC2SPayload(payload.nurseryPos(), index)));
        }
        addPcButton(NurseryPcLayout.eggs(), null, false, yard.eggCount() > 0,
                () -> ClientPlayNetworking.send(new ClaimDaycareEggsC2SPayload(payload.nurseryPos(), index)));
    }

    private boolean infoArrowUnavailable;

    private void mountPcInfoArrow() {
        if (infoArrowUnavailable) return;
        var arrow = NurseryPcLayout.infoArrow();
        // Kotlin's parameters after resource are nullable altResource, nullable tooltipKey,
        // and non-null label, in that order (Cobblemon 1.8.0 and 1.8.4).
        String tooltipKey = null;
        String label = "switch";
        try {
            addRenderableWidget(new IconButton(arrow.x(), arrow.y(), arrow.width(), arrow.height(),
                    pcArt("info_arrow"), null, tooltipKey, label, button -> {
                        // The arrow is outside all Pokémon hit areas, so an active yard
                        // shows the offspring when the pointer is over this control.
                        if (producingPreview(selectedYard())) {
                            inspector = offspringInspectorPage() == Inspector.INFO ? Inspector.IV : Inspector.INFO;
                        } else {
                            inspector = Inspector.values()[(inspector.ordinal() + 1) % Inspector.values().length];
                        }
                        if (minecraft != null) NurseryUiSounds.click();
                    }));
        } catch (LinkageError unavailableOnThisCobblemonBuild) {
            infoArrowUnavailable = true;
            org.slf4j.LoggerFactory.getLogger(DaycareScreen.class)
                    .warn("Cobblemon PC info arrow unavailable; the nursery actions remain active.",
                            unavailableOnThisCobblemonBuild);
        }
    }

    private boolean unlockConfirming(DaycareYardPayload yard) {
        return unlockConfirmationYard == yard.yardIndex() && unlockConfirmationPrice == yard.unlockCost()
                && priorHabitatOpen(yard.yardIndex()) && yard.canAffordUnlock();
    }

    private Controls controls() {
        DaycareYardPayload yard = selectedYard();
        int unlocked = 0;
        for (var option : payload.yards()) if (option.unlocked()) unlocked |= 1 << option.yardIndex();
        if (yard == null) return new Controls(-1, unlocked, -1, false, false, false, false);
        return new Controls(yard.yardIndex(), unlocked, yard.unlockCost(), !yard.unlocked() && priorHabitatOpen(yard.yardIndex())
                && yard.canAffordUnlock(), canStart(yard), canStop(yard), yard.eggCount() > 0);
    }

    private static boolean canStop(DaycareYardPayload yard) {
        return yard.unlocked() && ("BREEDING".equals(yard.visualState()) || "PAUSED_NO_SPACE".equals(yard.visualState()));
    }

    private static boolean canStart(DaycareYardPayload yard) {
        return NurseryPcLayout.canStart(yard.unlocked(), yard.parentA() != null, yard.parentB() != null,
                yard.compatible(), yard.visualState(), yard.eggCount(), yard.eggCapacity());
    }

    private void addPcButton(DaycareLayout.Rect rect, String key, boolean selected, boolean enabled, Runnable action) {
                NurseryPcButton button = new NurseryPcButton(rect, key == null ? Component.empty() : translated(key), selected,
                        key == null ? NurseryPcLayout.ButtonArt.NONE : NurseryPcLayout.ButtonArt.DOCK, false, () -> armed != null, ignored -> {
            failureVisibleUntil = 0;
            action.run();
        });
        button.active = enabled;
        addRenderableWidget(button);
    }

    private boolean priorHabitatOpen(int yardIndex) {
        return yardIndex <= 0 || payload.yards().stream().anyMatch(yard -> yard.yardIndex() == yardIndex - 1 && yard.unlocked());
    }

    private static Component translated(String key, Object... args) {
        return Component.translatable("gui.cobblenursery.pc." + key, args);
    }

    private void closeFromExitButton() {
        if (minecraft != null) {
            NurseryUiSounds.click();
            minecraft.setScreen(null);
            return;
        }
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;
        if (ticksElapsed >= pendingMoveUntil) { pendingMove = null; pendingLanding = null; }
        int delayFactor = 3;
        if (ticksElapsed % (2 * delayFactor) == 0) {
            selectPointerOffsetIncrement = !selectPointerOffsetIncrement;
        }
        if (ticksElapsed % delayFactor == 0) {
            selectPointerOffsetY += selectPointerOffsetIncrement ? 1 : -1;
        }

        tickYardActors();

        refreshTicker++;
        if (refreshTicker >= 20) {
            refreshTicker = 0;
            ClientPlayNetworking.send(new DaycareRefreshC2SPayload(payload.nurseryPos()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pcFrame == null) return false;
        double x = pcFrame.localX(mouseX), y = pcFrame.localY(mouseY);
        if (helpOpen) {
            super.mouseClicked(x, y, button);
            return true;
        }
        var route = NurseryPcLayout.clickRoute(button, armed != null);
        if (route == NurseryPcLayout.ClickRoute.CARRY) {
            NurseryTransfer move = moveFor(armed, x, y);
            switch (NurseryCursorInteraction.decide(true, false, move != null)) {
                case PLACE -> {
                    pendingMove = armed;
                    pendingLanding = move.toHabitat() ? move : null;
                    pendingMoveUntil = ticksElapsed + 40;
                    failureVisibleUntil = 0;
                    ClientPlayNetworking.send(new MoveNurseryPokemonC2SPayload(payload.nurseryPos(), move));
                }
                case CANCEL -> { /* Return visually; no other widget action or packet. */ }
                default -> { }
            }
            clearGesture();
            return true;
        }
        if (super.mouseClicked(x, y, button)) return true;
        if (route == NurseryPcLayout.ClickRoute.OTHER || pendingMove != null) return false;

        Grip source = gripAt(x, y);
        if (NurseryCursorInteraction.decide(false, source != null, false) != NurseryCursorInteraction.Click.PICK) return false;
        armed = source;
        if (!producingPreview(selectedYard())) inspected = source;
        selectedPartySlot = source.fromParty() ? source.slot() : -1;
        pickupGhost = pickupRenderState(source);
        pickupSway = new NurseryCursorInteraction.Sway();
        pickupSway.sample(x, y, net.minecraft.Util.getMillis());
        pickupRecoilAttempted = false;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Release never changes the held Pokemon; only the next click can place it.
        return pcFrame != null && super.mouseReleased(pcFrame.localX(mouseX), pcFrame.localY(mouseY), button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (helpOpen && keyCode == 256) {
            setHelp(false);
            return true;
        }
        if (keyCode == 256 && armed != null) {
            clearGesture();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Grip gripAt(double x, double y) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null || !yard.unlocked()) return null;
        int partySlot = NurseryPcLayout.partyTarget(x, y);
        DaycarePartyPokemonPayload party = partySlot < 0 ? null : partySlot(partySlot);
        if (party != null && party.occupied() && party.pokemonId() != null) {
            return new Grip(true, yard.yardIndex(), partySlot, party.pokemonId(), party.speciesId(), party.formId());
        }

        if (!NurseryPcLayout.stage().contains(x, y)) return null;
        double closest = Double.POSITIVE_INFINITY;
        Grip chosen = null;
        for (int side = 0; side < 2; side++) {
            boolean right = side == 1;
            var parent = right ? yard.parentB() : yard.parentA();
            if (parent == null || parent.pokemonId() == null) continue;
            YardActorState actor = getOrCreateYardActorState(yard, right, parent);
            var actual = actor.projected;
            var fallback = actual == null ? stageActorBox(yard, parent, actor, currentFramePartial) : null;
            double center = actual == null ? fallback.projectedX() : (actual.x0()+actual.x1())*.5;
            double distance = Math.abs(x - center);
            boolean within = actual == null ? fallback.hit(x,y) : x >= actual.x0()-2 && x <= actual.x1()+2
                    && y >= actual.y0()-2 && y <= actual.y1()+2;
            if (within && distance < closest) {
                chosen = new Grip(false, yard.yardIndex(), side, parent.pokemonId(), parent.speciesId(), parent.formId());
                closest = distance;
            }
        }
        return chosen;
    }

    private PokemonRenderState pickupRenderState(Grip source) {
        RenderablePokemon visual = null;
        if (source.fromParty()) {
            Pokemon pokemon = localPartyPokemon(source.id());
            if (pokemon != null) visual = pokemon.asRenderablePokemon();
        } else {
            var yard = selectedYard();
            var parent = source.slot() == 0 ? yard.parentA() : yard.parentB();
            if (parent != null && parent.preview() != null) visual = parentRenderable(parent, parent.preview());
        }
        return visual == null ? createRenderState(source.species(), source.form())
                : new PokemonRenderState(source.species(), source.form(), visual, new FloatingState());
    }

    private boolean gripIsCurrent(Grip grip) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null || yard.yardIndex() != grip.yard() || !yard.unlocked()) return false;
        if (grip.fromParty()) {
            var slot = partySlot(grip.slot());
            return slot != null && slot.occupied() && grip.id().equals(slot.pokemonId());
        }
        var parent = grip.slot() == 0 ? yard.parentA() : yard.parentB();
        return parent != null && grip.id().equals(parent.pokemonId());
    }

    private DaycarePartyPokemonPayload partySlot(int index) {
        for (var party : payload.partySlots()) if (party.slotIndex() == index) return party;
        return null;
    }

    private NurseryTransfer moveFor(Grip source, double x, double y) {
        if (!gripIsCurrent(source)) return null;
        if (source.fromParty()) {
            var yard = selectedYard();
            int vacancy = NurseryPcLayout.habitatTarget(x, y, yard.unlocked(),
                    yard.parentA() != null, yard.parentB() != null);
            return vacancy < 0 ? null : new NurseryTransfer(true, source.yard(), source.slot(), vacancy == 1, source.id());
        }
        int partyIndex = NurseryPcLayout.partyTarget(x, y);
        var target = partyIndex < 0 ? null : partySlot(partyIndex);
        return target == null || target.occupied() ? null
                : new NurseryTransfer(false, source.yard(), partyIndex, source.slot() == 1, source.id());
    }

    private void clearGesture() {
        armed = null;
        pickupGhost = null;
        pickupSway = null;
        selectedPartySlot = -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (pcFrame == null) return;
        int x = (int) Math.floor(pcFrame.localX(mouseX)), y = (int) Math.floor(pcFrame.localY(mouseY));
        currentFramePartial = partialTick;
        textMouseX = x;
        textMouseY = y;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(pcFrame.left(), pcFrame.top(), 0);
            float scale = (float) pcFrame.scale();
            graphics.pose().scale(scale, scale, 1.0F);
            if (helpOpen) drawHelpPage(graphics, partialTick);
            else drawPcBody(graphics, x, y, partialTick);
            super.render(graphics, x, y, partialTick);
            if (!helpOpen) drawDetachedLock(graphics);
            if (!helpOpen && armed != null) drawPcCursor(graphics, pcFrame.localX(mouseX), pcFrame.localY(mouseY), partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private static final String[] ROMAN_TABS = {"I","II","III","IV"};
    private static final int PC_LINE = 0xFF77909D;
    private static final int PC_MINT = 0xFFA8DEC2;
    private static final int PC_CORAL = 0xFFDC9481;
    private static final int PC_TEXT = 0xFFF0F4F0;
    private static final int PC_SOFT = 0xFFAFBDC5;

    private void setHelp(boolean open) {
        if (helpOpen == open) return;
        helpOpen = open;
        helpPage = 0;
        if (open) {
            helpEggAnimationStart = net.minecraft.Util.getMillis();
            stopYardCry();
        }
        setFocused(null);
        rebuildWidgets();
    }

    private static Component helpText(String key) {
        String id = "gui.cobblenursery.help." + key;
        return switch (key) {
            case "caption_1" -> Component.translatable(id, helpKeyword("keyword_egg_group"));
            case "attributes_caption_2" -> Component.translatable(id, helpKeyword("keyword_iv"));
            case "attributes_caption_3" -> Component.translatable(id,
                    helpKeyword("keyword_minimum_iv"), helpKeyword("example_text"));
            case "inheritance_caption_1" -> Component.translatable(id, helpKeyword("keyword_power_items"));
            default -> Component.translatable(id);
        };
    }

    private static Component helpKeyword(String key) {
        return Component.translatable("gui.cobblenursery.help." + key).withStyle(ChatFormatting.BOLD);
    }

    private static void drawStartFace(GuiGraphics g, int x, int y, boolean hover) {
        var source = NurseryPcLayout.startHover();
        if (hover) {
            g.blit(START_HOVER_TEXTURE, x, y, 0, 0, source.width(), source.height(), source.width(), source.height());
        } else {
            g.blit(NURSERY_BASE_TEXTURE, x, y, source.x(), source.y(), source.width(), source.height(),
                    NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT);
        }
    }

    private void drawHelpPage(GuiGraphics g, float partialTick) {
        var wallpaper = com.cobblemon.mod.common.client.render.gui.PCBoxWallpaperRepository.INSTANCE.getDefaultWallpaper();
        var glow=helpWallpaperLayer(wallpaper,"glow/");
        var outline=pcArt("pc_screen_overlay");
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        try {
            for (int i=0; i<3; i++) {
                var card = NurseryPcLayout.helpCard(i);
                var artwork = helpPage == 0 && i == 0 ? YARD_WALLPAPER_TEXTURES[0] : wallpaper;
                // Fill the whole opening with a centered crop: no dark inset or distorted trees.
                int sourceHeight = Math.round(174F * card.height() / card.width());
                g.blit(artwork, card.x(), card.y(), card.width(), card.height(),
                        0F, (155-sourceHeight)/2F, 174, sourceHeight, 174, 155);
                g.blit(outline, card.x(), card.y(), card.width(), card.height(), 0F, 0F, 174, 155, 174, 155);
            }
        } finally { com.mojang.blaze3d.systems.RenderSystem.disableBlend(); }
        g.blit(HELP_TEXTURE, 0, 0, 0, 0, NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT,
                NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT);

        // The atlas has 17 pixels of falloff outside a transparent 174 x 155 opening.
        // Line up that opening with the wallpaper and draw the falloff over the frame.
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        try {
            for (int i = 0; i < 3; i++) {
                var card = NurseryPcLayout.helpCard(i);
                int margin = 7;
                clipPc(g, card.x() - margin, card.y() - margin,
                        card.x() + card.width() + margin, card.y() + card.height() + margin);
                g.pose().pushPose();
                try {
                    g.pose().translate(card.x(), card.y(), 0);
                    g.pose().scale(card.width() / 174F, card.height() / 155F, 1F);
                    g.blit(glow, -17, -17, 0, 0, 208, 189, 208, 189);
                } finally {
                    g.pose().popPose();
                    g.disableScissor();
                }
            }
        } finally { com.mojang.blaze3d.systems.RenderSystem.disableBlend(); }

        // Authored side trim goes above the unchanged base, at native pixel size.
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        try {
            g.blit(HELP_SIDE_LEFT_TEXTURE, 0, 56, 0, 0, 13, 93, 13, 93);
            g.blit(HELP_SIDE_RIGHT_TEXTURE, NurseryPcLayout.WIDTH - 13, 56, 0, 0, 13, 93, 13, 93);
        } finally { com.mojang.blaze3d.systems.RenderSystem.disableBlend(); }

        // Mirror only the left spacer: reverse the U coordinates without changing its bounds.
        g.blit(pcArt("pc_spacer_right"), 78, 179, 32, 12, 64F, 0F, -64, 24, 64, 24);
        blitNativeHalf(g, pcArt("pc_spacer_right"), 238, 179, 64, 24);
        for (int step = 0; step < 3; step++) {
            var card = NurseryPcLayout.helpCard(step);
            int x = card.x(), y = card.y(), center = x + card.width()/2;
            // Render the original art over the wallpapers; only the scene objects are live native renders.
            clipPc(g, x+5, y+4, x+card.width()-5, y+card.height()-4);
            try {
                g.flush();
                if (helpPage == 1) drawAttributeCard(g,step,x,y); else if (helpPage == 2) drawInheritanceCard(g,step,x,y,partialTick); else if (step == 0) {
                    renderHelpAdult(g, "help:pair_left", HELP_MOTHER, "female", x+9,y,x+47,13,42,21,partialTick);
                    renderHelpAdult(g, "help:pair_right", HELP_FATHER, "male", x+53,y,x+92,-15,42,21,partialTick);
                    g.flush();
                    drawHelpParentTag(g, x+28, y+6, true);
                    drawHelpParentTag(g, x+73, y+6, false);
                } else if (step == 1) {
                    drawHelpTerminal(g,x,y);
                } else {
                    renderHelpNestScene(g,x+32,y+44);
                    g.flush();
                    drawHelpHeadline(g,helpText("nest_timer"),x+31,y+6,45,.9F,0xFFFFFFFF);
                    drawHelpArrow(g,x+60,y+26);
                    renderHelpAdult(g, "help:hatch", HELP_CHILD, "normal", x+65,y,x+94,-15,42,29,partialTick);
                }
                g.flush();
            } finally { g.disableScissor(); }
            drawHelpCardTitle(g, helpHeading((helpPage == 1 ? "attributes_step_" : helpPage == 2 ? "inheritance_step_" : "step_") + (step+1)), center, card.width()-10);
            drawHelpCaption(g, helpText((helpPage == 1 ? "attributes_caption_" : helpPage == 2 ? "inheritance_caption_" : "caption_") + (step+1)), x+5, helpPage == 0 && step == 0 ? 119 : 122, card.width()-10);
        }

        drawHelpTitle(g, helpHeading(helpPage == 1 ? "attributes_title" : helpPage == 2 ? "inheritance_title" : "title"), NurseryPcLayout.WIDTH/2, NurseryPcLayout.HELP_TITLE_TOP, 278, 1.45F);
    }

    private Component helpHeading(String key) {
        return Component.literal(helpText(key).getString().toUpperCase(java.util.Locale.ROOT));
    }

    private void drawHelpCardTitle(GuiGraphics g, Component title, int center, int maxWidth) {
        drawHelpTitle(g, title, center, 109, maxWidth, 1F);
    }

    private void drawHelpTitle(GuiGraphics g, Component title, int center, int y, int maxWidth, float preferredScale) {
        var styled = title.copy().withStyle(style -> style.withFont(nativeLargeFont()).withBold(true));
        int width = font.width(styled);
        // Preserve the PC glyphs, with a half GUI pixel of extra weight for the uppercase headings.
        float titleWeight = 0.5F;
        float scale = Math.min(preferredScale, (maxWidth - titleWeight) / Math.max(1, width));
        var pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(Math.round(center - (width * scale - titleWeight) / 2F), y, 0);
            pose.scale(scale, scale, 1F);
            g.drawString(font, styled, 0, 0, 0xFFFFFFFF, true);
            pose.translate(-titleWeight / scale, 0, 0);
            g.drawString(font, styled, 0, 0, 0x66FFFFFF, false);
        } finally {
            pose.popPose();
        }
    }


    private void drawHelpParentTag(GuiGraphics g, int cx, int y, boolean female) {
        g.fill(cx - 21, y, cx + 21, y + 14, 0x80000000);
        var symbol = Component.literal(female ? "\u2640" : "\u2642")
                .withStyle(style -> style.withColor(female ? 0xFC5454 : 0x32CBFF));
        var name = Component.translatable(female ? "cobblemon.species.togekiss.name" : "cobblemon.species.raichu.name");
        drawHelpHeadline(g, Component.empty().append(symbol).append(" ").append(name),
                cx, y + 1, 39, .8F, 0xFFFFFFFF);
        var shared = Component.translatable("cobblemon.egg_group.fairy")
                .withStyle(style -> style.withColor(PC_MINT & 0xFFFFFF));
        var groups = Component.translatable(female ? "cobblemon.egg_group.flying" : "cobblemon.egg_group.field")
                .append(" \u00B7 ").append(shared);
        drawHelpHeadline(g, groups, cx, y + 8, 40, .8F, 0xFFFFFFFF);
    }

    private void drawHelpTerminal(GuiGraphics g, int x, int y) {
        drawHelpItem(g, new ItemStack(NurseryRegistry.NURSERY_TERMINAL_ITEM), x + 6, y + 3, 2.55F);
        g.flush();
        drawHelpArrow(g, x + 56, y + 25);
        drawHelpItem(g, helpEgg(), x + 63, y + 13, 1.55F);
        g.flush();
    }


    private void drawHelpArrow(GuiGraphics g, int x, int y) {
        drawHelpHeadline(g,Component.literal("\u2192"),x,y,17,1.5F,0xFFFFFFFF);
    }

    private static ResourceLocation helpWallpaperLayer(ResourceLocation wallpaper,String directory) {
        String path=wallpaper.getPath();
        int slash=path.lastIndexOf('/')+1;
        return wallpaper.withPath(path.substring(0,slash)+directory+path.substring(slash));
    }

    private void changeHelpPage(int direction) {
        if (!helpOpen) return;
        helpPage = Math.clamp(helpPage + direction,0,2);
        if (helpPage == 0) helpEggAnimationStart = net.minecraft.Util.getMillis();
        setFocused(null);
        rebuildWidgets();
    }

    private void addHelpNavigation(boolean back) {
        addRenderableWidget(new NurseryPcButton(NurseryPcLayout.helpNavigation(back),
                helpText(back ? "previous" : "next"), false, NurseryPcLayout.ButtonArt.HELP_NAV,
                false, () -> false, button -> changeHelpPage(back ? -1 : 1)));
    }

    private static final Item[] HELP_ATTR_ICONS = {Items.APPLE, Items.IRON_SWORD, Items.SHIELD,
            Items.BLAZE_POWDER, Items.ENCHANTED_BOOK, Items.FEATHER};

    private void drawAttributeCard(GuiGraphics g,int step,int x,int y) {
        if (step == 0) {
            for (int i=0;i<HELP_ATTR_ICONS.length;i++) {
                int cx=x+21+(i%3)*30, top=y+5+(i/3)*23;
                drawHelpItem(g,new ItemStack(HELP_ATTR_ICONS[i]),cx-6,top,.75F);
                drawHelpHeadline(g,helpText("attribute_"+i),cx,top+12,26,.88F,PC_TEXT);
            }
        } else if(step == 1) {
            drawHelpPowerItem(g,"loaded_dice",x+21,y+7,1F);
            drawHelpArrow(g,x+50,y+9);
            renderHelpAdult(g,"help:iv_offspring",HELP_CHILD,"normal",x+65,y-5,x+94,8,36,26,0);
            for (int i=0;i<8;i++) {
                int bx=x+24+i*7;
                g.fill(bx,y+40-i*2,bx+5,y+43,i<4 ? PC_SOFT : PC_MINT);
            }
            drawHelpHeadline(g,Component.literal("0"),x+14,y+41,10,1F,PC_TEXT);
            drawHelpHeadline(g,Component.literal("31"),x+86,y+41,17,1F,PC_MINT);
        } else {
            drawHelpHeadline(g, helpText("example"), x+50, y+3, 90, .85F, PC_TEXT);
            g.blit(pcArt("info_box_stats"),x+19,y+13,0,0,63,21,63,69);
            drawHelpHeadline(g,helpText("attributes_minimum"),x+50,y+14,62,1F,PC_MINT);
            drawHelpHeadline(g,helpText("attributes_hp_example"),x+50,y+24,60,1F,PC_TEXT);
            int start=x+10, width=80, minimum=start+width*23/32;
            g.fill(start,y+36,minimum,y+40,PC_SOFT);
            g.fill(minimum,y+36,start+width,y+40,PC_MINT);
            drawHelpHeadline(g,Component.literal("0"),start+2,y+43,12,1F,PC_SOFT);
            drawHelpHeadline(g,Component.literal("23–31"),x+77,y+43,42,1F,PC_MINT);
        }
    }

    private void drawInheritanceCard(GuiGraphics g, int step, int x, int y, float partialTick) {
        drawHelpHeadline(g, helpText("example"), x+50, y+5, 90, .85F, PC_TEXT);
        if (step == 1) {
            renderHelpAdult(g,"help:roll_a",HELP_CHILD,"normal",x+5,y,x+46,12,37,24,partialTick);
            renderHelpAdult(g,"help:roll_b",HELP_CHILD,"normal",x+55,y,x+96,-12,37,24,partialTick);
            drawHelpArrow(g,x+51,y+21);
            drawHelpHeadline(g,helpText("inheritance_example_from"),x+26,y+38,40,.84F,PC_SOFT);
            drawHelpHeadline(g,helpText("inheritance_example_to"),x+75,y+38,40,.84F,PC_MINT);
            drawHelpHeadline(g,helpText("inheritance_keep"),x+26,y+46,36,1F,PC_SOFT);
            drawHelpHeadline(g,helpText("inheritance_better"),x+75,y+46,36,1F,PC_MINT);
            return;
        }
        boolean improved = step == 2;
        if (improved) {
            renderHelpAdult(g,"help:improved",HELP_CHILD,"normal",x+5,y,x+47,10,41,29,partialTick);
        } else {
            renderHelpAdult(g,"help:inheritance_parent",HELP_MOTHER,"female",
                    x+8,y,x+43,13,39,27,partialTick);
        }
        g.flush();
        drawHelpPowerItem(g,"power_weight",x+33,y+24,.95F);
        drawHelpArrow(g,x+56,y+21);
        renderHelpAdult(g,"help:next_offspring",HELP_CHILD,"normal",x+65,y,x+97,-10,41,29,partialTick);
        drawHelpHeadline(g,helpText(improved ? "inheritance_better" : "inheritance_parent_value"),
                x+27,y+44,39,1F,PC_MINT);
        drawHelpHeadline(g,helpText(improved ? "inheritance_new_minimum" : "inheritance_child_value"),
                x+78,y+44,39,1F,PC_MINT);
    }

    private static void drawHelpPowerItem(GuiGraphics g, String name, float x, float y, float scale) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("cobblemon",name))
                .ifPresent(item -> drawHelpItem(g,new ItemStack(item),x,y,scale));
    }

    private static final ResourceLocation HELP_FONT = ResourceLocation.withDefaultNamespace("uniform");
    private void drawHelpCaption(GuiGraphics g, Component text, int x, int y, int width) {
        var styled = text.copy().withStyle(st -> st.withFont(HELP_FONT));
        float scale = 1F;
        var lines = font.split(styled, width);
        while (lines.size() * (font.lineHeight + 1) * scale > 38F && scale > .76F) {
            scale -= .05F;
            lines = font.split(styled, (int)(width / scale));
        }
        float gap = (font.lineHeight + 1) * scale;
        for(int i=0;i<lines.size();i++) {
            g.pose().pushPose();
            try {
                g.pose().translate(NurseryPcLayout.helpCaptionLeft(x,width,Math.round(font.width(lines.get(i))*scale)),y+i*gap,0);
                g.pose().scale(scale,scale,1);
                g.drawString(font,lines.get(i),0,0,0xFFFFFFFF,true);
            } finally {g.pose().popPose();}
        }
    }
    private void drawHelpHeadline(GuiGraphics g, Component text, int cx, int y, int maxWidth,float preferred,int color) {
        var styled=text.copy().withStyle(st->st.withFont(HELP_FONT).withBold(true));
        int width=font.width(styled);
        float scale=Math.min(preferred,maxWidth/(float)Math.max(1,width));
        g.pose().pushPose();
        try {
            g.pose().translate(cx-width*scale/2,y,0);
            g.pose().scale(scale,scale,1);
            g.drawString(font,styled,0,0,color,true);
        } finally {g.pose().popPose();}
    }
    private void renderHelpAdult(GuiGraphics g, String key, String species, String sex,
                                 int left, int top, int right, float yaw, float partialTick) {
        renderHelpAdult(g,key,species,sex,left,top,right,yaw,50,42,partialTick);
    }

    private void renderHelpAdult(GuiGraphics g, String key, String species, String sex,
                                 int left, int top, int right, float yaw, int floor, int maxHeight, float partialTick) {
        prepareHelpPokemon(key,species,sex);
        double center=(left+right)/2.0;
        // Same live projected-geometry fitter as the habitat, including wings and the current pose.
        var fit=new NurseryGhostProjection.Stage(pcFrame.screenX(center),pcFrame.screenY(top+floor),
                pcFrame.screenX(left),pcFrame.screenX(right),maxHeight*pcFrame.scale(),pcFrame.screenY(top+7),
                pcFrame.scale(),net.minecraft.Util.getMillis(),null,ignored -> {});
        NurseryGhostProjection.fitStage(fit,()->renderPokemonModel(g,key,species,"normal",
                center,top+8,.62F,yaw,4,partialTick));
    }


    private void prepareHelpPokemon(String key,String id,String aspect) {
        helpRenderStates.computeIfAbsent(key, ignored -> {
            Species species=PokemonSpecies.getByIdentifier(ResourceLocation.parse(id));
            return species==null ? null : new PokemonRenderState(id,"normal",
                new RenderablePokemon(species,java.util.Set.of(aspect),ItemStack.EMPTY),new FloatingState());
        });
    }

    private static void drawHelpDone(GuiGraphics g, Component label, boolean hovered) {
        if (hovered) {
            var sprite=NurseryPcLayout.startHover();
            var face=NurseryPcLayout.helpDoneFace();
            g.blit(START_HOVER_TEXTURE,face.x(),face.y(),1,1,face.width(),face.height(),sprite.width(),sprite.height());
        }
        Font f=Minecraft.getInstance().font;
        var text=label.copy().withStyle(st->st.withFont(HELP_FONT).withBold(true));
        var pos=NurseryPcLayout.helpDoneText(f.width(text),f.lineHeight);
        g.pose().pushPose();
        try {
            g.pose().translate(pos.x(),pos.y(),0);
            g.drawString(f,text,0,0,PC_TEXT,true);
        } finally {g.pose().popPose();}
    }


    private static ItemStack helpEgg() {
        var egg = new ItemStack(bat.cobblenursery.registry.NurseryRegistry.NURSERY_EGG_ITEM);
        egg.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(NurseryEggAppearanceResolver.resolve(HELP_CHILD, "normal").modelData()));
        return egg;
    }

    private void renderHelpNestScene(GuiGraphics g,int x,int y) {
        var client=Minecraft.getInstance();
        var stack=g.pose();
        var buffers=g.bufferSource();
        g.flush();
        stack.pushPose();
        try {
            stack.translate(x,y,170);
            // Same physical mesh units and camera for both items, so the actual front lip depth-tests the egg.
            stack.scale(40,-40,40);
            stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(15));
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            stack.pushPose();
            try {
                stack.translate(0,.5,0);
                client.getItemRenderer().renderStatic(new ItemStack(bat.cobblenursery.registry.NurseryRegistry.INCUBATION_NEST_ITEM),
                        net.minecraft.world.item.ItemDisplayContext.NONE,0x00F000F0,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,stack,buffers,client.level,0);
            } finally { stack.popPose(); }
            drawHelpHatchingEgg(g);
            buffers.endBatch();
        } finally { stack.popPose(); }
    }

    private void drawHelpHatchingEgg(GuiGraphics g) {
        float roll=(float)NurseryPcLayout.helpEggRoll(net.minecraft.Util.getMillis()-helpEggAnimationStart);
        var client=Minecraft.getInstance();
        var stack=g.pose();
        // Same native nesting transform and sole pivot as the world renderer, but no world/timer state.
        float scale=.53F;
        stack.pushPose();
        try {
            stack.translate(0,1.9/16,0);
            stack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll));
            stack.translate(0,(.5-1.0/16)*scale,0);
            stack.scale(scale,scale,scale);
            client.getItemRenderer().renderStatic(helpEgg(),net.minecraft.world.item.ItemDisplayContext.NONE,
                    0x00F000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    stack,g.bufferSource(),client.level,0);
        } finally { stack.popPose(); }
    }

    private static void drawHelpItem(GuiGraphics g, ItemStack item, float x, float y, float scale) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1);
            g.renderItem(item,0,0);
        } finally { g.pose().popPose(); }
    }


    private void drawPcBody(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.blit(NURSERY_BASE_TEXTURE, 0, 0, 0, 0, NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT,
                NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT);
        blitNativeHalf(g, pcArt("pc_spacer_right"), 275.5, 184, 64, 24);

        DaycareYardPayload yard = selectedYard();
        if (yard == null) return;
        unlockFrame = sampleUnlockFrame(yard.yardIndex(), net.minecraft.Util.getMillis());
        drawPcStage(g, yard, partialTick);
        drawPcStatus(g, yard);
        drawPcParty(g, mouseX, mouseY, partialTick);
        drawPcInspector(g, yard, mouseX, mouseY, partialTick);

        if (yard.eggCount() > 0) {
            var slot = NurseryPcLayout.eggs();
            renderCenteredYardEgg(g, slot, firstYardEggAppearances.getOrDefault(yard.yardIndex(), NurseryEggAppearance.NORMAL));
            String count = Integer.toString(yard.eggCount());
            drawNativePcText(g, NurseryPcNativeText.nurseryStatus(slot.x() + slot.width() - 3 - font.width(count) * .5,
                    slot.y() + slot.height() - 7, 0xFFFFFFFF), Component.literal(count));
            if (armed == null && NurseryPcLayout.buttonHit(slot, mouseX, mouseY)) drawPcPartySlotPointer(g, slot);
        }
    }

    private void clipPc(GuiGraphics g, int x, int y, int right, int bottom) {
        g.enableScissor((int) Math.floor(pcFrame.screenX(x)), (int) Math.floor(pcFrame.screenY(y)),
                (int) Math.ceil(pcFrame.screenX(right)), (int) Math.ceil(pcFrame.screenY(bottom)));
    }

    private void drawPcStage(GuiGraphics g, DaycareYardPayload yard, float partialTick) {
        DaycareLayout.Rect r = NurseryPcLayout.stage();

        clipPc(g, r.x(), r.y(), r.x() + r.width(), r.y() + r.height());
        try {
            // Sample a landscape band rather than vertically squashing the PC wallpaper.
            g.pose().pushPose();
            g.pose().translate(r.x(), r.y(), 0);
            g.pose().scale(r.width() / 174.0F, 1F, 1F);
            g.blit(YARD_WALLPAPER_TEXTURES[yard.yardIndex()], 0, 0, 0, 40,
                    174, r.height(), BIOME_WALLPAPER_WIDTH, BIOME_WALLPAPER_HEIGHT);
            g.pose().popPose();
            if (!yard.unlocked() || unlockFrame != null) {
                int alpha = !yard.unlocked() ? 0xD2 : (int)Math.round(0xD2*(1-unlockFrame.landscape()));
                if (alpha != 0) g.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), alpha << 24 | 0x14212B);
            }

            if (!yard.unlocked()) {
                String titleKey = "locked_" + (yard.yardIndex() + 1);
                var title = translated(titleKey);
                var price = translated("price", yard.unlockCost());
                int titleWidth = font.width(title.copy().withStyle(style -> style.withFont(nativeLargeFont()).withBold(true)));
                int priceWidth = font.width(price.copy().withStyle(style -> style.withFont(nativeLargeFont()).withBold(true)));
                for (var line : NurseryPcNativeText.lockedHabitat(r, titleKey, titleWidth, priceWidth, font.lineHeight, PC_MINT)) {
                    drawNativePcText(g, line, line.key().equals(titleKey) ? title : price);
                }
                return;
            }

            NurseryPcLayout.drawActorLayer(g::flush, () -> renderYardPokemon(g, yard, partialTick));
        } finally {
            g.disableScissor();
            for (int x : new int[]{r.x(), r.x() + r.width() - 1})
                for (int y : new int[]{r.y(), r.y() + r.height() - 1})
                    g.blit(NURSERY_BASE_TEXTURE, x, y, x, y, 1, 1, NurseryPcLayout.WIDTH, NurseryPcLayout.HEIGHT);
        }
    }

    private void drawPcStatus(GuiGraphics g, DaycareYardPayload yard) {
        DaycareLayout.Rect r = NurseryPcLayout.status();

        int textWidth = 164;
        if (ticksElapsed < failureVisibleUntil) {
            drawCenteredPcStatus(g, failureMessage, r, r.height(), PC_CORAL);
            return;
        }
        String key;
        if (!yard.unlocked()) {
            key = !priorHabitatOpen(yard.yardIndex()) ? "prior" : !yard.canAffordUnlock() ? "funds" : unlockConfirming(yard) ? "confirm_unlock" : "unlock_available";
        } else if (yard.eggCount() >= yard.eggCapacity() && !"BREEDING".equals(yard.visualState())) {
            key = "full";
        } else {
            key = switch (yard.visualState()) {
                case "PAUSED_NO_SPACE" -> "full";
                case "BREEDING" -> "breeding";
                case "READY_TO_START" -> "ready";
                case "BLOCKED" -> "incompatible";
                default -> yard.parentA() == null && yard.parentB() == null ? "empty" : "needs_parent";
            };
        }
        String status = "incompatible".equals(key) && !yard.compatibilityReason().isBlank()
                ? prettyReason(yard.compatibilityReason()) : translated(key).getString();
        boolean running = "BREEDING".equals(yard.visualState());
        drawCenteredPcStatus(g, status, r, running ? 16 : r.height(),
                "funds".equals(key) || "prior".equals(key) ? PC_CORAL : PC_TEXT);
        if ("BREEDING".equals(yard.visualState())) {
            int x = r.x() + 5, y = r.y() + 17;
            g.fill(x, y, x + textWidth, y + 2, 0xFF0F1D26);
            g.fill(x, y, x + Math.round(textWidth * Math.clamp(yard.progress(), 0.0F, 1.0F)), y + 2, PC_MINT);
        }
    }

    private void drawCenteredPcStatus(GuiGraphics g, String value, DaycareLayout.Rect well, int height, int color) {
        var text = Component.literal(value);
        int usable = well.width() - 12;
        int maxLines = height >= 20 ? 2 : 1;
        var lines = font.split(text, usable);
        double scale = 1;
        if (lines.size() > maxLines) {
            // Wrap at native size first. Only exceptional long diagnostics may use a uniform scale down.
            for (double candidate : new double[]{.9, .8, .72}) {
                scale = candidate;
                lines = font.split(text, (int) (usable / scale));
                if (lines.size() <= maxLines) break;
            }
        }
        int count = Math.min(maxLines, lines.size());
        double lineHeight = font.lineHeight * scale;
        double top = well.y() + (height - count * lineHeight) / 2.0;
        for (int i=0;i<count;i++) {
            var line = lines.get(i);
            int width = font.width(line);
            var place = NurseryPcLayout.centerText(well.x()+6,top+i*lineHeight,usable,lineHeight,width,font.lineHeight,scale);
            var pose = g.pose();
            pose.pushPose();
            try {
                // Keep pixel glyph proportions; coordinates are centered by their actual measured advances.
                pose.translate(place.x(), place.y(), 0);
                pose.scale((float)place.scale(), (float)place.scale(), 1F);
                RenderHelperKt.drawScaledText(g, line, 0, 0, 1F, 1F, 1F, color, false, true);
            } finally { pose.popPose(); }
        }
    }

    private void drawPcWrapped(GuiGraphics g, String text, int x, int y, int width, int maxLines, int color) {
        // Status text has no direct equivalent in the stock PC. Use stock compact typography,
        // with native wrapping rather than scaling or painting an unrelated font.
        var lines = font.split(Component.literal(text), width * 2);
        float lineY = y;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            var style = NurseryPcNativeText.nurseryStatus(x, lineY, color);
            RenderHelperKt.drawScaledText(g, lines.get(i), style.x(), style.y(), style.scale(), 1F, 1F,
                    style.color(), style.centered(), style.shadow());
            lineY += 8;
        }
    }

    private void drawPcParty(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        Grip held = armed;
        for (int i = 0; i < 6; i++) {
            DaycareLayout.Rect r = NurseryPcLayout.partySlot(i);
            var slot = partySlot(i);
            boolean source = held != null && held.fromParty() && held.slot() == i;
            if (slot != null && slot.occupied()) {
                if (!source) drawPcPartySlot(g, slot, partialTick);
                if (!source && NurseryPcLayout.buttonHit(r, mouseX, mouseY)) drawPcPartySlotPointer(g, r);
            }
        }

    }

    private Object inspectorSubject(DaycareYardPayload yard, int x, int y, java.util.function.Predicate<Grip> displayable) {
        boolean producing = producingPreview(yard);
        Grip heldFocus = !producing && armed != null ? inspectionGrip(yard, armed.id()) : null;
        Grip last = !producing && inspected != null ? inspectionGrip(yard, inspected.id()) : null;
        Grip hover = null;
        if (heldFocus == null) {
            int slot = NurseryPcLayout.partyTarget(x, y);
            var party = slot < 0 ? null : partySlot(slot);
            hover = slot >= 0 ? (party != null && party.occupied() ? inspectionGrip(yard, party.pokemonId()) : null)
                    : gripAt(x, y);
            if (producing && hover != null && ((armed != null && armed.id().equals(hover.id())) || !displayable.test(hover))) {
                hover = null;
            }
        }
        Object subject = NurseryInspectorChoice.resolve(producing, (Object) yard.eggPreview(), heldFocus, hover, last);
        if (!producing && subject instanceof Grip source) inspected = source;
        return subject;
    }

    private boolean inspectorCanShow(DaycareYardPayload yard, Grip source) {
        if (source.fromParty()) return localPartyPokemon(source.id()) != null;
        var parent = source.slot() == 0 ? yard.parentA() : yard.parentB();
        return parent != null && parent.preview() != null && parentRenderable(parent, parent.preview()) != null;
    }

    private void drawPcInspector(GuiGraphics g, DaycareYardPayload yard, int mouseX, int mouseY, float partialTick) {
        Object subject = inspectorSubject(yard, mouseX, mouseY, source -> inspectorCanShow(yard, source));
        if (subject instanceof DaycareEggPreviewPayload offspring) {
            var species = ResourceLocation.tryParse(offspring.childSpeciesId());
            EggGroupTab.draw(g, 1, NurseryPcLayout.HEIGHT - 1, species == null ? null : PokemonSpecies.getByIdentifier(species));
            drawOffspringInspector(g, yard, offspring, mouseX, mouseY, partialTick);
            return;
        }
        if (!(subject instanceof Grip source)) {
            EggGroupTab.draw(g, 1, NurseryPcLayout.HEIGHT - 1, null);
            return;
        }
        Pokemon party = source.fromParty() ? localPartyPokemon(source.id()) : null;
        var parent = source.fromParty() ? null : source.slot() == 0 ? yard.parentA() : yard.parentB();
        var preview = parent == null ? null : parent.preview();
        if (party == null && parent == null) return;
        var renderable = party != null ? party.asRenderablePokemon()
                : preview == null ? null : parentRenderable(parent, preview);
        if (renderable == null) return;

        EggGroupTab.draw(g, 1, NurseryPcLayout.HEIGHT - 1, renderable.getSpecies());
        drawInspectorPortrait(g, source.id().toString(), renderable, mouseX, mouseY, partialTick);

        var gender = party != null ? party.getGender().name() : parent.gender();
        MutableComponent nickname = party != null ? party.getDisplayName(false)
                : preview.nickname().isBlank() ? renderable.getSpecies().getTranslatedName().copy()
                : Component.literal(preview.nickname());
        int level = party != null ? party.getLevel() : preview.level();
        ResourceLocation ball = party != null ? party.getCaughtBall().getName() : ResourceLocation.tryParse(parent.ballId());
        NurseryPcNativeText.header(gender, field -> {
            if (field.key().equals("name") && ball != null) {
                var art = ball.withPrefix("textures/item/poke_balls/").withSuffix(".png");
                if (minecraft.getResourceManager().getResource(art).isPresent()) blitNativeHalf(g, art, 3.5, 12, 16, 16);
            }
            var value = switch (field.key()) {
                case "level_label" -> Component.translatable("cobblemon.ui.lv");
                case "level" -> Component.literal(Integer.toString(level));
                case "name" -> nickname;
                default -> Component.literal(field.key());
            };
            drawNativePcText(g, field, value);
        });
        var held = party != null ? party.heldItem() : heldItemStack(parent.heldItemId());
        var cosmetic = party != null ? party.getCosmeticItem() : heldItemStack(preview.cosmetic());
        if (!held.isEmpty()) {
            g.renderItem(held, 3, 98);
            g.renderItemDecorations(font, held, 3, 98);
        }
        drawInspectorItem(g, cosmetic, 53, 99);

        var form = renderable.getForm();
        drawInspectorTypeIcons(g, party != null ? party.getPrimaryType() : form.getPrimaryType(),
                party != null ? party.getSecondaryType() : form.getSecondaryType());
        drawInspectorInfoBox(g);

        if (inspector == Inspector.INFO) {
            MutableComponent nature = party != null
                    ? com.cobblemon.mod.common.client.gui.summary.widgets.common.NatureInfoUtilsKt.reformatNatureTextIfMinted(party)
                    : localPokemonText("cobblemon.nature.", parent.natureId());
            MutableComponent ability = party != null ? Component.translatable(party.getAbility().getDisplayName())
                    : localPokemonText("cobblemon.ability.", parent.abilityId());
            List<MutableComponent> moves = party != null ? party.getMoveSet().getMoves().stream()
                    .limit(4).map(move -> move.getDisplayName()).toList()
                    : preview.moves().stream().limit(4).map(id -> {
                        var move = com.cobblemon.mod.common.api.moves.Moves.getByName(id);
                        return move == null ? Component.literal(prettyToken(id)) : move.getDisplayName();
                    }).toList();
            drawInspectorInfo(g, nature, ability, moves, false);
        } else {
            boolean iv = inspector == Inspector.IV;
            List<Stat> stats = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);
            NurseryPcNativeText.stats(iv, field -> {
                if (field.key().startsWith("ui.")) {
                    drawNativePcText(g, field, Component.translatable("cobblemon." + field.key()));
                    return;
                }
                int i = field.key().charAt(4) - '0';
                Stat stat = stats.get(i);
                int value = party == null ? (iv ? preview.ivs() : preview.evs()).get(i)
                        : iv ? party.getIvs().getEffectiveBattleIV(stat) : party.getEvs().getOrDefault(stat);
                var number = Component.literal(Integer.toString(value));
                if (party != null && iv && !java.util.Objects.equals(party.getIvs().get(stat), value)) {
                    number.withStyle(net.minecraft.ChatFormatting.ITALIC);
                }
                drawNativePcText(g, field, number);
            });
        }
    }

    private Grip inspected;
    private String portraitKey;
    private NurseryPcLayout.NativePortrait portraitViewport;

    private static boolean producingPreview(DaycareYardPayload yard) {
        return yard != null && yard.unlocked() && yard.eggPreview() != null
                && !yard.eggPreview().childSpeciesId().isBlank()
                && NurseryInspectorChoice.production(yard.visualState(), true);
    }

    private Grip inspectionGrip(DaycareYardPayload yard, UUID id) {
        if (id == null) return null;
        for (var slot : payload.partySlots()) {
            if (slot.occupied() && id.equals(slot.pokemonId())) {
                return new Grip(true, yard.yardIndex(), slot.slotIndex(), id, slot.speciesId(), slot.formId());
            }
        }
        if (yard.unlocked()) {
            for (int side = 0; side < 2; side++) {
                var parent = side == 0 ? yard.parentA() : yard.parentB();
                if (parent != null && id.equals(parent.pokemonId())) {
                    return new Grip(false, yard.yardIndex(), side, id, parent.speciesId(), parent.formId());
                }
            }
        }
        return null;
    }

    private void drawInspectorPortrait(GuiGraphics g, String key, RenderablePokemon renderable,
                                       int mouseX, int mouseY, float partialTick) {
        var viewport = NurseryPcLayout.nativePortrait(pcFrame);
        var rect = viewport.rect();
        if (!key.equals(portraitKey) || pcPortrait == null || !viewport.equals(portraitViewport)) {
            pcPortrait = new ModelWidget(rect.x(), rect.y(), rect.width(), rect.height(),
                    renderable, viewport.baseScale(), 325F, viewport.offsetY(), false, false, 13);
            pcPortrait.setPokemon(renderable);
            portraitKey = key;
            portraitViewport = viewport;
        } else {
            var prior = pcPortrait.getPokemon();
            if (prior.getSpecies() != renderable.getSpecies() || !prior.getAspects().equals(renderable.getAspects())
                    || !ItemStack.matches(prior.getHeldItem(), renderable.getHeldItem())) {
                // The stock setter recreates FloatingState. Don't restart the portrait animation every draw.
                pcPortrait.setPokemon(renderable);
            }
        }

        // The stock widget clips in raw GUI coordinates, not via the pose matrix. Remove
        // only the Nursery-local frame transform and give the widget their true viewport once.
        // Do not add a second scissor; the native widget owns the matching screen clip.
        var matrices = g.pose();
        matrices.pushPose();
        try {
            pcFrame.undoFit(matrices);
            int screenX = (int) Math.round(pcFrame.screenX(mouseX));
            int screenY = (int) Math.round(pcFrame.screenY(mouseY));
            NurseryPcLayout.drawActorLayer(g::flush, () -> pcPortrait.render(g, screenX, screenY, partialTick));
        } finally {
            matrices.popPose();
        }
    }

    private void drawInspectorTypeIcons(GuiGraphics g, com.cobblemon.mod.common.api.types.ElementalType primary,
                                        com.cobblemon.mod.common.api.types.ElementalType secondary) {
        blitNativeHalf(g, pcArt(secondary == null ? "type_spacer_single" : "type_spacer_double"),
                9, 118.5, 126, 12);
        new com.cobblemon.mod.common.client.gui.TypeIcon(40.5, 117, primary, secondary,
                true, true, 10F, 5F, 1F).render(g);
    }

    static MutableComponent inspectorMovesHeader(boolean offspring) {
        return Component.translatable(offspring ? "gui.cobblenursery.pc.egg_moves" : "cobblemon.ui.moves");
    }

    private void drawInspectorInfoBox(GuiGraphics g) {
        g.blit(pcArt(inspector == Inspector.INFO ? "info_box" : "info_box_stats"),
                9, 128, 0, 0, 63, 69, 63, 69);
    }

    Inspector offspringInspectorPage() {
        // Preserve the ordinary Pokémon's EV page preference on hover. The
        // offspring has no EV page, so show Minimum IVs immediately instead.
        return inspector == Inspector.EV ? Inspector.IV : inspector;
    }

    private void drawOffspringInspector(GuiGraphics g, DaycareYardPayload yard, DaycareEggPreviewPayload offspring,
                                        int mouseX, int mouseY, float partialTick) {
        String key = "offspring:" + offspring.childSpeciesId() + "|" + offspring.childFormId();
        var visual = renderStates.computeIfAbsent(key,
                ignored -> createRenderState(offspring.childSpeciesId(), offspring.childFormId()));
        if (visual == null) return;
        var pokemon = visual.renderablePokemon;
        drawInspectorPortrait(g, key, pokemon, mouseX, mouseY, partialTick);
        NurseryPcNativeText.header("", field -> {
            if (field.key().equals("level_label")) drawNativePcText(g, field, translated("offspring_preview"));
            if (field.key().equals("name")) drawNativePcText(g, field, pokemon.getSpecies().getTranslatedName());
        });
        var form = pokemon.getForm();
        drawInspectorTypeIcons(g, form.getPrimaryType(), form.getSecondaryType());
        drawInspectorInfoBox(g);
        // The existing server projection is not a promise of exact unseen rolls or EVs.
        if (offspringInspectorPage() == Inspector.INFO) {
            MutableComponent unknown = Component.literal("\u2014");
            MutableComponent nature = offspringNatureLabel(yard);
            var moves = offspringEggMoveLabels(offspring);
            drawInspectorInfo(g, nature, unknown, moves, true);
        } else {
            var minimums = offspringMinimums(yard);
            NurseryPcNativeText.stats(true,
                    field -> drawNativePcText(g, field, offspringStatLabel(field.key(), minimums)));
        }
    }

    static List<Integer> offspringMinimums(DaycareYardPayload yard) {
        var a = yard.parentA();
        var b = yard.parentB();
        return bat.cobblenursery.breeding.service.IvInheritanceService.guaranteedMinimums(
                a == null ? "" : a.heldItemId(), a == null || a.preview() == null ? List.of() : a.preview().ivs(),
                b == null ? "" : b.heldItemId(), b == null || b.preview() == null ? List.of() : b.preview().ivs());
    }

    static MutableComponent offspringStatLabel(String key, List<Integer> minimums) {
        if (key.equals("ui.stats.ivs") && minimums != null)
            return Component.translatable("gui.cobblenursery.pc.minimum_ivs");
        if (key.startsWith("ui.")) return Component.translatable("cobblemon." + key);
        int value = minimums == null ? 0 : minimums.get(key.charAt(4) - '0');
        return Component.literal(value > 0 ? Integer.toString(value) : "—");
    }

    private DaycareEggPreviewPayload namedEggMovesFor;
    private List<MutableComponent> cachedEggMoveLabels = List.of();

    private List<MutableComponent> offspringEggMoveLabels(DaycareEggPreviewPayload offspring) {
        if (namedEggMovesFor != offspring) {
            namedEggMovesFor = offspring;
            cachedEggMoveLabels = bat.cobblenursery.breeding.service.EggMoveInheritanceService
                    .projectEggMoves(offspring.moveIds(), offspring.childSpeciesId(), offspring.childFormId())
                    .stream().map(com.cobblemon.mod.common.api.moves.Moves::getByName)
                    .filter(java.util.Objects::nonNull).map(move -> move.getDisplayName()).toList();
        }
        return cachedEggMoveLabels;
    }

    static MutableComponent offspringNatureLabel(DaycareYardPayload yard) {
        var a = yard.parentA();
        var b = yard.parentB();
        if (a != null && b != null && yard.inheritedNatureId() != null && !yard.inheritedNatureId().isBlank()) {
            String certain = bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                    a.natureId(), new bat.cobblenursery.nursery.model.BreedingItemSnapshot(a.heldItemId(), 1),
                    b.natureId(), new bat.cobblenursery.nursery.model.BreedingItemSnapshot(b.heldItemId(), 1));
            // Older v3 servers may send a particular roll when both holders disagree.
            if (certain.equals(yard.inheritedNatureId())) {
                var nature = com.cobblemon.mod.common.api.pokemon.Natures.getNature(ResourceLocation.parse(certain));
                return Component.translatable(nature.getDisplayName());
            }
        }
        return Component.literal("—");
    }

    private static Pokemon localPartyPokemon(UUID id) {
        var storage = CobblemonClient.INSTANCE.getStorage().getParty();
        if (storage == null) return null;
        for (Pokemon pokemon : storage) if (pokemon != null && id.equals(pokemon.getUuid())) return pokemon;
        return null;
    }

    private RenderablePokemon parentRenderable(DaycareParentPayload parent, DaycareParentPreview preview) {
        ResourceLocation id = ResourceLocation.tryParse(parent.speciesId());
        Species species = id == null ? null : PokemonSpecies.getByIdentifier(id);
        return species == null ? null : new RenderablePokemon(species, java.util.Set.copyOf(preview.aspects()), heldItemStack(parent.heldItemId()));
    }

    private static void blitNativeHalf(GuiGraphics g, ResourceLocation art, double x, double y, int width, int height) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(.5F, .5F, 1);
            g.blit(art, 0, 0, 0, 0, width, height, width, height);
        } finally {
            g.pose().popPose();
        }
    }

    private void drawInspectorItem(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(.5F, .5F, 1F);
            g.renderItem(stack, 0, 0);
        } finally {
            g.pose().popPose();
        }
    }

    private void drawInspectorInfo(GuiGraphics g, MutableComponent nature, MutableComponent ability,
                                   List<MutableComponent> moves, boolean offspring) {
        NurseryPcNativeText.info(field -> {
            var key = field.key();
            MutableComponent value;
            if (key.equals("ui.moves")) value = inspectorMovesHeader(offspring);
            else if (key.startsWith("ui.")) value = Component.translatable("cobblemon." + key);
            else if (key.equals("nature")) value = nature;
            else if (key.equals("ability")) value = ability;
            else {
                int i = key.charAt(4) - '0' - (4 - moves.size());
                value = i < 0 ? Component.literal("\u2014") : moves.get(i);
            }
            drawNativePcText(g, field, value);
        });
    }

    private MutableComponent localPokemonText(String prefix, String id) {
        int separator = id.indexOf(':');
        String key = prefix + (separator < 0 ? id : id.substring(separator + 1));
        return net.minecraft.locale.Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(prettyToken(id));
    }

    private static ResourceLocation nativeLargeFont() {
        return CobblemonResources.INSTANCE.getDEFAULT_LARGE();
    }

    private static void drawNativePcText(GuiGraphics g, NurseryPcNativeText.Draw draw, Component value) {
        MutableComponent text = draw.bold() ? value.copy().withStyle(net.minecraft.ChatFormatting.BOLD) : value.copy();
        // Match the stock native arguments, not a manually pre-scaled or pre-centered approximation.
        // The outer GUI pose alone adapts the local 349x205 canvas to a smaller viewport.
        RenderHelperKt.drawScaledText(g, draw.large() ? nativeLargeFont() : null, text, draw.x(), draw.y(),
                draw.scale(), 1F, Integer.MAX_VALUE, draw.color(), draw.centered(), draw.shadow(), null, null);
    }

    private void drawPcStats(GuiGraphics g, DaycarePartyPokemonPayload slot) {
        drawStockCentered(g, "IV", 50, 105, 15, PC_MINT);
        drawStockCentered(g, "EV", 69, 105, 17, PC_MINT);
        String[] labels = java.util.stream.IntStream.range(0,6).mapToObj(i -> translated("stat_"+i).getString()).toArray(String[]::new);
        int[] iv = {slot.hpIv(),slot.attackIv(),slot.defenceIv(),slot.specialAttackIv(),slot.specialDefenceIv(),slot.speedIv()};
        int[] ev = {slot.hpEv(),slot.attackEv(),slot.defenceEv(),slot.specialAttackEv(),slot.specialDefenceEv(),slot.speedEv()};
        for (int i = 0; i < labels.length; i++) {
            int y = 114 + i * 8;
            if ((i & 1) == 0) g.fill(17, y, 78, y + 8, 0x4420323B);
            drawAdaptiveText(g, labels[i], 18, y, 21, PC_SOFT, false);
            drawAdaptiveText(g, Integer.toString(iv[i]), 43, y, 15, PC_TEXT, true);
            drawAdaptiveText(g, Integer.toString(ev[i]), 60, y, 18, PC_TEXT, true);
        }
    }

    private void drawPcEggDetails(GuiGraphics g, DaycareEggPreviewPayload egg) {
        if (egg == null) {
            drawPcWrapped(g, translated("no_preview").getString(), 17, 100, 61, 4, PC_SOFT);
            return;
        }
        pcDetailRow(g, "nature", prettyToken(egg.natureId()), 98);
        pcDetailRow(g, "ability", prettyToken(egg.abilityId()), 107);
        pcDetailRow(g, "ball", prettyToken(egg.ballId()), 116);
        drawAdaptiveText(g, translated("moves").getString(), 18, 126, 60, PC_SOFT, false);
        var moves = egg.moveIds().isEmpty() ? List.of(translated("none").getString()) : rightPanelMoveLines(egg);
        for (int i = 0; i < Math.min(4, moves.size()); i++) {
            drawAdaptiveText(g, moves.get(i), 18, 136 + i * 6, 60, PC_TEXT, false, moves.size() > 3, moves.size() > 3);
        }
    }

    private void drawPcRolls(GuiGraphics g, DaycareYardPayload yard) {
        pcDetailRow(g, "iv_picks", yard.inheritedIvCount() + "/6", 94);
        String nature = yard.inheritedNatureId().isBlank() ? translated("random").getString() : rightPanelNatureRollText(yard);
        pcDetailRow(g, "nature", nature, 103);
        pcDetailRow(g, "shiny", "1/" + NurseryConfig.DEFAULT.shinyOddsDenominator(), 112);
        var egg = yard.eggPreview();
        if (egg == null) return;
        var mins = offspringMinimums(yard);
        String[] labels = java.util.stream.IntStream.range(0,6).mapToObj(i -> translated("stat_"+i).getString()).toArray(String[]::new);
        for (int i = 0; i < mins.size(); i++) {
            int y = 123 + (i % 3) * 12, x = i < 3 ? 18 : 49;
            drawAdaptiveText(g, labels[i], x, y, 14, PC_SOFT, false, true);
            drawAdaptiveText(g, ivRollRangeText(mins.get(i)), x + 14, y, 16, PC_TEXT, false, true);
        }
    }

    private void pcDetailRow(GuiGraphics g, String label, String value, int y) {
        drawAdaptiveText(g, translated(label).getString(), 17, y, 24, PC_SOFT, false);
        drawAdaptiveText(g, value, 43, y, 35, PC_TEXT, false);
    }

    NurseryHabitatFx.Frame sampleUnlockFrame(int yardIndex, long now) {
        var tab = NurseryPcLayout.tab(yardIndex);
        double spriteCenterY = tab.y() + (tab.height()-10)/2 + 5;
        return habitatFx.frame(yardIndex, now, pcFrame.localY(height) - spriteCenterY + 12);
    }

    private void drawDetachedLock(GuiGraphics g) {
        if (unlockFrame == null || selectedYard() == null) return;
        var tab = NurseryPcLayout.tab(selectedYard().yardIndex());
        int x = tab.x() + (tab.width()-10)/2, y = tab.y() + (tab.height()-10)/2;
        g.pose().pushPose();
        try {
            g.pose().translate(x+5+unlockFrame.dx(), y+5+unlockFrame.dy(), 550);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)unlockFrame.rotation()));
            drawPcLock(g, -5, -5);
        } finally { g.pose().popPose(); }
    }

    private static void drawPcLock(GuiGraphics g, int x, int y) {
        blitNativeHalf(g, ResourceLocation.fromNamespaceAndPath("cobblemon",
                "textures/gui/trade/trade_slot_icon_locked.png"), x, y, 20, 20);
    }

    private void drawPcCursor(GuiGraphics g, double mouseX, double mouseY, float partialTick) {
        if (pickupGhost == null || pickupSway == null) return;
        var tilt = pickupSway.sample(mouseX, mouseY, net.minecraft.Util.getMillis());
        final double gap = 4;
        final double targetX = pcFrame.screenX(mouseX);
        final double targetY = pcFrame.screenY(mouseY);
        var matrices = new com.mojang.blaze3d.vertex.PoseStack();
        matrices.last().pose().set(g.pose().last().pose());
        matrices.last().normal().set(g.pose().last().normal());

        var visual = pickupGhost.renderablePokemon;
        var state = pickupGhost.floatingState;
        state.setCurrentAspects(visual.getAspects());
        boolean sprite = VaryingModelRepository.INSTANCE.getSprite(visual.getSpecies().getResourceIdentifier(), state,
                com.cobblemon.mod.common.client.render.SpriteType.PROFILE) != null;
        // The native sprite branch is already centered; its submitted quad starts at local y=0.
        // For posed models this initial placement is only a safe fallback if a custom poser cannot be probed.
        var profile = sprite ? null : profileFor(pickupGhost);
        double x = mouseX - (profile == null ? 0 : 20 * profile.x());
        double y = mouseY + gap / pcFrame.scale()
                - (profile == null ? 0 : NurseryPcLayout.profileShiftY(profile.y(), profile.scale(), 20));
        matrices.translate(x, y, 320);
        if (sprite) NurseryGhostProjection.alignNativeSprite(matrices, targetX, targetY, gap, tilt.roll());
        Quaternionf rotation = NurseryCursorInteraction.heldModelRotation(tilt);
        g.flush();
        NurseryGhostProjection.centerGhost(targetX, targetY, gap, tilt.roll(), () ->
                PokemonGuiUtilsKt.drawProfilePokemon(visual, matrices, rotation, PoseType.PROFILE, state,
                        partialTick, 20F, ProfileTransformType.PROFILE, false, 1F, 1F, 1F, 0.92F, 0F, 0F, 13));
        if (!pickupRecoilAttempted && pickupGhost.floatingState.getCurrentModel() != null) {
            pickupRecoilAttempted = true;
            try {
                var animation = state.getCurrentModel().getAnimation(state, "recoil", state.getRuntime());
                if (animation instanceof BedrockActiveAnimation bedrock && !bedrock.getAnimation().getShouldLoop()
                        && bedrock.getAnimation().getEffects().isEmpty()) {
                    state.addActiveAnimation(bedrock, ignored -> kotlin.Unit.INSTANCE);
                }
            } catch (RuntimeException ignored) {
                // Missing or effectful recoil falls back to mouse-driven sway.
            }
        }
    }

    private void drawWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawYardWallpapers(graphics);
        drawDetailsPortraitBackground(graphics);
        drawSelectedYardPreview(graphics, partialTick);
        ResourceLocation activeBackgroundTexture = showRightPanelRolls ? DAYCARE_BASE_ROLLS_TEXTURE : DAYCARE_BASE_TEXTURE;

        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0F);
        graphics.pose().scale(GUI_SCALE, GUI_SCALE, 1.0F);
        graphics.blit(
                activeBackgroundTexture,
                0,
                0,
                0,
                0,
                DaycareLayout.BASE_WIDTH,
                DaycareLayout.BASE_HEIGHT,
                DaycareLayout.BASE_WIDTH,
                DaycareLayout.BASE_HEIGHT
        );
        graphics.pose().popPose();

        for (DaycareYardPayload yard : payload.yards()) {
            renderYardPokemon(graphics, yard, partialTick);
        }

        drawYardEggSummaries(graphics);
        drawYardHeartLoaders(graphics);
    }


    private void drawDetailsPortraitBackground(GuiGraphics graphics) {
        DaycareLayout.Rect rect = layout.detailsPreviewRect();

        int portraitShiftRight = -2;
        int portraitShiftUp = -2;
        int portraitExtraWidth = 4;
        int portraitExtraHeight = 4;

        int drawX = rect.x() - (portraitExtraWidth / 2) + portraitShiftRight;
        int drawY = rect.y() - (portraitExtraHeight / 2) + portraitShiftUp;
        int drawWidth = rect.width() + portraitExtraWidth;
        int drawHeight = rect.height() + portraitExtraHeight;

        graphics.enableScissor(
                rect.x(),
                rect.y(),
                rect.x() + rect.width(),
                rect.y() + rect.height()
        );

        graphics.blit(
                PC_PORTRAIT_BACKGROUND_TEXTURE,
                drawX,
                drawY,
                0,
                0,
                drawWidth,
                drawHeight,
                PC_PORTRAIT_BACKGROUND_WIDTH,
                PC_PORTRAIT_BACKGROUND_HEIGHT
        );

        graphics.disableScissor();
    }

    private void drawSelectedYardPreview(GuiGraphics graphics, float partialTick) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null || !yard.unlocked()) {
            return;
        }

        DaycareLayout.Rect rect = layout.detailsPreviewRect();
        graphics.enableScissor(
                rect.x(),
                rect.y(),
                rect.x() + rect.width(),
                rect.y() + rect.height()
        );

        if (yard.eggPreview() != null) {
            renderDetailsEggPreview(graphics, yard.eggPreview(), partialTick);
        } else if (yard.parentA() != null) {
            renderDetailsParentPreview(graphics, yard.parentA(), partialTick, false);
        } else if (yard.parentB() != null) {
            renderDetailsParentPreview(graphics, yard.parentB(), partialTick, true);
        }

        graphics.disableScissor();
    }

    private void renderDetailsParentPreview(
            GuiGraphics graphics,
            DaycareParentPayload parent,
            float partialTick,
            boolean rightFacing
    ) {
        DaycareLayout.Point center = layout.detailsPreviewCenter();
        renderPokemonModel(
                graphics,
                "details_parent:" + parent.speciesId() + ":" + parent.formId() + ":" + (rightFacing ? "B" : "A"),
                parent.speciesId(),
                parent.formId(),
                center.x(),
                center.y() + Math.sin(System.currentTimeMillis() / 420.0D) * 1.1D,
                DETAILS_RENDER_SCALE,
                rightFacing ? 16.0F : -16.0F,
                6.0F,
                partialTick
        );
    }

    private void drawForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (DaycarePartyPokemonPayload slot : payload.partySlots()) {
            if (slot.occupied()) {
                drawPcPartySlot(graphics, slot, partialTick);
            }
        }

        drawYardHeadersAndLockedInfo(graphics);
        drawYardNameTags(graphics, partialTick);
        drawRightPanelHeader(graphics);
        DaycareLayout.Rect details = layout.detailsTextRect();
        int detailBottom = layout.detailsGlyphBottom();
        graphics.enableScissor(details.x() + 3, details.y() + 24, details.x() + details.width(), detailBottom);
        try {
            if (ticksElapsed < failureVisibleUntil) {
                drawInlineFailure(graphics, details, detailBottom);
            } else if (showRightPanelRolls) {
                drawRightPanelRollDetails(graphics);
            } else {
                drawRightPanelStatusAndCompatibility(graphics);
                drawRightPanelResultData(graphics);
            }
        } finally {
            graphics.disableScissor();
        }
        drawRightPanelToggleHint(graphics);
        drawHoveredPokemonStats(graphics, hoveredPartySlot(mouseX, mouseY));
    }

    private void drawInlineFailure(GuiGraphics graphics, DaycareLayout.Rect details, int bottom) {
        int x = details.x() + 5;
        int maxWidth = details.width() - 10;
        int y = details.y() + 25;
        drawStockLine(graphics, Component.translatable("gui.cobblenursery.daycare.action_failed").getString(), x, y, maxWidth, 0xFFFFADB1);
        y += font.lineHeight + 3;
        var lines = this.font.split(stockComponent(failureMessage, false), maxWidth);
        for (var line : lines) {
            if (y + font.lineHeight > bottom - 2) break;
            var anchor = DaycareUiText.stockAnchor(x, y, 0, 0, 1.0F, false);
            RenderHelperKt.drawScaledText(graphics, line, anchor.x(), anchor.y(), 1.0F, 1.0F, 1.0F,
                    0xFFFFE4E5, anchor.centered(), anchor.shadow());
            y += font.lineHeight;
        }
    }

    private static MutableComponent stockComponent(String text, boolean bold) {
        return Component.literal(text).withStyle(style ->
            style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(bold)
        );
    }

    private static int stockWidth(Font font, String text, boolean bold) {
        return font.width(stockComponent(text, bold));
    }

    private static void drawStockText(GuiGraphics graphics, Font font, String text, int x, int y,
                                      int width, int color, float scale, boolean centered, boolean bold) {
        String fitted = DaycareUiText.fit(text, (int) (width / scale), value -> stockWidth(font, value, bold));
        if (fitted.isEmpty()) return;
        var anchor = DaycareUiText.stockAnchor(x, y, width, stockWidth(font, fitted, bold), scale, centered);
        drawStockAtAnchor(graphics, fitted, anchor, color, scale, bold);
    }

    private static void drawStockUnclipped(GuiGraphics graphics, String text, int x, int y,
                                           int color, float scale, boolean bold) {
        drawStockAtAnchor(graphics, text, DaycareUiText.stockAnchor(x, y, 0, 0, scale, false), color, scale, bold);
    }

    private static void drawStockAtAnchor(GuiGraphics graphics, String text, DaycareUiText.StockAnchor anchor,
                                          int color, float scale, boolean bold) {
        // Cobblemon 1.8.x: arguments are centered THEN shadow. These anchors are already left-aligned.
        RenderHelperKt.drawScaledText(graphics, CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                stockComponent(text, bold), anchor.x(), anchor.y(), scale, 1.0F, Integer.MAX_VALUE, color,
                anchor.centered(), anchor.shadow(), null, null);
    }

    private void drawStockLine(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        drawStockText(graphics, font, text, x, y, width, color, 1.0F, false, false);
    }

    private void drawAdaptiveText(GuiGraphics graphics, String text, int x, int y, int width, int color, boolean centered) {
        drawAdaptiveText(graphics, text, x, y, width, color, centered, false);
    }

    private void drawAdaptiveText(GuiGraphics graphics, String text, int x, int y, int width, int color, boolean centered, boolean compact) {
        drawAdaptiveText(graphics, text, x, y, width, color, centered, compact, false);
    }

    private void drawAdaptiveText(GuiGraphics graphics, String text, int x, int y, int width, int color,
                                  boolean centered, boolean compact, boolean bold) {
        int measured = stockWidth(font, text, bold);
        float scale = DaycareUiText.stockScale(measured, width, compact);
        int scaledY = scale == 1.0F || compact ? y : y + 2;
        int rowHeight = compact ? 4 : 8;
        int overflow = (int) Math.ceil(measured * scale) - width;
        if (overflow > 0 && textMouseX >= x && textMouseX < x + width && textMouseY >= y && textMouseY < y + rowHeight) {
            // Keep the full value readable within the hovered entry, without an external tooltip.
            long now = net.minecraft.Util.getMillis();
            if (marqueeCellX != x || marqueeCellY != y) {
                marqueeCellX = x;
                marqueeCellY = y;
                marqueeStartedAt = now;
            }
            long phase = (now - marqueeStartedAt) % (1200L + overflow * 110L);
            int offset = (int) Math.min(overflow, Math.max(0L, phase - 600L) / 110L);
            clipPc(graphics, x, y, x + width, y + rowHeight);
            try {
                drawStockUnclipped(graphics, text, x - offset, scaledY, color, scale, bold);
            } finally {
                graphics.disableScissor();
            }
        } else {
            if (marqueeCellX == x && marqueeCellY == y) marqueeCellX = Integer.MIN_VALUE;
            drawStockText(graphics, font, text, x, scaledY, width, color, scale, centered, bold);
        }
    }

    private void drawStockCentered(GuiGraphics graphics, String text, int center, int y, int width, int color) {
        drawStockText(graphics, font, text, center - width / 2, y, width, color, 1.0F, true, false);
    }

    private void drawRightPanelResultData(GuiGraphics graphics) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null) {
            return;
        }

        DaycareEggPreviewPayload egg = yard.eggPreview();

        int y = topPos + DETAILS_RESULT_START_Y;

        drawPanelSectionTitle(
                graphics,
                "INFO",
                leftPos + DETAILS_RESULT_LABEL_X,
                layout.summaryInfoTitleY()
        );

        drawPcPanelInfoLine(
                graphics,
                "Nature",
                rightPanelNatureText(egg),
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_NATURE
        );

        y += 8;

        drawPcPanelInfoLine(
                graphics,
                "Ability",
                rightPanelAbilityText(egg),
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_ABILITY
        );

        y += 8;

        drawPcPanelInfoLine(
                graphics,
                "Ball",
                rightPanelBallText(egg),
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_BALL
        );

        drawEggMovesSection(graphics, egg, topPos + DETAILS_MOVES_START_Y);
    }

    private void drawRightPanelRollDetails(GuiGraphics graphics) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null) {
            return;
        }

        drawIvRollRangeSection(graphics, yard, topPos + 103);

        int y = topPos + 167;
        drawPanelSectionTitle(
                graphics,
                "ROLLS",
                leftPos + DETAILS_RESULT_LABEL_X,
                y - 9
        );

        drawPcPanelInfoLine(
                graphics,
                "IVs",
                yard.inheritedIvCount() + "/6",
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_VALUE
        );

        y += DETAILS_RESULT_LINE_GAP;

        drawPcPanelInfoLine(
                graphics,
                "Nature",
                rightPanelNatureRollText(yard),
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_NATURE
        );

        y += DETAILS_RESULT_LINE_GAP;

        drawPcPanelInfoLine(
                graphics,
                "Shiny",
                "1/" + NurseryConfig.DEFAULT.shinyOddsDenominator(),
                leftPos + DETAILS_RESULT_LABEL_X,
                leftPos + DETAILS_RESULT_VALUE_X,
                y,
                COLOR_PANEL_SUBTITLE,
                COLOR_PANEL_BALL
        );
    }

    private String rightPanelNatureText(DaycareEggPreviewPayload egg) {
        if (egg == null || egg.natureId() == null || egg.natureId().isBlank()) {
            return "---";
        }
        return prettyToken(egg.natureId());
    }

    private String rightPanelAbilityText(DaycareEggPreviewPayload egg) {
        if (egg == null || egg.abilityId() == null || egg.abilityId().isBlank()) {
            return "---";
        }
        return prettyToken(egg.abilityId());
    }

    private String rightPanelBallText(DaycareEggPreviewPayload egg) {
        if (egg == null || egg.ballId() == null || egg.ballId().isBlank()) {
            return "---";
        }
        return prettyToken(egg.ballId());
    }

    private String rightPanelNatureRollText(DaycareYardPayload yard) {
        if (yard == null || yard.inheritedNatureId() == null || yard.inheritedNatureId().isBlank()) {
            return "Random";
        }
        return "100% " + prettyToken(yard.inheritedNatureId());
    }

    private String ivRollRangeText(int minimum) {
        int clampedMinimum = Math.max(0, Math.min(31, minimum));
        return clampedMinimum >= 31 ? "31" : clampedMinimum + "-31";
    }

    private List<String> rightPanelMoveLines(DaycareEggPreviewPayload egg) {
        if (egg == null || egg.moveIds() == null || egg.moveIds().isEmpty()) {
            return List.of("None");
        }

        List<String> lines = new ArrayList<>();
        for (String moveId : egg.moveIds()) {
            lines.add(prettyToken(moveId));
        }
        return lines;
    }

    private void drawEggMovesSection(GuiGraphics graphics, DaycareEggPreviewPayload egg, int startY) {
        drawPanelSectionTitle(
                graphics,
                "EGG MOVES",
                leftPos + DETAILS_RESULT_LABEL_X,
                startY
        );

        List<String> moveLines = rightPanelMoveLines(egg);
        int x = leftPos + DETAILS_RESULT_LABEL_X;
        int width = detailsRight() - x;
        boolean compact = moveLines.size() > 2;
        for (int index = 0; index < moveLines.size(); index++) {
            int y = layout.summaryEggMoveY(index, compact);
            if (y + (compact ? 4 : font.lineHeight) > layout.detailsGlyphBottom()) break;
            drawAdaptiveText(graphics, moveLines.get(index), x, y, width, COLOR_PANEL_VALUE, false, compact, compact);
        }
    }

    private void drawIvRollRangeSection(GuiGraphics graphics, DaycareYardPayload yard, int startY) {
        drawPanelSectionTitle(
                graphics,
                "IV RANGE",
                leftPos + DETAILS_RESULT_LABEL_X,
                startY
        );

        DaycareEggPreviewPayload egg = yard.eggPreview();
        if (egg == null) {
            drawPcPanelValueOnlyLine(
                    graphics,
                    "Need egg preview",
                    leftPos + DETAILS_RESULT_LABEL_X,
                    startY + DETAILS_MOVES_SECTION_GAP,
                    COLOR_PANEL_REASON
            );
            return;
        }

        int y = startY + 7;
        var minimums = offspringMinimums(yard);
        drawPcPanelInfoLine(graphics, "HP", ivRollRangeText(minimums.get(0)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
        y += 8;
        drawPcPanelInfoLine(graphics, "Atk", ivRollRangeText(minimums.get(1)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
        y += 8;
        drawPcPanelInfoLine(graphics, "Def", ivRollRangeText(minimums.get(2)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
        y += 8;
        drawPcPanelInfoLine(graphics, "SpA", ivRollRangeText(minimums.get(3)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
        y += 8;
        drawPcPanelInfoLine(graphics, "SpD", ivRollRangeText(minimums.get(4)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
        y += 8;
        drawPcPanelInfoLine(graphics, "Spe", ivRollRangeText(minimums.get(5)), leftPos + DETAILS_RESULT_LABEL_X, leftPos + DETAILS_RESULT_VALUE_X, y, COLOR_PANEL_SUBTITLE, COLOR_PANEL_VALUE);
    }

    private void drawRightPanelStatusAndCompatibility(GuiGraphics graphics) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null) {
            return;
        }

        int y = topPos + DETAILS_INFO_START_Y;
        int labelX = leftPos + DETAILS_INFO_LABEL_X;
        int valueX = leftPos + DETAILS_INFO_VALUE_X + 4;

        drawPanelTitleValueLine(
                graphics,
                "STATE",
                rightPanelStateText(yard),
                labelX,
                valueX,
                y,
                COLOR_PANEL_TITLE,
                COLOR_PANEL_SUBTITLE,
                -1
        );

        y += DETAILS_INFO_LINE_GAP;

        drawPanelTitleValueLine(
                graphics,
                "BREED",
                rightPanelCompatibilityText(yard),
                labelX,
                valueX,
                y - 1,
                COLOR_PANEL_TITLE,
                COLOR_PANEL_SUBTITLE,
                0
        );

        if (shouldShowCompatibilityReason(yard) || shouldShowEggGroups(yard)) {
            if (shouldShowCompatibilityReason(yard)) {
                drawCenteredWrappedLines(
                        graphics,
                        List.of(prettyReason(yard.compatibilityReason())),
                        COLOR_PANEL_REASON
                );
            } else {
                drawCenteredWrappedLines(
                        graphics,
                        eggGroupLines(yard),
                        COLOR_PANEL_REASON
                );
            }
        }
    }

    private boolean shouldShowEggGroups(DaycareYardPayload yard) {
        return yard.unlocked()
                && yard.parentA() != null
                && yard.parentB() != null
                && yard.compatible();
    }

    private List<String> eggGroupLines(DaycareYardPayload yard) {
        if (yard.parentA() == null || yard.parentB() == null) {
            return List.of();
        }
        return List.of(
                parentEggGroupLine(yard.parentA()),
                parentEggGroupLine(yard.parentB())
        );
    }

    private String parentEggGroupLine(DaycareParentPayload parent) {
        if (parent == null) {
            return "-";
        }
        return prettySpecies(parent.speciesId()) + ": " + prettyEggGroups(parent);
    }

    private String prettyEggGroups(DaycareParentPayload parent) {
        if (parent == null) {
            return "-";
        }
        if (parent.eggGroups() == null || parent.eggGroups().isEmpty()) {
            return prettySpecies(parent.speciesId()).equalsIgnoreCase("Ditto") ? "Ditto" : "-";
        }
        return joinPretty(parent.eggGroups());
    }

    private void drawCenteredWrappedLines(GuiGraphics graphics, List<String> values, int valueColor) {
        DaycareLayout.Rect details = layout.detailsTextRect();
        int maxWidth = details.width() - 10;
        for (int index = 0; index < values.size(); index++) {
            int y = layout.compatibilityLabelY(index);
            if (y + 4 >= layout.summaryInfoTitleY()) break;
            drawAdaptiveText(graphics, values.get(index), details.centerX() - maxWidth / 2,
                    y, maxWidth, valueColor, true, true);
        }
    }

    private int detailsRight() {
        DaycareLayout.Rect details = layout.detailsTextRect();
        return details.x() + details.width() - 4;
    }

    private void drawPanelSectionTitle(GuiGraphics graphics, String title, int x, int y) {
        drawStockLine(graphics, title, x, y, detailsRight() - x, COLOR_PANEL_TITLE);
    }

    private void drawPcPanelInfoLine(GuiGraphics graphics, String label, String value, int labelX, int valueX,
                                      int y, int labelColor, int valueColor) {
        drawAdaptiveText(graphics, label, labelX, y, valueX - labelX - 1, labelColor, false);
        drawAdaptiveText(graphics, value, valueX, y, detailsRight() - valueX, valueColor, false);
    }

    private void drawPanelTitleValueLine(GuiGraphics graphics, String label, String value, int labelX, int valueX,
                                         int y, int labelColor, int valueColor, int valueYOffset) {
        drawPcPanelInfoLine(graphics, label, value, labelX, valueX, y, labelColor, valueColor);
    }

    private void drawPcPanelValueOnlyLine(GuiGraphics graphics, String value, int x, int y, int valueColor) {
        drawStockLine(graphics, value, x, y, detailsRight() - x, valueColor);
    }

    private String rightPanelStateText(DaycareYardPayload yard) {
        return switch (yard.visualState()) {
            case "LOCKED" -> "Locked";
            case "EMPTY" -> "Empty";
            case "INCOMPLETE" -> "Waiting";
            case "READY_TO_START" -> "Ready";
            case "BREEDING" -> "Breeding";
            case "PAUSED_NO_SPACE" -> "Paused";
            case "BLOCKED" -> "Blocked";
            default -> trimLabelSafe(prettyToken(yard.visualState()), 16);
        };
    }

    private int rightPanelStateColor(DaycareYardPayload yard) {
        return switch (yard.visualState()) {
            case "READY_TO_START" -> 0xFF9EE6B8;
            case "BREEDING" -> 0xFFFFC97A;
            case "PAUSED_NO_SPACE" -> 0xFFFFA7B8;
            case "BLOCKED", "LOCKED" -> 0xFFE6B3C2;
            default -> 0xFFF2EEF2;
        };
    }

    private String rightPanelCompatibilityText(DaycareYardPayload yard) {
        if (!yard.unlocked()) {
            return "Locked";
        }
        if (yard.parentA() == null || yard.parentB() == null) {
            return "No pair";
        }
        return yard.compatible() ? "Match" : "No Match";
    }

    private int rightPanelCompatibilityColor(DaycareYardPayload yard) {
        if (!yard.unlocked()) {
            return 0xFFC7C1C7;
        }
        if (yard.parentA() == null || yard.parentB() == null) {
            return 0xFFD8D2D8;
        }
        return yard.compatible() ? 0xFF9EE6B8 : 0xFFFFA7B8;
    }

    private boolean shouldShowCompatibilityReason(DaycareYardPayload yard) {
        if (!yard.unlocked()) {
            return false;
        }
        if (yard.parentA() == null || yard.parentB() == null) {
            return false;
        }
        return !yard.compatible()
                && yard.compatibilityReason() != null
                && !yard.compatibilityReason().isBlank();
    }

    private void drawRightPanelHeader(GuiGraphics graphics) {
        DaycareYardPayload yard = selectedYard();
        if (yard == null) return;

        DaycareLayout.Rect details = layout.detailsTextRect();
        int x = details.x() + 4;
        int width = details.width() - 8;
        drawAdaptiveText(graphics, buildRightPanelHeaderTitle(yard).getString(), x,
                details.y() + 7, width, COLOR_PANEL_TITLE, true);
        drawAdaptiveText(graphics, buildRightPanelHeaderSubtitle(yard).getString(), x,
                details.y() + 16, width, COLOR_PANEL_SUBTITLE, true);
    }

    private Component buildRightPanelHeaderTitle(DaycareYardPayload yard) {
        if (yard.eggPreview() != null) {
            DaycareEggPreviewPayload egg = yard.eggPreview();
            return Component.literal(prettySpecies(egg.childSpeciesId()) + formSuffix(egg.childFormId()));
        }
        if (yard.parentA() != null && yard.parentB() != null) {
            return Component.literal(prettySpecies(yard.parentA().speciesId()) + " / " + prettySpecies(yard.parentB().speciesId()));
        }
        DaycareParentPayload parent = yard.parentA() == null ? yard.parentB() : yard.parentA();
        return parent == null ? Component.literal("Empty Yard") :
                Component.literal(prettySpecies(parent.speciesId()) + formSuffix(parent.formId()));
    }

    private Component buildRightPanelHeaderSubtitle(DaycareYardPayload yard) {
        String yardLabel = "Yard " + (yard.yardIndex() + 1);

        if (showRightPanelRolls) {
            return Component.literal(yardLabel + " - Rolls");
        }

        if (yard.eggPreview() != null) {
            return Component.literal(yardLabel + " / Egg");
        }

        if (yard.parentA() != null || yard.parentB() != null) {
            return Component.literal(yardLabel + " / Pair");
        }

        return Component.literal(yardLabel);
    }

    private void drawRightPanelToggleHint(GuiGraphics graphics) {
        DaycareLayout.Rect preview = layout.detailsPreviewRect();
        int left = preview.x() + 2;
        int right = preview.x() + preview.width() - 2;
        int bottom = preview.y() + preview.height() - 2;
        int top = bottom - 11;
        String key = showRightPanelRolls ? "gui.cobblenursery.daycare.show_summary" : "gui.cobblenursery.daycare.show_rolls";
        graphics.fill(left, top, right, bottom, 0xF0222428);
        String hint = Component.translatable(key).getString();
        int width = right - left - 4;
        float scale = DaycareUiText.stockScale(stockWidth(font, hint, true), width, false);
        drawStockText(graphics, font, hint, left + 2, top + (11 - Math.round(8 * scale)) / 2,
                width, COLOR_PANEL_TITLE, scale, true, true);
    }

    private void drawYardNameTags(GuiGraphics graphics, float partialTick) {
        int selectedYardIndex = payload.selectedYardIndex();

        for (DaycareYardPayload yard : payload.yards()) {
            if (!yard.unlocked()) {
                continue;
            }

            if (yard.yardIndex() != selectedYardIndex) {
                continue;
            }

            YardActorState actorA = yard.parentA() == null ? null : getOrCreateYardActorState(yard, false, yard.parentA());
            YardActorState actorB = yard.parentB() == null ? null : getOrCreateYardActorState(yard, true, yard.parentB());

            if (yard.parentA() != null && actorA != null && !isHeldParent(yard, yard.parentA(), false)) {
                drawYardNameTag(graphics, yard, yard.parentA(), actorA, partialTick);
            }

            if (yard.parentB() != null && actorB != null && !isHeldParent(yard, yard.parentB(), true)) {
                drawYardNameTag(graphics, yard, yard.parentB(), actorB, partialTick);
            }
        }
    }

    private void drawYardNameTag(
            GuiGraphics graphics,
            DaycareYardPayload yard,
            DaycareParentPayload parent,
            YardActorState actor,
            float partialTick
    ) {
        String label = yardDisplayName(parent);
        if (label.isBlank() || "-".equals(label)) return;

        DaycareLayout.Rect area = layout.yardRect(yard.yardIndex());
        DaycareLayout.Point anchor = layout.yardParentAnchor(yard.yardIndex(), actor.parentB);
        double renderX = actorScreenX(area, actor.parentB, actor.xNorm);
        double renderY = anchor.y() + actorBobOffset(actor, partialTick) + (actor.behavior == YardBehavior.SLEEP ? 4 : 0);
        String fitted = DaycareUiText.fit(label, area.width() - 8, value -> stockWidth(font, value, false));
        int width = stockWidth(font, fitted, false) + 6;
        int height = font.lineHeight + 1;
        int x = Math.max(area.x() + 1, Math.min((int) Math.round(renderX) - width / 2, area.x() + area.width() - width - 1));
        int y = Math.max(area.y() + 1, Math.min((int) Math.round(renderY) - YARD_NAME_TAG_Y_OFFSET, area.y() + area.height() - height - 1));
        graphics.fill(x, y, x + width, y + height, 0xB0202125);
        drawStockUnclipped(graphics, fitted, x + 3, y + 1, 0xFFF6F2F5, 1.0F, false);
    }

    private String yardDisplayName(DaycareParentPayload parent) {
        return trimLabelSafe(prettySpecies(parent.speciesId()), YARD_NAME_TAG_MAX_CHARS);
    }


    private void drawYardHeadersAndLockedInfo(GuiGraphics graphics) {
        Font font = this.font;

        for (DaycareYardPayload yard : payload.yards()) {
            DaycareLayout.Point title = layout.yardTitleCenter(yard.yardIndex());

            int titleY = title.y();
            if (yard.yardIndex() >= 2) { // yard 3 y 4
                titleY -= 2;
            }

            drawStockCentered(graphics, "Yard " + (yard.yardIndex() + 1), title.x(), titleY,
                    layout.yardRect(yard.yardIndex()).width() / 2, 0xFFF5F0F4);

            if (!yard.unlocked()) {
                DaycareLayout.Point cost = layout.yardLockCostCenter(yard.yardIndex());
                drawStockCentered(graphics, translated("price", yard.unlockCost()).getString(), cost.x(), cost.y(),
                        layout.yardRect(yard.yardIndex()).width() - 8, 0xFFE9DDE3
                );
            }
        }
    }



    private void drawYardWallpapers(GuiGraphics graphics) {
        for (int yardIndex = 0; yardIndex < 4; yardIndex++) {
            DaycareLayout.Rect rect = layout.yardRect(yardIndex);

            graphics.enableScissor(
                    rect.x(),
                    rect.y(),
                    rect.x() + rect.width(),
                    rect.y() + rect.height()
            );

            ResourceLocation wallpaper = YARD_WALLPAPER_TEXTURES[yardIndex % YARD_WALLPAPER_TEXTURES.length];

            blitScaledToRect(
                    graphics,
                    wallpaper,
                    rect.x(),
                    rect.y(),
                    rect.width(),
                    rect.height(),
                    BIOME_WALLPAPER_WIDTH,
                    BIOME_WALLPAPER_HEIGHT
            );

            graphics.disableScissor();
        }
    }

    private void blitScaledToRect(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int targetWidth,
            int targetHeight,
            int sourceWidth,
            int sourceHeight
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(
                targetWidth / (float) sourceWidth,
                targetHeight / (float) sourceHeight,
                1.0F
        );
        graphics.blit(texture, 0, 0, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        graphics.pose().popPose();
    }

    private void drawYardText(GuiGraphics graphics, DaycareYardPayload yard) {
        Font font = this.font;
        DaycareLayout.Rect rect = layout.yardRect(yard.yardIndex());
        int titleX = rect.x() + layout.scaled(5);
        int titleY = rect.y() + layout.scaled(4);
        graphics.drawString(font, Component.literal("Yard " + (yard.yardIndex() + 1)), titleX, titleY, 0xFFF5F0F4, false);

        if (!yard.unlocked()) {
            graphics.drawString(font, "Locked", rect.x() + layout.scaled(7), rect.y() + layout.scaled(26), 0xFFF2C8D1, false);
            graphics.drawString(font, translated("price", yard.unlockCost()).getString(), rect.x() + layout.scaled(7), rect.y() + layout.scaled(34), 0xFFD7CDD2, false);
            return;
        }

        if (yard.parentA() == null && yard.parentB() == null) {
            graphics.drawString(font, "Empty yard", rect.x() + layout.scaled(7), rect.y() + layout.scaled(27), 0xFFDAD3D8, false);
            graphics.drawString(font, "Click with party mon", rect.x() + layout.scaled(7), rect.y() + layout.scaled(35), 0xFFBDB3BA, false);
            return;
        }

        if (yard.parentA() != null) {
            graphics.drawString(font, trimLabelSafe(prettySpecies(yard.parentA().speciesId()), 10), rect.x() + layout.scaled(4), rect.y() + layout.scaled(18), 0xFFF3EDF1, false);
            drawGenderIcon(graphics, yard.parentA().gender(), rect.x() + layout.scaled(5), rect.y() + layout.scaled(25));
        }
        if (yard.parentB() != null) {
            int rightTextX = rect.x() + layout.scaled(39);
            graphics.drawString(font, trimLabelSafe(prettySpecies(yard.parentB().speciesId()), 10), rightTextX, rect.y() + layout.scaled(18), 0xFFF3EDF1, false);
            drawGenderIcon(graphics, yard.parentB().gender(), rightTextX, rect.y() + layout.scaled(25));
        }
        if (yard.parentA() != null && yard.parentB() != null) {
            graphics.drawString(font, "<3", rect.x() + layout.scaled(34), rect.y() + layout.scaled(24), 0xFFF08BAA, false);
        }

        String status = trimLabelSafe(statusLabel(yard), 20);
        graphics.drawString(font, status, rect.x() + layout.scaled(4), rect.y() + layout.scaled(39), 0xFFE9E1E5, false);
    }

    private void drawEggTray(GuiGraphics graphics) {
        Font font = this.font;
        List<DaycareEggTrayPayload> eggs = payload.eggTray();
        DaycareLayout.Rect tray = layout.trayRect();

        if (eggs.isEmpty()) {
            graphics.drawString(font, "No eggs", tray.x() + layout.scaled(42), tray.y() + layout.scaled(8), 0xFFAAA2A7, false);
        }

        for (int i = 0; i < eggs.size() && i < 14; i++) {
            DaycareLayout.Rect cell = layout.eggCellRect(i);
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(), 0x18000000);
            renderEggSprite(graphics, cell.centerX() - layout.scaled(6), cell.centerY() - layout.scaled(6), trayEggAppearances.get(i));
        }

        graphics.drawString(
            font,
            eggs.size() + "/" + payload.eggTrayCapacity(),
            tray.x() + tray.width() - layout.scaled(19),
            tray.y() - layout.scaled(6),
            0xFFE9DDE3,
            false
        );
    }

    private void drawDetailsPanel(GuiGraphics graphics) {
        Font font = this.font;
        DaycareYardPayload yard = selectedYard();
        DaycareLayout.Rect details = layout.detailsTextRect();
        DaycareLayout.Rect bottom = layout.detailsBottomRect();
        int x = details.x() + layout.scaled(3);
        int y = details.y() + layout.scaled(4);

        if (yard == null) {
            graphics.drawString(font, "Select a yard", x, y, 0xFFF0E6EB, false);
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("State: " + prettyToken(yard.visualState()));
        lines.add("Breed: " + (yard.compatible() ? "Yes" : "No"));
        lines.add("Why: " + prettyReason(yard.compatibilityReason()));
        lines.add("A: " + parentName(yard.parentA()));
        lines.add("B: " + parentName(yard.parentB()));
        lines.add("Groups: " + eggGroups(yard.parentA()));
        lines.add("       " + eggGroups(yard.parentB()));
        if (yard.eggPreview() != null) {
            DaycareEggPreviewPayload egg = yard.eggPreview();
            lines.add("Child: " + trimLabelSafe(prettySpecies(egg.childSpeciesId()) + formSuffix(egg.childFormId()), 16));
            lines.add("Nature: " + prettyToken(egg.natureId()));
            lines.add("Ability: " + trimLabelSafe(prettyToken(egg.abilityId()), 15));
            lines.add("Ball: " + prettyToken(egg.ballId()));
            lines.add("Moves: " + trimLabelSafe(egg.moveIds().isEmpty() ? "None" : joinPretty(egg.moveIds()), 18));
        }
        lines.add("IVs: " + yard.inheritedIvCount() + " inherited");
        lines.add("Timer: " + remainingLabel(yard.remainingMs()));

        int currentY = y;
        for (String line : lines) {
            currentY = drawWrapped(graphics, font, line, x, currentY, details.width() - layout.scaled(6), 0xFFECE6EA) + 1;
            if (currentY >= details.y() + details.height() - layout.scaled(6)) {
                break;
            }
        }

        graphics.drawString(font, "Yard eggs", bottom.x() + layout.scaled(7), bottom.y() + layout.scaled(8), 0xFFECE6EA, false);
        graphics.drawString(
            font,
            payload.eggTray().isEmpty() ? "Tray empty" : payload.eggTray().size() + " waiting",
            bottom.x() + layout.scaled(7),
            bottom.y() + layout.scaled(16),
            0xFFC8C0C6,
            false
        );
        drawProgressBar(graphics, bottom.x() + layout.scaled(6), bottom.y() + layout.scaled(28), bottom.width() - layout.scaled(12), layout.scaled(3), yard.progress());
    }

    private void drawYardEggSummaries(GuiGraphics graphics) {
        for (DaycareYardPayload yard : payload.yards()) {
            if (!yard.unlocked() || yard.eggCount() <= 0) {
                continue;
            }

            DaycareLayout.Rect slot = layout.yardEggSummaryRect(yard.yardIndex());
            renderEggSprite(graphics, slot.x(), slot.y(), firstYardEggAppearances.getOrDefault(yard.yardIndex(), NurseryEggAppearance.NORMAL));
            if (yard.eggCount() > 1) {
                drawEggCountOverlay(graphics, "x" + yard.eggCount(), slot.x() + layout.scaled(6), slot.y() + layout.scaled(8));
            }
        }
    }

    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        graphics.fill(x, y, x + width, y + height, 0xFF5A535A);
        int fill = Math.max(0, Math.min(width - 2, Math.round((width - 2) * progress)));
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + height - 1, 0xFFF08BAA);
    }

    private void drawYardHeartLoaders(GuiGraphics graphics) {
        for (DaycareYardPayload yard : payload.yards()) {
            if (!shouldRenderHeartLoader(yard)) {
                continue;
            }

            ResourceLocation texture = heartTextureForProgress(yard.progress(), yard.visualState());
            if (texture == null) {
                continue;
            }

            DaycareLayout.Point anchor = layout.yardHeartAnchor(yard.yardIndex());

            graphics.blit(
                    texture,
                    anchor.x(),
                    anchor.y(),
                    0,
                    0,
                    YARD_HEART_WIDTH,
                    YARD_HEART_HEIGHT,
                    YARD_HEART_WIDTH,
                    YARD_HEART_HEIGHT
            );
        }
    }

    private boolean shouldRenderHeartLoader(DaycareYardPayload yard) {
        if (!yard.unlocked() || yard.parentA() == null || yard.parentB() == null || !yard.compatible()) {
            return false;
        }

        return switch (yard.visualState()) {
            case "BREEDING", "PAUSED_NO_SPACE" -> true;
            default -> false;
        };
    }

    private ResourceLocation heartTextureForProgress(float progress, String visualState) {
        if ("PAUSED_NO_SPACE".equals(visualState) || progress >= 1.0F) {
            return YARD_HEART_TEXTURES[YARD_HEART_TEXTURES.length - 1];
        }
        if (progress <= 0.0F) {
            return YARD_HEART_TEXTURES[0];
        }

        int index = Math.min(
                YARD_HEART_TEXTURES.length - 1,
                Math.max(0, (int) Math.floor(progress * YARD_HEART_TEXTURES.length))
        );
        return YARD_HEART_TEXTURES[index];
    }

    private void renderPartyPokemon(GuiGraphics graphics, DaycarePartyPokemonPayload slot, float partialTick) {
        DaycareLayout.Point anchor = layout.partyPokemonAnchor(slot.slotIndex());
        double bobY = Math.sin((System.currentTimeMillis() + (slot.slotIndex() * 91L)) / 360.0D) * 0.7D;
        renderPokemonModel(
            graphics,
            "party:" + slot.slotIndex(),
            slot.speciesId(),
            slot.formId(),
            anchor.x(),
            anchor.y() + bobY,
            PARTY_RENDER_SCALE,
            -18.0F,
            7.0F,
            partialTick
        );
    }

    private void drawPcPartySlot(GuiGraphics graphics, DaycarePartyPokemonPayload slot, float partialTick) {
        DaycareLayout.Rect slotRect = NurseryPcLayout.partySlot(slot.slotIndex());
        renderPokemonInPcPartySlot(graphics, slot, slotRect, partialTick);
        drawPcPartySlotLevel(graphics, slot, slotRect);
        drawPcPartySlotGender(graphics, slot, slotRect);
        drawPcPartySlotHeldItem(graphics, slot, slotRect);

        if (selectedPartySlot == slot.slotIndex()) {
            drawPcPartySlotPointer(graphics, slotRect);
        }
    }

    private void renderPokemonInPcPartySlot(
        GuiGraphics graphics,
        DaycarePartyPokemonPayload slot,
        DaycareLayout.Rect slotRect,
        float partialTick
    ) {
        RenderablePokemon renderablePokemon = createPartySlotRenderablePokemon(slot);
        PokemonRenderState renderState = renderStates.computeIfAbsent(
            "party_state:" + slot.slotIndex(),
            key -> new PokemonRenderState(slot.speciesId(), slot.formId(), null, new FloatingState())
        );
        if (renderablePokemon == null) {
            return;
        }

        float slotScale = Math.min(slotRect.width(), slotRect.height()) / 25.0F;
        float visualScale = slotScale >= 1.0F
            ? slotScale
            : slotScale + ((1.0F - slotScale) * PC_PARTY_RENDER_SIZE_RECOVERY);
        var matrices = graphics.pose();
        matrices.pushPose();
        matrices.translate(
            slotRect.x() + (slotRect.width() / 2.0D),
            slotRect.y() + 1.0F,
            0.0F
        );
        matrices.scale(PC_PARTY_RENDER_POSE_SCALE * visualScale, PC_PARTY_RENDER_POSE_SCALE * visualScale, 1.0F);

        Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(
            new Quaternionf(),
            new Vector3f(13.0F, 35.0F, 0.0F)
        );

        PokemonGuiUtilsKt.drawProfilePokemon(
            renderablePokemon,
            matrices,
            rotation,
            PoseType.PROFILE,
            renderState.floatingState,
            partialTick,
            PC_PARTY_RENDER_SCALE * visualScale,
            ProfileTransformType.PROFILE,
            false,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            0.0F,
            0.0F,
            13
        );
        matrices.popPose();
    }

    private RenderablePokemon createPartySlotRenderablePokemon(DaycarePartyPokemonPayload slot) {
        Pokemon actual = slot.pokemonId() == null ? null : localPartyPokemon(slot.pokemonId());
        if (actual != null) return actual.asRenderablePokemon();
        try {
            if (slot.speciesId() == null || slot.speciesId().isBlank()) {
                return null;
            }

            Species species = PokemonSpecies.getByIdentifier(ResourceLocation.parse(slot.speciesId()));
            if (species == null) {
                return null;
            }

            FormData form = species.getFormByName(slot.formId());
            HashSet<String> aspects = form == null ? new HashSet<>() : new HashSet<>(form.getAspects());
            return new RenderablePokemon(species, aspects, heldItemStack(slot.heldItemId()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void drawPcPartySlotLevel(GuiGraphics g, DaycarePartyPokemonPayload slot, DaycareLayout.Rect rect) {
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 100);
            drawNativePcText(g, NurseryPcNativeText.partyLevel(rect.x(), rect.y()),
                    Component.translatable("cobblemon.ui.lv.number", slot.level()));
        } finally {
            g.pose().popPose();
        }
    }

    private void drawPcPartySlotGender(GuiGraphics graphics, DaycarePartyPokemonPayload slot, DaycareLayout.Rect slotRect) {
        ResourceLocation texture = switch (slot.gender() == null ? "" : slot.gender().toLowerCase(Locale.ROOT)) {
            case "male" -> PARTY_GENDER_MALE_TEXTURE;
            case "female" -> PARTY_GENDER_FEMALE_TEXTURE;
            default -> null;
        };
        if (texture == null) {
            return;
        }

        blitNativeHalf(graphics, texture, slotRect.x() + 21, slotRect.y() + 1, 6, 8);
    }

    private void drawPcPartySlotHeldItem(GuiGraphics graphics, DaycarePartyPokemonPayload slot, DaycareLayout.Rect slotRect) {
        ItemStack heldItem = heldItemStack(slot.heldItemId());
        if (heldItem.isEmpty()) {
            return;
        }

        float iconScale = 0.5F;
        graphics.pose().pushPose();
        graphics.pose().scale(iconScale, iconScale, 1.0F);
        int itemX = Math.round((slotRect.x() + 14) / iconScale);
        int itemY = Math.round((slotRect.y() + 14) / iconScale);
        graphics.renderItem(heldItem, itemX, itemY);
        graphics.pose().popPose();
    }

    private void drawPcPartySlotPointer(GuiGraphics graphics, DaycareLayout.Rect slotRect) {
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0,0,500);
            blitNativeHalf(graphics, PARTY_POINTER_TEXTURE, slotRect.x() + 10,
                    slotRect.y() - 3 - selectPointerOffsetY * .5, 11, 8);
        } finally { pose.popPose(); }
    }

    private void drawScaledTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        graphics.blit(texture, scaledX, scaledY, 0, 0, width, height, width, height);
        graphics.pose().popPose();
    }

    private void drawScaledSlotText(GuiGraphics graphics, Component text, int x, int y, float scale, int color) {
        drawStockUnclipped(graphics, text.getString(), x, y, color, 0.5F, false);
    }

    private ItemStack heldItemStack(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private void syncConfirmedLanding(OpenDaycareScreenS2CPayload fresh, boolean habitatChanged, boolean failed) {
        var move = pendingLanding;
        if (move == null) return;
        if (failed || habitatChanged || ticksElapsed >= pendingMoveUntil) { pendingLanding = null; return; }
        var yard = selectedYard();
        if (yard == null || yard.yardIndex() != move.yardIndex()) return;
        var target = move.parentB() ? yard.parentB() : yard.parentA();
        var source = partySlot(move.partySlot());
        if (source != null && NurseryPcLayout.landingConfirmed(move, yard.yardIndex(), fresh.messageSuccess(),
                source.pokemonId(), target == null ? null : target.pokemonId())) {
            var actor = getOrCreateYardActorState(yard, move.parentB(), target);
            actor.landing = new NurseryLandingMotion(net.minecraft.Util.getMillis());
            pendingLanding = null;
        }
    }

    private void renderYardPokemon(GuiGraphics graphics, DaycareYardPayload yard, float partialTick) {
        YardActorState actorA = yard.parentA() == null ? null : getOrCreateYardActorState(yard, false, yard.parentA());
        YardActorState actorB = yard.parentB() == null ? null : getOrCreateYardActorState(yard, true, yard.parentB());

        if (yard.parentA() != null && actorA != null && !isHeldParent(yard, yard.parentA(), false)) {
            renderYardPokemonActor(graphics, yard, yard.parentA(), actorA, actorB, partialTick);
        }
        if (yard.parentB() != null && actorB != null && !isHeldParent(yard, yard.parentB(), true)) {
            renderYardPokemonActor(graphics, yard, yard.parentB(), actorB, actorA, partialTick);
        }
        drawStageHeldItem(graphics, yard, yard.parentA(), actorA, false);
        drawStageHeldItem(graphics, yard, yard.parentB(), actorB, true);
        updateYardCry(yard, actorA, actorB);
    }

    private void drawStageHeldItem(GuiGraphics g, DaycareYardPayload yard, DaycareParentPayload parent,
                                   YardActorState actor, boolean sideB) {
        if (parent == null || actor == null || isHeldParent(yard, parent, sideB)) return;
        if (parent.heldItemId() == null || parent.heldItemId().isBlank()) return;
        var anchor = NurseryPcLayout.stageHeldIcon(actor.projected);
        if (anchor == null) return; // No visible model resolved; do not invent a static position.
        if (!parent.heldItemId().equals(actor.stageHeldItemId)) {
            actor.stageHeldItemId = parent.heldItemId();
            actor.stageHeldStack = heldItemStack(actor.stageHeldItemId);
        }
        if (actor.stageHeldStack.isEmpty()) return;
        // Draw in the real clipped stage after geometry capture, so the item does not affect
        // fit/picking, or render twice in the CPU model probe.
        RenderHelperKt.renderScaledGuiItemIcon(actor.stageHeldStack, anchor.x(), anchor.y(),
                anchor.scale(), 0F, g.pose());
    }

    private record YardCry(int yard, UUID id, FloatingState state, BedrockActiveAnimation animation,
                           ResourceLocation sound, float at, long until,
                           net.minecraft.client.resources.sounds.SoundInstance playing) {}

    @Override
    public void removed() {
        stopYardCry();
        super.removed();
    }

    private void stopYardCry() {
        if (runningCry == null) return;
        runningCry.state().getActiveAnimations().remove(runningCry.animation());
        if (runningCry.playing() != null && minecraft != null) minecraft.getSoundManager().stop(runningCry.playing());
        runningCry = null;
    }

    private void updateYardCry(DaycareYardPayload yard, YardActorState a, YardActorState b) {
        if (minecraft == null || minecraft.screen != this) return;
        long now = net.minecraft.Util.getMillis();
        var left = yard.parentA();
        var right = yard.parentB();
        boolean visibleA = left != null && a != null && a.projected != null && a.behavior != YardBehavior.SLEEP
                && (a.landing == null || !a.landing.ongoing(now)) && !isHeldParent(yard, left, false);
        boolean visibleB = right != null && b != null && b.projected != null && b.behavior != YardBehavior.SLEEP
                && (b.landing == null || !b.landing.ongoing(now)) && !isHeldParent(yard, right, true);
        if (runningCry != null) {
            var cry = runningCry;
            if (cry.yard() != yard.yardIndex()
                    || !(visibleA && cry.id().equals(left.pokemonId()) || visibleB && cry.id().equals(right.pokemonId()))) {
                stopYardCry();
            } else if (cry.state().getAnimationSeconds() >= cry.at() && cry.playing() == null) {
                var instance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(cry.sound()), 1F, .55F);
                minecraft.getSoundManager().play(instance);
                runningCry = new YardCry(cry.yard(), cry.id(), cry.state(), cry.animation(), cry.sound(),
                        cry.at(), cry.until(), instance);
            } else if (now >= cry.until() && cry.playing() != null && !minecraft.getSoundManager().isActive(cry.playing())) {
                stopYardCry();
            }
            return;
        }
        if (!habitatCry.ready(yard.yardIndex(), visibleA || visibleB, now)) return;
        if (habitatCry.firstSide() == 0) {
            if (!(visibleA && startYardCry(yard, left, false, now)) && visibleB) startYardCry(yard, right, true, now);
        } else {
            if (!(visibleB && startYardCry(yard, right, true, now)) && visibleA) startYardCry(yard, left, false, now);
        }
    }

    private boolean startYardCry(DaycareYardPayload yard, DaycareParentPayload parent, boolean sideB, long now) {
        var visual = renderStates.get(yard.yardIndex() + (sideB ? ":B" : ":A"));
        if (visual == null) return false;
        var state = visual.floatingState;
        var model = state.getCurrentModel();
        if (model == null) return false;

        com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation stock;
        try {
            stock = model.getAnimation(state, "cry", state.getRuntime());
            if (stock == null) stock = model.getCryAnimation().invoke(state);
        } catch (RuntimeException invalidThirdPartyCry) {
            return false; // A missing/malformed optional resource must not break this screen.
        }
        if (stock instanceof com.cobblemon.mod.common.client.render.models.blockbench.animation.PrimaryAnimation primary)
            stock = primary.getAnimation();
        if (!(stock instanceof BedrockActiveAnimation bedrock)) return false;
        var motion = bedrock.getAnimation();
        if (motion.getShouldLoop() || !Double.isFinite(motion.getAnimationLength()) || motion.getAnimationLength() <= 0) return false;

        // Keep the exact native bone timelines and the resolver's own form-aware sound. Use
        // one managed local sound instead of running arbitrary model effects or double-playing.
        var manager = minecraft.getSoundManager();
        var voice = motion.getEffects().stream()
                .filter(com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockSoundKeyframe.class::isInstance)
                .map(com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockSoundKeyframe.class::cast)
                .filter(key -> manager.getSoundEvent(key.getSound()) != null)
                .findFirst().orElse(null);
        ResourceLocation event;
        if (voice != null) event = voice.getSound();
        else {
            var species = visual.renderablePokemon.getSpecies().getResourceIdentifier();
            String fallback = NurseryHabitatFx.registeredCry(species.getPath(), visual.formId, path ->
                    manager.getSoundEvent(ResourceLocation.fromNamespaceAndPath(species.getNamespace(), path)) != null);
            if (fallback == null) return false;
            event = ResourceLocation.fromNamespaceAndPath(species.getNamespace(), fallback);
        }
        var bones = motion.copy(false, motion.getAnimationLength(), java.util.List.of(), motion.getBoneTimelines());
        var animation = new BedrockActiveAnimation(bones);
        state.addActiveAnimation(animation, ignored -> kotlin.Unit.INSTANCE);
        runningCry = new YardCry(yard.yardIndex(), parent.pokemonId(), state, animation, event,
                animation.getStartedSeconds() + (voice == null ? 0 : Math.max(0, voice.getSeconds())),
                now + Math.max(1000, Math.round(motion.getAnimationLength()*1000)), null);
        return true;
    }

    private boolean isHeldParent(DaycareYardPayload yard, DaycareParentPayload parent, boolean sideB) {
        return armed != null && !armed.fromParty() && armed.yard() == yard.yardIndex()
                && armed.slot() == (sideB ? 1 : 0) && armed.id().equals(parent.pokemonId());
    }

    private NurseryPcLayout.ActorBox stageActorBox(DaycareYardPayload yard, DaycareParentPayload parent,
                                                    YardActorState actor, float partialTick) {
        String key = yard.yardIndex() + (actor.parentB ? ":B" : ":A");
        var renderState = renderStates.computeIfAbsent(key, ignored -> createRenderState(parent.speciesId(), parent.formId()));
        var profile = profileFor(renderState);
        double x = actorScreenX(NurseryPcLayout.stage(), actor.parentB, actor.xNorm);
        double bob = actorBobOffset(actor, partialTick) + (actor.behavior == YardBehavior.SLEEP ? 4 : 0);
        return NurseryPcLayout.stageModel(x, profile.x(), profile.y(), profile.scale(), bob);
    }

    private record ProfileShape(double x, double y, double scale) {}

    private static ProfileShape profileFor(PokemonRenderState renderState) {
        if (renderState == null || renderState.renderablePokemon == null) return new ProfileShape(0, 0.75, 0.65);
        if (renderState.profile != null) return renderState.profile;
        try {
            var floating = renderState.floatingState;
            var pokemon = renderState.renderablePokemon;
            var model = floating.getCurrentModel();
            if (model == null) {
                floating.setCurrentAspects(pokemon.getAspects());
                model = VaryingModelRepository.INSTANCE.getPoser(pokemon.getSpecies().getResourceIdentifier(), floating);
            }
            var offset = model.getProfileTranslation();
            double scale = model.getProfileScale();
            if (Double.isFinite(offset.x) && Double.isFinite(offset.y) && Double.isFinite(scale) && scale > 0) {
                return renderState.profile = new ProfileShape(offset.x, offset.y, scale);
            }
        } catch (RuntimeException ignored) {
            // Use the proven stage anchor for a missing resource or a custom poser without PROFILE data.
        }
        return new ProfileShape(0, 0.75, 0.65);
    }

    private void renderYardPokemonActor(
        GuiGraphics graphics,
        DaycareYardPayload yard,
        DaycareParentPayload parent,
        YardActorState actor,
        YardActorState partner,
        float partialTick
    ) {
        var placement = stageActorBox(yard, parent, actor, partialTick);

        float rotationY = actorRotationY(actor, yard, partner);
        float tilt = switch (actor.behavior) {
            case WALK -> YARD_TILT_WALK;
            case SLEEP -> YARD_TILT_SLEEP;
            default -> "BREEDING".equals(yard.visualState()) ? 9.0F : YARD_TILT_IDLE;
        };
        PoseType poseType = switch (actor.behavior) {
            case WALK -> PoseType.WALK;
            case SLEEP -> PoseType.SLEEP;
            default -> PoseType.STAND;
        };

        var stage = NurseryPcLayout.stage();
        double left = stage.x() + 4, right = stage.x()+stage.width()-4;
        if (yard.parentA() != null && yard.parentB() != null) {
            if (actor.parentB) left = stage.centerX()+2;
            else right = stage.centerX()-2;
        }
        double ground = stage.y()+stage.height()-9;
        long renderMs = net.minecraft.Util.getMillis();
        boolean falling = actor.landing != null && actor.landing.ongoing(renderMs);
        double step = !falling && actor.behavior == YardBehavior.WALK ? Math.max(0, -actorBobOffset(actor,partialTick)) : 0;
        double height = ground - stage.y() - 20; // Preserve the existing steady model size.
        actor.projected = null;
        var fit = new NurseryGhostProjection.Stage(pcFrame.screenX(placement.projectedX()),
                pcFrame.screenY(ground-step),pcFrame.screenX(left),pcFrame.screenX(right),
                height*pcFrame.scale(),pcFrame.screenY(stage.y()+2),pcFrame.scale(),renderMs,actor.landing,
                box -> actor.projected = new NurseryGhostProjection.StageBounds(
                        pcFrame.localX(box.x0()),pcFrame.localY(box.y0()),pcFrame.localX(box.x1()),pcFrame.localY(box.y1())));
        NurseryGhostProjection.fitStage(fit, () -> renderPokemonModel(
                graphics,
                yard.yardIndex() + (actor.parentB ? ":B" : ":A"),
                parent.speciesId(),
                parent.formId(),
                placement.anchorX(),
                placement.anchorY(),
                YARD_RENDER_SCALE,
                rotationY,
                tilt,
                poseType,
                partialTick
        ));
    }

    private void tickYardActors() {
        if (layout == null) {
            return;
        }

        HashSet<String> expected = new HashSet<>();
        for (DaycareYardPayload yard : payload.yards()) {
            YardActorState actorA = yard.parentA() == null ? null : getOrCreateYardActorState(yard, false, yard.parentA());
            YardActorState actorB = yard.parentB() == null ? null : getOrCreateYardActorState(yard, true, yard.parentB());

            if (actorA != null) {
                expected.add(actorA.key);
                tickYardActor(yard, actorA, actorB);
            }
            if (actorB != null) {
                expected.add(actorB.key);
                tickYardActor(yard, actorB, actorA);
            }
        }

        yardActorStates.entrySet().removeIf(entry -> !expected.contains(entry.getKey()));
    }

    private YardActorState getOrCreateYardActorState(DaycareYardPayload yard, boolean parentB, DaycareParentPayload parent) {
        String key = yardActorKey(yard.yardIndex(), parentB);
        String signature = parent.pokemonId() + "|" + parent.speciesId() + "|" + parent.formId();
        YardActorState existing = yardActorStates.get(key);
        if (existing != null && existing.signature.equals(signature)) {
            return existing;
        }
        renderStates.remove(yard.yardIndex() + (parentB ? ":B" : ":A"));

        long seed = computeYardActorSeed(yard.yardIndex(), parentB, signature);
        Random random = new Random(seed);
        YardActorState created = new YardActorState(key, signature, yard.yardIndex(), parentB, seed, random);
        created.xNorm = randomRange(random, 0.20D, 0.90D);
        created.targetXNorm = created.xNorm;
        created.facingRight = random.nextBoolean();
        created.behavior = YardBehavior.IDLE;
        created.behaviorTicksRemaining = 20 + random.nextInt(25);
        created.motionPhase = random.nextDouble() * Math.PI * 2.0D;
        yardActorStates.put(key, created);
        return created;
    }

    private void tickYardActor(DaycareYardPayload yard, YardActorState actor, YardActorState partner) {
        if (actor.landing != null) {
            if (actor.landing.ongoing(net.minecraft.Util.getMillis())) return;
            actor.landing = null;
        }
        actor.behaviorTicksRemaining--;
        if (actor.behaviorTicksRemaining <= 0) {
            chooseNextYardBehavior(actor, yard, partner);
        }

        switch (actor.behavior) {
            case WALK -> tickWalkingActor(actor);
            case LOOK_AT_PARTNER -> {
                if (partner != null) {
                    actor.facingRight = actorGlobalX(NurseryPcLayout.stage(), actor.parentB, actor.xNorm)
                        < actorGlobalX(NurseryPcLayout.stage(), partner.parentB, partner.xNorm);
                }
                actor.motionPhase += 0.08D;
            }
            case SLEEP -> actor.motionPhase += 0.05D;
            case IDLE -> actor.motionPhase += 0.10D;
        }
    }

    private void tickWalkingActor(YardActorState actor) {
        double delta = actor.targetXNorm - actor.xNorm;
        double direction = Math.signum(delta);
        if (Math.abs(delta) <= YARD_WALK_SPEED) {
            actor.xNorm = actor.targetXNorm;
            actor.behaviorTicksRemaining = Math.min(actor.behaviorTicksRemaining, 8);
        } else {
            actor.xNorm += direction * YARD_WALK_SPEED;
            actor.facingRight = direction > 0.0D;
        }
        actor.motionPhase += 0.42D;
    }

    private void chooseNextYardBehavior(YardActorState actor, DaycareYardPayload yard, YardActorState partner) {
        boolean breeding = "BREEDING".equals(yard.visualState()) || "PAUSED_NO_SPACE".equals(yard.visualState());
        double walkChance = breeding ? 0.42D : 0.52D;
        double lookChance = partner != null ? (breeding ? 0.28D : 0.18D) : 0.0D;
        double sleepChance = breeding ? 0.14D : 0.08D;
        double roll = actor.random.nextDouble();

        if (roll < walkChance) {
            actor.behavior = YardBehavior.WALK;
            actor.targetXNorm = randomWalkTarget(actor, breeding);
            actor.behaviorTicksRemaining = 40 + actor.random.nextInt(50);
            return;
        }

        if (roll < walkChance + lookChance) {
            actor.behavior = YardBehavior.LOOK_AT_PARTNER;
            actor.behaviorTicksRemaining = 28 + actor.random.nextInt(26);
            actor.facingRight = partner != null
                && actorGlobalX(NurseryPcLayout.stage(), actor.parentB, actor.xNorm)
                < actorGlobalX(NurseryPcLayout.stage(), partner.parentB, partner.xNorm);
            return;
        }

        if (roll < walkChance + lookChance + sleepChance) {
            actor.behavior = YardBehavior.SLEEP;
            actor.behaviorTicksRemaining = 70 + actor.random.nextInt(90);
            return;
        }

        actor.behavior = YardBehavior.IDLE;
        actor.behaviorTicksRemaining = 24 + actor.random.nextInt(50);
        if (actor.random.nextFloat() < 0.35F) {
            actor.facingRight = !actor.facingRight;
        }
    }

    private double randomWalkTarget(YardActorState actor, boolean breeding) {
        if (breeding && actor.random.nextFloat() < 0.45F) {
            return actor.parentB
                    ? randomRange(actor.random, 0.08D, 0.44D)
                    : randomRange(actor.random, 0.56D, 0.92D);
        }
        return randomRange(actor.random, 0.08D, 0.92D);
    }

    private double actorBobOffset(YardActorState actor, float partialTick) {
        double phase = actor.motionPhase + (partialTick * (actor.behavior == YardBehavior.WALK ? 0.42D : 0.10D));
        return switch (actor.behavior) {
            case WALK -> Math.sin(phase) * 0.85D;
            case LOOK_AT_PARTNER -> Math.sin(phase) * 0.20D;
            case SLEEP -> Math.sin(phase) * 0.12D;
            case IDLE -> Math.sin(phase) * 0.35D;
        };
    }

    private float actorRotationY(YardActorState actor, DaycareYardPayload yard, YardActorState partner) {
        if (actor.behavior == YardBehavior.LOOK_AT_PARTNER && partner != null) {
            boolean shouldFaceRight = actorGlobalX(NurseryPcLayout.stage(), actor.parentB, actor.xNorm)
                < actorGlobalX(NurseryPcLayout.stage(), partner.parentB, partner.xNorm);
            return shouldFaceRight ? YARD_YAW_RIGHT : YARD_YAW_LEFT;
        }
        return actor.facingRight ? YARD_YAW_RIGHT : YARD_YAW_LEFT;
    }

    private double actorScreenX(DaycareLayout.Rect yardRect, boolean parentB, double xNorm) {
        double min = parentB
            ? yardRect.x() + (yardRect.width() / 2.0D) + YARD_CENTER_PADDING
            : yardRect.x() + YARD_SIDE_PADDING;
        double max = parentB
            ? yardRect.x() + yardRect.width() - YARD_SIDE_PADDING
            : yardRect.x() + (yardRect.width() / 2.0D) - YARD_CENTER_PADDING;
        return min + ((max - min) * xNorm);
    }

    private double actorGlobalX(DaycareLayout.Rect yardRect, boolean parentB, double xNorm) {
        return actorScreenX(yardRect, parentB, xNorm);
    }

    private long computeYardActorSeed(int yardIndex, boolean parentB, String signature) {
        long seed = 0x9E3779B97F4A7C15L;
        seed ^= ((long) yardIndex + 1L) * 0x632BE59BD9B4E019L;
        seed ^= parentB ? 0x94D049BB133111EBL : 0x2545F4914F6CDD1DL;
        seed ^= signature.hashCode();
        return seed;
    }

    private String yardActorKey(int yardIndex, boolean parentB) {
        return "yard_actor:" + yardIndex + ':' + (parentB ? 'B' : 'A');
    }

    private double randomRange(Random random, double min, double max) {
        return min + ((max - min) * random.nextDouble());
    }

    private void renderDetailsEggPreview(GuiGraphics graphics, DaycareEggPreviewPayload egg, float partialTick) {
        DaycareLayout.Point center = layout.detailsPreviewCenter();
        renderPokemonModel(
            graphics,
            "details:" + egg.childSpeciesId() + ":" + egg.childFormId(),
            egg.childSpeciesId(),
            egg.childFormId(),
            center.x(),
            center.y() - 20 + Math.sin(System.currentTimeMillis() / 420.0D) * 1.1D,
            DETAILS_RENDER_SCALE,
            -14.0F,
            6.0F,
            partialTick
        );
    }

    private void renderPokemonModel(
        GuiGraphics graphics,
        String cacheKey,
        String speciesId,
        String formId,
        double centerX,
        double baseY,
        float scale,
        float rotationY,
        float tilt,
        float partialTick
    ) {
        renderPokemonModel(graphics, cacheKey, speciesId, formId, centerX, baseY, scale, rotationY, tilt, PoseType.PROFILE, partialTick);
    }

    private void renderPokemonModel(
        GuiGraphics graphics,
        String cacheKey,
        String speciesId,
        String formId,
        double centerX,
        double baseY,
        float scale,
        float rotationY,
        float tilt,
        PoseType poseType,
        float partialTick
    ) {
        var cache = cacheKey.startsWith("help:") ? helpRenderStates : renderStates;
        PokemonRenderState renderState = cache.computeIfAbsent(cacheKey, key -> createRenderState(speciesId, formId));
        if (renderState == null) {
            return;
        }

        var matrices = graphics.pose();
        matrices.pushPose();
        matrices.translate(centerX, baseY, 0.0D);
        matrices.scale(scale, scale, scale);

        Quaternionf rotation = new Quaternionf().rotationXYZ(
            (float) Math.toRadians(tilt),
            (float) Math.toRadians(rotationY),
            0.0F
        );

        if (NurseryGhostProjection.fittingStage()) {
            var visual = renderState.renderablePokemon;
            var state = renderState.floatingState;
            state.setCurrentAspects(visual.getAspects());
            if (VaryingModelRepository.INSTANCE.getSprite(visual.getSpecies().getResourceIdentifier(), state,
                    com.cobblemon.mod.common.client.render.SpriteType.PROFILE) != null) {
                NurseryGhostProjection.alignStageSprite(matrices);
            }
        }

        PokemonGuiUtilsKt.drawProfilePokemon(
            renderState.renderablePokemon,
            matrices,
            rotation,
            poseType,
            renderState.floatingState,
            partialTick,
            18.0F,
            ProfileTransformType.PROFILE,
            false,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            0.0F,
            0.0F,
            13
        );
        matrices.popPose();
    }

    private PokemonRenderState createRenderState(String speciesId, String formId) {
        try {
            Species species = PokemonSpecies.getByIdentifier(ResourceLocation.parse(speciesId));
            if (species == null) {
                return null;
            }
            FormData form = species.getFormByName(formId);
            HashSet<String> aspects = form == null ? new HashSet<>() : new HashSet<>(form.getAspects());
            return new PokemonRenderState(
                speciesId,
                formId,
                new RenderablePokemon(species, aspects, ItemStack.EMPTY),
                new FloatingState()
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private void refreshEggAppearances() {
        firstYardEggAppearances.clear();
        trayEggAppearances = payload.eggTray().stream().map(egg -> {
            NurseryEggAppearance appearance = NurseryEggAppearanceResolver.resolve(egg.childSpeciesId(), egg.childFormId());
            firstYardEggAppearances.putIfAbsent(egg.sourceYardIndex(), appearance);
            return appearance;
        }).toList();
    }

    private record EggSpriteBounds(int width, int height, int x0, int y0, int x1, int y1) {}
    private final Map<ResourceLocation,EggSpriteBounds> eggSpriteBounds = new HashMap<>();

    private void renderCenteredYardEgg(GuiGraphics g, DaycareLayout.Rect slot, NurseryEggAppearance appearance) {
        ResourceLocation art = ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID,
                "textures/gui/daycare/eggs/" + appearance.slug() + ".png");
        var bounds = eggSpriteBounds.computeIfAbsent(art, this::eggBounds);
        var place = NurseryPcLayout.centerSprite(slot, 2, bounds.x0(),bounds.y0(),bounds.x1(),bounds.y1());
        var pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(place.x(), place.y(), 0);
            pose.scale((float)place.scale(), (float)place.scale(), 1F);
            g.blit(art, 0, 0, 0, 0, bounds.width(), bounds.height(), bounds.width(), bounds.height());
        } finally { pose.popPose(); }
    }

    private EggSpriteBounds eggBounds(ResourceLocation art) {
        var resource = minecraft.getResourceManager().getResource(art);
        if (resource.isPresent()) {
            try (var stream = resource.get().open(); var image = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                int w=image.getWidth(), h=image.getHeight();
                int x0=w,y0=h,x1=0,y1=0;
                for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
                    if ((image.getPixelRGBA(x,y) >>> 24) == 0) continue;
                    x0=Math.min(x0,x); y0=Math.min(y0,y);
                    x1=Math.max(x1,x+1); y1=Math.max(y1,y+1);
                }
                if (x0<x1 && y0<y1) return new EggSpriteBounds(w,h,x0,y0,x1,y1);
                return new EggSpriteBounds(w,h,0,0,w,h);
            } catch (java.io.IOException | RuntimeException ignored) {
                // Keep the standard 16x16 texture footprint if a resource override cannot be measured.
            }
        }
        return new EggSpriteBounds(16,16,0,0,16,16);
    }

    private void renderEggSprite(GuiGraphics graphics, int x, int y, NurseryEggAppearance appearance) {
        ResourceLocation sprite = ResourceLocation.fromNamespaceAndPath("cobblenursery", "textures/gui/daycare/eggs/" + appearance.slug() + ".png");
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.blit(sprite, 0, 0, 0, 0, 16, 16, 16, 16);
        graphics.pose().popPose();
    }

    private void drawEggCountOverlay(GuiGraphics graphics, String text, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        drawStockUnclipped(graphics, text, x, y, 0xFFF4F1F4, 0.5F, false);
        graphics.pose().popPose();
    }

    private void drawHoveredPokemonStats(GuiGraphics graphics, DaycarePartyPokemonPayload slot) {
        DaycareLayout.Rect panel = layout.statsPanelRect();
        int x = panel.x();
        int y = panel.y() + 2;
        int ivCenter = x + 30, evCenter = x + 47;
        drawStockCentered(graphics, "IV", ivCenter, y, 14, 0xFFFFFFFF);
        drawStockCentered(graphics, "EV", evCenter, y, 17, 0xFFFFFFFF);
        if (slot == null || !slot.occupied()) return;

        String[] labels = {"HP", "Atk", "Def", "SpA", "SpD", "Spe"};
        String[] ivs = statValues(slot, DaycarePartyPokemonPayload::hpIv, DaycarePartyPokemonPayload::attackIv,
                DaycarePartyPokemonPayload::defenceIv, DaycarePartyPokemonPayload::specialAttackIv,
                DaycarePartyPokemonPayload::specialDefenceIv, DaycarePartyPokemonPayload::speedIv);
        String[] evs = statValues(slot, DaycarePartyPokemonPayload::hpEv, DaycarePartyPokemonPayload::attackEv,
                DaycarePartyPokemonPayload::defenceEv, DaycarePartyPokemonPayload::specialAttackEv,
                DaycarePartyPokemonPayload::specialDefenceEv, DaycarePartyPokemonPayload::speedEv);
        for (int row = 0; row < labels.length; row++) {
            int line = y + (row + 1) * 8;
            drawAdaptiveText(graphics, labels[row], x + 4, line, 20, 0xFFE5E0E6, false);
            drawAdaptiveText(graphics, ivs[row], ivCenter - 7, line, 14, 0xFFFFFFFF, true);
            drawAdaptiveText(graphics, evs[row], evCenter - 9, line, 18, 0xFFFFFFFF, true);
        }
    }

    @SafeVarargs
    private final String[] statValues(DaycarePartyPokemonPayload slot, java.util.function.ToIntFunction<DaycarePartyPokemonPayload>... extractors) {
        String[] values = new String[extractors.length];
        for (int i = 0; i < extractors.length; i++) {
            values[i] = slot != null && slot.occupied() ? Integer.toString(extractors[i].applyAsInt(slot)) : "---";
        }
        return values;
    }



    private DaycarePartyPokemonPayload hoveredPartySlot(double mouseX, double mouseY) {
        int slotIndex = findPartySlot(mouseX, mouseY);
        if (slotIndex < 0) {
            return null;
        }
        return payload.partySlots().stream()
            .filter(slot -> slot.slotIndex() == slotIndex)
            .findFirst()
            .orElse(null);
    }

    private void drawGenderIcon(GuiGraphics graphics, String gender, int x, int y) {
        ResourceLocation texture = switch (gender == null ? "" : gender.toLowerCase(Locale.ROOT)) {
            case "male" -> PARTY_GENDER_MALE_TEXTURE;
            case "female" -> PARTY_GENDER_FEMALE_TEXTURE;
            default -> null;
        };
        if (texture != null) {
            graphics.blit(texture, x, y, 0, 0, 8, 8, 8, 8);
        }
    }

    private int findPartySlot(double mouseX, double mouseY) {
        for (int slotIndex = 0; slotIndex < 6; slotIndex++) {
            if (NurseryPcLayout.partySlot(slotIndex).contains(mouseX, mouseY)) {
                return slotIndex;
            }
        }
        return -1;
    }

    private int findYard(double mouseX, double mouseY) {
        for (int yardIndex = 0; yardIndex < 4; yardIndex++) {
            if (layout.yardRect(yardIndex).contains(mouseX, mouseY)) {
                return yardIndex;
            }
        }
        return -1;
    }

    private boolean isRightPanelClicked(double mouseX, double mouseY) {
        return layout.detailsPreviewRect().contains(mouseX, mouseY)
                || layout.detailsTextRect().contains(mouseX, mouseY)
                || layout.detailsBottomRect().contains(mouseX, mouseY);
    }

    private DaycareYardPayload selectedYard() {
        for (DaycareYardPayload yard : payload.yards()) {
            if (yard.yardIndex() == payload.selectedYardIndex()) {
                return yard;
            }
        }
        return payload.yards().isEmpty() ? null : payload.yards().get(0);
    }

    private String statusLabel(DaycareYardPayload yard) {
        return switch (yard.visualState()) {
            case "LOCKED" -> "Locked";
            case "EMPTY" -> "Empty";
            case "INCOMPLETE" -> "Waiting for pair";
            case "READY_TO_START" -> "Ready to start";
            case "BREEDING" -> "Breeding " + remainingLabel(yard.remainingMs());
            case "PAUSED_NO_SPACE" -> "Paused eggs full";
            case "BLOCKED" -> "Blocked";
            default -> prettyToken(yard.visualState());
        };
    }

    private String remainingLabel(long remainingMs) {
        if (remainingMs <= 0L) {
            return "ready";
        }
        long totalSeconds = Math.max(1L, remainingMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + "m " + seconds + "s";
    }

    private int drawWrapped(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, int color) {
        List<String> lines = wrap(text, maxWidth, font);
        int currentY = y;
        for (String line : lines) {
            graphics.drawString(font, line, x, currentY, color, false);
            currentY += 10;
        }
        return currentY;
    }

    private List<String> wrap(String text, int maxWidth, Font font) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String parentName(DaycareParentPayload parent) {
        return parent == null ? "-" : prettySpecies(parent.speciesId()) + formSuffix(parent.formId());
    }

    private String eggGroups(DaycareParentPayload parent) {
        return parent == null || parent.eggGroups().isEmpty() ? "-" : joinPretty(parent.eggGroups());
    }

    private String prettyReason(String reason) {
        return reason == null || reason.isBlank() ? "-" : bat.cobblenursery.text.NurseryMessages.component(reason).getString();
    }

    private String prettySpecies(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) {
            return "-";
        }
        String raw = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        return prettyToken(raw);
    }

    private String prettyToken(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String raw = value;
        if (raw.contains(":")) {
            raw = raw.substring(raw.indexOf(':') + 1);
        }
        raw = raw.replace('_', ' ').replace('-', ' ');
        String[] parts = raw.split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    private String joinPretty(List<String> values) {
        List<String> pretty = values.stream().map(this::prettyToken).toList();
        return String.join(", ", pretty);
    }

    private String trimLabelSafe(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private String trimLabel(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private String formSuffix(String formId) {
        if (formId == null || formId.isBlank() || "normal".equalsIgnoreCase(formId) || "base".equalsIgnoreCase(formId)) {
            return "";
        }
        return " (" + prettyToken(formId) + ")";
    }

    private boolean inspectionStillPresent(String signature) {
        for (var slot : payload.partySlots()) if (slot.occupied() && signature.equals(slot.speciesId() + "|" + slot.formId())) return true;
        for (var yard : payload.yards()) {
            if (yard.parentA() != null && signature.equals(yard.parentA().speciesId() + "|" + yard.parentA().formId())) return true;
            if (yard.parentB() != null && signature.equals(yard.parentB().speciesId() + "|" + yard.parentB().formId())) return true;
            var egg = yard.eggPreview();
            if (egg != null && signature.equals(egg.childSpeciesId() + "|" + egg.childFormId())) return true;
        }
        return false;
    }

    private void pruneRenderStates() {
        Map<String, String> expected = new HashMap<>();
        for (DaycarePartyPokemonPayload slot : payload.partySlots()) {
            if (slot.occupied()) {
                expected.put("party_state:" + slot.slotIndex(), slot.speciesId() + "|" + slot.formId());
            }
        }
        DaycareYardPayload selected = selectedYard();
        if (selected != null) {
            if (producingPreview(selected)) {
                var preview = selected.eggPreview();
                String signature = preview.childSpeciesId() + "|" + preview.childFormId();
                expected.put("offspring:" + signature, signature);
            }
            if (selected.eggPreview() != null) {
                expected.put(
                    "details:" + selected.eggPreview().childSpeciesId() + ":" + selected.eggPreview().childFormId(),
                    selected.eggPreview().childSpeciesId() + "|" + selected.eggPreview().childFormId()
                );
            } else if (selected.parentA() != null) {
                expected.put(
                    "details_parent:" + selected.parentA().speciesId() + ":" + selected.parentA().formId() + ":A",
                    selected.parentA().speciesId() + "|" + selected.parentA().formId()
                );
            } else if (selected.parentB() != null) {
                expected.put(
                    "details_parent:" + selected.parentB().speciesId() + ":" + selected.parentB().formId() + ":B",
                    selected.parentB().speciesId() + "|" + selected.parentB().formId()
                );
            }
        }
        for (DaycareYardPayload yard : payload.yards()) {
            if (yard.parentA() != null) {
                expected.put(yard.yardIndex() + ":A", yard.parentA().speciesId() + "|" + yard.parentA().formId());
            }
            if (yard.parentB() != null) {
                expected.put(yard.yardIndex() + ":B", yard.parentB().speciesId() + "|" + yard.parentB().formId());
            }
        }
        renderStates.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith("inspect:")) {
                return !inspectionStillPresent(entry.getKey().substring("inspect:".length()));
            }
            String expectedSignature = expected.get(entry.getKey());
            if (expectedSignature == null) {
                return true;
            }
            String actualSignature = entry.getValue().speciesId + "|" + entry.getValue().formId;
            return !expectedSignature.equals(actualSignature);
        });

        Map<String, String> expectedActors = new HashMap<>();
        for (DaycareYardPayload yard : payload.yards()) {
            if (yard.parentA() != null) {
                expectedActors.put(yardActorKey(yard.yardIndex(), false), yard.parentA().pokemonId() + "|" + yard.parentA().speciesId() + "|" + yard.parentA().formId());
            }
            if (yard.parentB() != null) {
                expectedActors.put(yardActorKey(yard.yardIndex(), true), yard.parentB().pokemonId() + "|" + yard.parentB().speciesId() + "|" + yard.parentB().formId());
            }
        }
        yardActorStates.entrySet().removeIf(entry -> {
            String expectedSignature = expectedActors.get(entry.getKey());
            return expectedSignature == null || !expectedSignature.equals(entry.getValue().signature);
        });
    }



    private enum YardBehavior {
        IDLE,
        WALK,
        SLEEP,
        LOOK_AT_PARTNER
    }

    private static final class YardActorState {
        private String stageHeldItemId;
        private ItemStack stageHeldStack;
        private final String key;
        private final String signature;
        private final int yardIndex;
        private final boolean parentB;
        private final long seed;
        private final Random random;
        private YardBehavior behavior;
        private double xNorm;
        private double targetXNorm;
        private int behaviorTicksRemaining;
        private boolean facingRight;
        private double motionPhase;
        private NurseryLandingMotion landing;
        private NurseryGhostProjection.StageBounds projected;

        private YardActorState(String key, String signature, int yardIndex, boolean parentB, long seed, Random random) {
            this.key = key;
            this.signature = signature;
            this.yardIndex = yardIndex;
            this.parentB = parentB;
            this.seed = seed;
            this.random = random;
        }
    }

    private static class TradeStyleDaycareButton extends Button {
        public TradeStyleDaycareButton(int x, int y, Component message, OnPress onPress) {
            this(x, y, 31, 10, message, onPress);
        }

        public TradeStyleDaycareButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
            // onPress handles the single custom sound for mouse and keyboard.
        }

        @Override
        public void onPress() {
            if (!active || !visible) return;
            super.onPress();
            NurseryUiSounds.click();
        }
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = active ? TRADE_BUTTON_TEXTURE : TRADE_BUTTON_DISABLED_TEXTURE;

            boolean hovered = mouseX >= getX()
                && mouseX <= getX() + width
                && mouseY >= getY()
                && mouseY <= getY() + height;

            int v = hovered && active ? TRADE_BUTTON_TEXTURE_HEIGHT : 0;

            graphics.pose().pushPose();
            graphics.pose().translate(getX(), getY(), 0.0F);
            graphics.pose().scale(
                width / (float) TRADE_BUTTON_TEXTURE_WIDTH,
                height / (float) TRADE_BUTTON_TEXTURE_HEIGHT,
                1.0F
            );
            graphics.blit(
                texture,
                0,
                0,
                0,
                v,
                TRADE_BUTTON_TEXTURE_WIDTH,
                TRADE_BUTTON_TEXTURE_HEIGHT,
                TRADE_BUTTON_TEXTURE_WIDTH,
                TRADE_BUTTON_TEXTURE_HEIGHT * 2
            );
            graphics.pose().popPose();

            Font font = Minecraft.getInstance().font;
            String text = getMessage().getString();
            float scale = DaycareUiText.stockScale(stockWidth(font, text, true), width - 4, false);
            int textHeight = Math.round(8 * scale);
            drawStockText(graphics, font, text, getX() + 2, getY() + (height - textHeight) / 2,
                    width - 4, active ? 0xFFFFFFFF : 0xFFCDC9CF, scale, true, true);
        }
    }

    private final class HopperExportButton extends Button {
        private static final ResourceLocation ON_FACE = ResourceLocation.fromNamespaceAndPath("cobblemon",
                "textures/gui/interact/request/button_request_accept.png");
        private static final ResourceLocation OFF_FACE = ResourceLocation.fromNamespaceAndPath("cobblemon",
                "textures/gui/interact/request/button_request_decline.png");
        private static final ResourceLocation ON_MARK = ResourceLocation.withDefaultNamespace("container/beacon/confirm");
        private static final ResourceLocation OFF_MARK = ResourceLocation.withDefaultNamespace("container/beacon/cancel");
        private final ItemStack icon = new ItemStack(Items.HOPPER);
        private final DaycareLayout.Rect bounds;

        HopperExportButton(DaycareLayout.Rect bounds) {
            super(bounds.x(), bounds.y(), bounds.width(), bounds.height(), Component.empty(), ignored -> {},
                    DEFAULT_NARRATION);
            this.bounds = bounds;
        }

        @Override public void onPress() {
            if (!visible || !active || armed != null || !ClientPlayNetworking.canSend(ToggleTerminalEggExportC2SPayload.TYPE)) return;
            ClientPlayNetworking.send(new ToggleTerminalEggExportC2SPayload(payload.nurseryPos(), payload.selectedYardIndex(), !hopperExportEnabled()));
        }

        @Override protected boolean clicked(double x, double y) {
            return visible && active && armed == null && NurseryPcLayout.buttonHit(bounds, x, y);
        }

        @Override public void playDownSound(SoundManager manager) { NurseryUiSounds.click(); }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = active && armed == null && (isFocused() || NurseryPcLayout.buttonHit(bounds, mouseX, mouseY));
            int x = getX(), y = getY();
            // Stock Cobblemon button sheet; use the second frame for hover.
            graphics.blit(hopperExportEnabled() ? ON_FACE : OFF_FACE, x, y, 0, NurseryPcLayout.hopperAtlasRow(hovered),
                    34, 19, 34, 38);
            graphics.renderItem(icon, x + 4, y + 1);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 200);
                // Cobblemon's exact accept/decline icons are thumbs, so keep Minecraft's existing check/X sprites.
                graphics.blitSprite(hopperExportEnabled() ? ON_MARK : OFF_MARK, x + 20, y + 2, 13, 13);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private static final class NurseryPcButton extends Button {
        private final boolean selected;
        private final NurseryPcLayout.ButtonArt art;
        private final boolean lockedTab;
        private final java.util.function.BooleanSupplier holding;

        NurseryPcButton(DaycareLayout.Rect rect, Component label, boolean selected, NurseryPcLayout.ButtonArt art,
                        boolean lockedTab, java.util.function.BooleanSupplier holding, OnPress action) {
            super(rect.x(), rect.y(), rect.width(), rect.height(), label, action, DEFAULT_NARRATION);
            this.selected = selected;
            this.art = art;
            this.lockedTab = lockedTab;
            this.holding = holding;
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
            NurseryUiSounds.click();
        }

        private boolean inBounds(double x, double y) {
            return NurseryPcLayout.buttonHit(new DaycareLayout.Rect(getX(),getY(),width,height),x,y);
        }

        @Override
        protected boolean clicked(double x, double y) {
            return active && visible && inBounds(x,y);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY();
            if (art == NurseryPcLayout.ButtonArt.HELP_NAV) {
                boolean back=getMessage().equals(helpText("previous"));
                // Use the native PC navigation atlas: normal above, hovered below.
                g.pose().pushPose();
                try {
                    g.pose().translate(x+(width-7)/2.0,y+(height-7)/2.0,0);
                    g.pose().scale(.5F,.5F,1F);
                    g.blit(pcArt(back ? "pc_arrow_previous" : "pc_arrow_next"),0,0,0,active && isHovered() ? 14 : 0,14,14,14,28);
                } finally { g.pose().popPose(); }
                return;
            }
            if (art == NurseryPcLayout.ButtonArt.HELP_DONE) {
                drawHelpDone(g,getMessage(),active && inBounds(mouseX,mouseY));
                return;
            }
            if (art == NurseryPcLayout.ButtonArt.HELP) {
                boolean hovered = art.visible(active, inBounds(mouseX, mouseY), holding.getAsBoolean());
                g.blit(pcArt("pc_release_button"), x, y, 0, NurseryPcLayout.helpAtlasRow(hovered), 58, 16, 58, 32);
                drawNativePcText(g, new NurseryPcNativeText.Draw("help", x + 29, y + 3.5,
                        1F, true, true, true, true, 0xFFFFFFFF), getMessage());
                return;
            }
            if (art.visible(active, inBounds(mouseX, mouseY), holding.getAsBoolean())) {
                if (art == NurseryPcLayout.ButtonArt.TAB) {
                    g.blit(TAB_HOVER_TEXTURE, x, y, 0, 0, width, height, width, height);
                } else {
                    var sprite = NurseryPcLayout.startHover();
                    g.blit(START_HOVER_TEXTURE, sprite.x(), sprite.y(), 0, 0,
                            sprite.width(), sprite.height(), sprite.width(), sprite.height());
                }
            }
            if (lockedTab) {
                drawPcLock(g, x + (width-10)/2, y + (height-10)/2);
                return;
            }
            var label = getMessage().copy();
            if (label.getString().isEmpty()) return;
            var preview = label.copy().withStyle(style -> style.withBold(true).withFont(nativeLargeFont()));
            boolean narrow = Minecraft.getInstance().font.width(preview) > width - 6;
            var style = NurseryPcNativeText.nurseryButton(x + width / 2.0, y + height / 2.0 - (narrow ? 2 : 4),
                    narrow, !active ? PC_SOFT : selected ? PC_MINT : 0xFFFFFFFF);
            drawNativePcText(g, style, label);
        }
    }

    private static final class PokemonRenderState {
        private final String speciesId;
        private final String formId;
        private final RenderablePokemon renderablePokemon;
        private final FloatingState floatingState;
        private ProfileShape profile;

        private PokemonRenderState(String speciesId, String formId, RenderablePokemon renderablePokemon, FloatingState floatingState) {
            this.speciesId = speciesId;
            this.formId = formId;
            this.renderablePokemon = renderablePokemon;
            this.floatingState = floatingState;
        }
    }
}
