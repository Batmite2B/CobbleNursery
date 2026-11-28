package bat.cobblemondaycare.client.screen;

final class NurseryPcLayout {
    static final int WIDTH = 349;
    static final int HEIGHT = 205;

    record Frame(int left, int top, double scale) {
        double screenX(double local) { return left + local * scale; }
        double screenY(double local) { return top + local * scale; }
        double localX(double screen) { return (screen - left) / scale; }
        double localY(double screen) { return (screen - top) / scale; }
        void undoFit(com.mojang.blaze3d.vertex.PoseStack pose) {
            float inverse = 1.0F / (float) scale;
            pose.scale(inverse, inverse, 1F);
            pose.translate(-left, -top, 0);
        }
    }

    static Frame fit(int width, int height) {
        double totalHeight = HEIGHT + EggGroupTab.HEIGHT;
        double s = Math.max(0.001, Math.min(1.0, Math.min((width - 8.0) / WIDTH, (height - 8.0) / totalHeight)));
        return new Frame((int) ((width - WIDTH * s) / 2), (int) ((height - totalHeight * s) / 2), s);
    }

    static DaycareLayout.Rect inspector() { return new DaycareLayout.Rect(6, 27, 66, 170); }
    static DaycareLayout.Rect center() { return new DaycareLayout.Rect(79, 7, 186, 184); }
    static DaycareLayout.Rect party() { return new DaycareLayout.Rect(275, 27, 71, 169); }
    static DaycareLayout.Rect stage() { return new DaycareLayout.Rect(85, 29, 174, 99); }
    static DaycareLayout.Rect status() { return new DaycareLayout.Rect(85, 140, 174, 24); }
    static DaycareLayout.Rect tab(int index) { return new DaycareLayout.Rect(79 + index * 47, 7, 45, 18); }
    static DaycareLayout.Rect partySlot(int index) {
        return new DaycareLayout.Rect(278 + (index % 2) * 31, 35 + (index / 2) * 31 + (index % 2) * 8, 25, 25);
    }

    static DaycareLayout.Rect action() { return new DaycareLayout.Rect(128, 167, 59, 23); }
    static DaycareLayout.Rect startHover() { return action(); }
    static DaycareLayout.Rect hopperExport() {
        // Free footer between the inspector and the Start/egg dock.
        return new DaycareLayout.Rect(82, 177, 34, 19);
    }
    static int hopperAtlasRow(boolean hovered) { return hovered ? 19 : 0; }

    static DaycareLayout.Rect helpButton() {
        // Stock PC StorageWidget at (85,27), ReleaseButton offset (+194,+124).
        return new DaycareLayout.Rect(279,151,58,16);
    }
    static int helpAtlasRow(boolean hovered) { return hovered ? 16 : 0; }
    static DaycareLayout.Rect helpNavigation(boolean back) { return new DaycareLayout.Rect(back ? 119 : 207,172,23,21); }
    static DaycareLayout.Rect helpDone() { return helpDoneFace(); }
    static DaycareLayout.Rect helpDoneFace() { return new DaycareLayout.Rect(146,172,57,21); }
    static Placement helpDoneText(int glyphWidth,int lineHeight) {
        var face=helpDoneFace();
        return centerText(face.x(),face.y(),face.width(),face.height(),glyphWidth,lineHeight);
    }
    static double helpCaptionLeft(int x,int width,int glyphWidth) { return x+(width-glyphWidth)/2.0; }
    // The revised authored recess has an uninterrupted y16..34 face at its center.
    static final int HELP_TITLE_TOP = 19;
    static double helpEggRoll(long elapsedMillis) {
        if (elapsedMillis < 0) return 0;
        return 5.0 * bat.cobblemondaycare.nest.NestLifeVisual.pulse(elapsedMillis % 3000 - 550);
    }

    static DaycareLayout.Rect helpCard(int card) { return new DaycareLayout.Rect(9+115*card,45,101,57); }

    enum ButtonArt {
        NONE, TAB, DOCK, HELP, HELP_DONE, HELP_NAV;
        boolean visible(boolean enabled, boolean hovered, boolean holding) {
            return enabled && hovered && !holding && this != NONE;
        }
    }

    static boolean canStart(boolean unlocked, boolean parentA, boolean parentB, boolean compatible,
                            String state, int queued, int capacity) {
        return unlocked && parentA && parentB && compatible && !"BREEDING".equals(state)
                && !"PAUSED_NO_SPACE".equals(state) && queued < capacity;
    }

    enum ClickRoute { CARRY, WIDGET, OTHER }
    static ClickRoute clickRoute(int button, boolean holding) {
        return button != 0 ? ClickRoute.OTHER : holding ? ClickRoute.CARRY : ClickRoute.WIDGET;
    }

    static boolean buttonHit(DaycareLayout.Rect rect, double x, double y) {
        return x >= rect.x() && x < rect.x() + rect.width()
                && y >= rect.y() && y < rect.y() + rect.height();
    }

    record Placement(double x, double y, double scale) {}
    static Placement centerText(double x, double y, double width, double height, int glyphWidth, int lineHeight) {
        return centerText(x,y,width,height,glyphWidth,lineHeight,1);
    }
    static Placement centerText(double x, double y, double width, double height, int glyphWidth, int lineHeight,
                                double preferredScale) {
        double scale = Math.min(preferredScale, Math.min(width / Math.max(1, glyphWidth), height / Math.max(1, lineHeight)));
        return new Placement(x + (width - glyphWidth * scale) / 2, y + (height - lineHeight * scale) / 2, scale);
    }

    /** Sprite bounds are half-open and include only nontransparent pixels. */
    static Placement centerSprite(DaycareLayout.Rect slot, int padding, int x0, int y0, int x1, int y1) {
        double scale = Math.min((slot.width()-2.0*padding) / (x1-x0), (slot.height()-2.0*padding) / (y1-y0));
        return new Placement(slot.x()+slot.width()/2.0 - scale*(x0+x1)/2.0,
                slot.y()+slot.height()/2.0 - scale*(y0+y1)/2.0, scale);
    }

    static DaycareLayout.Rect eggs() { return new DaycareLayout.Rect(189, 167, 24, 24); }
    static DaycareLayout.Rect portrait() { return new DaycareLayout.Rect(6, 27, 66, 66); }
    record NativePortrait(DaycareLayout.Rect rect, float baseScale, double offsetY) {}

    static NativePortrait nativePortrait(Frame frame) {
        var local = portrait();
        int left = (int) Math.floor(frame.screenX(local.x()));
        int top = (int) Math.floor(frame.screenY(local.y()));
        int right = (int) Math.ceil(frame.screenX(local.x() + local.width()));
        int bottom = (int) Math.ceil(frame.screenY(local.y() + local.height()));
        return new NativePortrait(new DaycareLayout.Rect(left, top, right - left, bottom - top),
                (float) (2 * frame.scale()), -10 * frame.scale());
    }

    static DaycareLayout.Rect infoArrow() { return new DaycareLayout.Rect(1, 157, 10, 16); }
    static DaycareLayout.Rect exit() { return new DaycareLayout.Rect(320, 186, 26, 13); }

    static int partyTarget(double x, double y) {
        for (int i = 0; i < 6; i++) if (partySlot(i).contains(x, y)) return i;
        return -1;
    }

    static int habitatTarget(double x, double y, boolean unlocked, boolean parentAOccupied, boolean parentBOccupied) {
        var area = stage();
        if (!unlocked || !(x >= area.x() && x < area.x() + area.width()
                && y >= area.y() && y < area.y() + area.height())) return -1;
        // The four rounded corner pixels are the baked frame, not the open habitat.
        boolean cornerX = x < area.x() + 1 || x >= area.x() + area.width() - 1;
        boolean cornerY = y < area.y() + 1 || y >= area.y() + area.height() - 1;
        if (cornerX && cornerY) return -1;
        // Position in the artwork does not pick a parent slot. Use the first vacancy.
        if (!parentAOccupied) return 0;
        return parentBOccupied ? -1 : 1;
    }

    static final double STAGE_MODEL_SCALE = 0.94;
    static final double PROFILE_DRAW_SCALE = 18.0;

    record ActorBox(double anchorX, double anchorY, double projectedX, double projectedY,
                    double hitTop, double hitBottom) {
        boolean hit(double x, double y) {
            return Math.abs(x - projectedX) <= 25 && y >= hitTop && y < hitBottom;
        }
    }

    static double profileShiftY(double translationY, double profileScale, double pixels) {
        // Real Cobblemon 1.8.x PROFILE transform, after its draw scale and before rotating the mesh.
        return (translationY + 1.5 * profileScale) * pixels;
    }

    static ActorBox stageModel(double centerX, double translationX, double translationY, double profileScale, double bob) {
        var r = stage();
        double pixels = STAGE_MODEL_SCALE * PROFILE_DRAW_SCALE;
        double shift = profileShiftY(translationY, profileScale, pixels);
        double target = Math.clamp(r.y() + 20 + shift, r.y() + 42.0, r.y() + 63.0) + Math.clamp(bob, -3.0, 5.0);
        return new ActorBox(centerX - translationX * pixels, target - shift, centerX, target,
                Math.max(r.y() + 6, target - 30), Math.min(r.y() + r.height(), target + 20));
    }

    record StageFit(double scale,double tx,double ty) {}

    static StageFit fitStageMesh(double x0,double y0,double x1,double y1,
                                 double center,double floor,double maxWidth,double maxHeight) {
        double scale = Math.min(2.15, Math.min(maxWidth / Math.max(1e-8,x1-x0), maxHeight / Math.max(1e-8,y1-y0)));
        return new StageFit(scale, center - (x0+x1)*.5*scale, floor - y1*scale);
    }

    static boolean landingConfirmed(bat.cobblemondaycare.nursery.model.NurseryTransfer move, int selected,
                                    boolean success, java.util.UUID source, java.util.UUID destination) {
        return move != null && move.toHabitat() && success && selected == move.yardIndex()
                && move.expectedPokemonId() != null && !move.expectedPokemonId().equals(source)
                && move.expectedPokemonId().equals(destination);
    }

    static void drawActorLayer(Runnable flushGui, Runnable renderActors) {
        flushGui.run();
        renderActors.run();
        flushGui.run();
    }
    record StageHeldIcon(double x, double y, double scale) {}

    static StageHeldIcon stageHeldIcon(NurseryGhostProjection.StageBounds visual) {
        if (visual == null) return null;
        var r = stage();
        double right = r.x() + r.width(), bottom = r.y() + r.height();
        if (visual.x1() <= r.x() || visual.x0() >= right || visual.y1() <= r.y() || visual.y0() >= bottom)
            return null;
        // Stock PC uses its own 16 px item renderer at half scale. Anchor it
        // beside the live Pokémon rather than beside an immobile slot.
        return new StageHeldIcon(Math.clamp(visual.x1()-1, r.x()+1, right-9),
                Math.clamp(visual.y1()-7, r.y()+1, bottom-9), .5);
    }
}
