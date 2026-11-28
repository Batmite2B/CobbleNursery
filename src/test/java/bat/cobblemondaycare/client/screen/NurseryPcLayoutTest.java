package bat.cobblemondaycare.client.screen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NurseryPcLayoutTest {
    @Test
    void v3CanvasAndNativePartyMatchTheAuthoredTemplate() {
        assertEquals(349, NurseryPcLayout.WIDTH);
        assertEquals(205, NurseryPcLayout.HEIGHT);
        assertEquals(new DaycareLayout.Rect(85, 29, 174, 99), NurseryPcLayout.stage());
        assertEquals(new DaycareLayout.Rect(278, 35, 25, 25), NurseryPcLayout.partySlot(0));
        assertEquals(new DaycareLayout.Rect(309, 105, 25, 25), NurseryPcLayout.partySlot(5));
    }

    @Test
    void fittedCoordinatesRoundTripOnSmallAndLargeGui() {
        for (int width : new int[]{278, 427, 850}) for (int height : new int[]{151, 240, 480}) {
            var frame = NurseryPcLayout.fit(width, height);
            assertTrue(frame.left() >= 0 && frame.top() >= 0);
            assertTrue(frame.screenX(NurseryPcLayout.WIDTH) <= width);
            assertTrue(frame.screenY(NurseryPcLayout.HEIGHT + EggGroupTab.HEIGHT) <= height);
            assertEquals(145.5, frame.localX(frame.screenX(145.5)), 1e-8);
            assertEquals(79.5, frame.localY(frame.screenY(79.5)), 1e-8);
        }
    }

    @Test
    void stageAndTabsNeverOverlapAndOnlyExactSlotsAreDropTargets() {
        var stage = NurseryPcLayout.stage();
        assertEquals(0, NurseryPcLayout.habitatTarget(stage.x() + 10, stage.y() + 10, true, false, false));
        assertEquals(0, NurseryPcLayout.habitatTarget(stage.x() + stage.width() - 10, stage.y() + 10, true, false, false));
        assertEquals(-1, NurseryPcLayout.habitatTarget(stage.centerX(), stage.y() - 1, true, false, false));
        for (int i = 0; i < 6; i++) {
            var slot = NurseryPcLayout.partySlot(i);
            assertEquals(i, NurseryPcLayout.partyTarget(slot.centerX(), slot.centerY()));
            assertEquals(-1, NurseryPcLayout.habitatTarget(slot.centerX(), slot.centerY(), true, false, false));
        }
        for (int i = 0; i < 4; i++) {
            var tab = NurseryPcLayout.tab(i);
            assertEquals(-1, NurseryPcLayout.habitatTarget(tab.centerX(), tab.centerY(), true, false, false));
            assertEquals(-1, NurseryPcLayout.partyTarget(tab.centerX(), tab.centerY()));
        }
    }
    @Test
    void eitherHalfUsesTheFirstVacancyAndNeverOverwritesAnOccupiedParent() {
        for (var frame : new NurseryPcLayout.Frame[]{NurseryPcLayout.fit(854,480), NurseryPcLayout.fit(278,151)}) {
            var r = NurseryPcLayout.stage();
            for (double localX : new double[]{r.x()+1.25, r.centerX(), r.x()+r.width()-1.25}) {
                double x = frame.localX(frame.screenX(localX));
                double y = frame.localY(frame.screenY(r.centerY()));
                assertEquals(0, NurseryPcLayout.habitatTarget(x,y,true,false,false));
                assertEquals(1, NurseryPcLayout.habitatTarget(x,y,true,true,false));
                assertEquals(0, NurseryPcLayout.habitatTarget(x,y,true,false,true));
                assertEquals(-1, NurseryPcLayout.habitatTarget(x,y,true,true,true));
                assertEquals(-1, NurseryPcLayout.habitatTarget(x,y,false,false,false));
            }
            assertEquals(-1, NurseryPcLayout.habitatTarget(r.x()+.5, r.y()+.5,true,false,false));
            assertEquals(-1, NurseryPcLayout.habitatTarget(r.x()+r.width()-.5, r.y()+r.height()-.5,true,false,false));
            assertEquals(0, NurseryPcLayout.habitatTarget(r.x()+1.25, r.y()+.25,true,false,false));
            assertEquals(-1, NurseryPcLayout.habitatTarget(r.x()-.25, r.centerY(),true,false,false));
            assertEquals(-1, NurseryPcLayout.habitatTarget(r.x()+r.width(),r.centerY(),true,false,false));
            assertEquals(-1, NurseryPcLayout.habitatTarget(r.centerX(),r.y()+r.height(),true,false,false));
        }
    }
    @Test
    void authoredButtonArtUsesNativeBoundsAndOnlyIntendedStates() {
        assertEquals(new DaycareLayout.Rect(128,167,59,23), NurseryPcLayout.startHover());
        assertEquals(new DaycareLayout.Rect(128,167,59,23), NurseryPcLayout.action());
        assertTrue(NurseryPcLayout.startHover().x()+NurseryPcLayout.startHover().width() < NurseryPcLayout.eggs().x());
        for (int tab=0;tab<4;tab++) {
            assertEquals(45, NurseryPcLayout.tab(tab).width());
            assertEquals(18, NurseryPcLayout.tab(tab).height());
        }
        var tab = NurseryPcLayout.ButtonArt.TAB;
        var start = NurseryPcLayout.ButtonArt.DOCK;
        var none = NurseryPcLayout.ButtonArt.NONE;
        assertFalse(tab.visible(true,false,false));
        assertTrue(tab.visible(true,true,false));
        assertFalse(tab.visible(true,false,false));
        assertFalse(start.visible(true,false,false));
        assertTrue(start.visible(true,true,false));
        assertFalse(start.visible(false,true,false));
        assertFalse(none.visible(true,true,false));
        assertFalse(start.visible(true,true,true));
        assertFalse(tab.visible(true,true,true));
        var action = NurseryPcLayout.action();
        for (var f : new NurseryPcLayout.Frame[]{NurseryPcLayout.fit(854,480), NurseryPcLayout.fit(278,151)}) {
            double x=f.localX(f.screenX(action.centerX())), y=f.localY(f.screenY(action.centerY()));
            assertTrue(NurseryPcLayout.buttonHit(action,x,y));
            assertFalse(NurseryPcLayout.buttonHit(action,action.x()+action.width(),y));
            assertFalse(NurseryPcLayout.buttonHit(action,x,action.y()+action.height()));
        }
    }
    @Test void alphaGeometryAndMeasuredGlyphsCenterInsideTheirActualWells() {
        var egg = NurseryPcLayout.eggs();
        var scaled = NurseryPcLayout.centerSprite(egg, 2, 1, 3, 15, 11);
        assertEquals(egg.x()+egg.width()/2.0, scaled.x()+scaled.scale()*8.0, 1e-8);
        assertEquals(egg.y()+egg.height()/2.0, scaled.y()+scaled.scale()*7.0, 1e-8);
        assertEquals(20, scaled.scale()*14, 1e-8);
        var area = NurseryPcLayout.status();
        var text = NurseryPcLayout.centerText(area.x()+6,area.y(),area.width()-12,area.height(),95,9);
        assertEquals(area.x()+area.width()/2.0, text.x()+95*text.scale()/2.0, 1e-8);
        assertEquals(area.y()+area.height()/2.0, text.y()+9*text.scale()/2.0, 1e-8);
        assertEquals(1, text.scale());
        assertTrue(NurseryPcLayout.centerText(0,0,60,20,120,9).scale() < 1);
    }
    @Test void dockingDoesNotStartInvalidPairsAndCarryStillOwnsTheFirstClick() {
        assertEquals(NurseryPcLayout.ClickRoute.CARRY, NurseryPcLayout.clickRoute(0,true));
        assertEquals(NurseryPcLayout.ClickRoute.WIDGET, NurseryPcLayout.clickRoute(0,false));
        assertEquals(NurseryPcLayout.ClickRoute.OTHER, NurseryPcLayout.clickRoute(1,true));
        assertTrue(NurseryPcLayout.canStart(true,true,true,true,"READY_TO_START",1,4));
        assertFalse(NurseryPcLayout.canStart(false,true,true,true,"READY_TO_START",1,4));
        assertFalse(NurseryPcLayout.canStart(true,false,true,true,"INCOMPLETE",1,4));
        assertFalse(NurseryPcLayout.canStart(true,true,true,false,"BLOCKED",1,4));
        assertFalse(NurseryPcLayout.canStart(true,true,true,true,"BREEDING",1,4));
        assertFalse(NurseryPcLayout.canStart(true,true,true,true,"PAUSED_NO_SPACE",3,4));
        assertFalse(NurseryPcLayout.canStart(true,true,true,true,"READY_TO_START",4,4));
    }
    @Test void stockHalfScaleHeldItemFollowsFinalVisualBoundsAndAvoidsClipping() {
        var stage = NurseryPcLayout.stage();
        var walking = NurseryPcLayout.stageHeldIcon(new NurseryGhostProjection.StageBounds(120,55,155,95));
        var moved = NurseryPcLayout.stageHeldIcon(new NurseryGhostProjection.StageBounds(130,47,165,87));
        assertEquals(10,moved.x()-walking.x(),.0001);
        assertEquals(-8,moved.y()-walking.y(),.0001);
        assertEquals(.5,moved.scale(),.0001);
        var right = NurseryPcLayout.stageHeldIcon(new NurseryGhostProjection.StageBounds(239,97,259,128));
        assertTrue(right.x()+8 < stage.x()+stage.width());
        assertTrue(right.y()+8 < stage.y()+stage.height());
        assertNull(NurseryPcLayout.stageHeldIcon(null));
        assertNull(NurseryPcLayout.stageHeldIcon(new NurseryGhostProjection.StageBounds(300,90,340,112)));
    }
    @org.junit.jupiter.api.Test void tutorialModalCapturesLiveDaycareInputBeforePokemonOrServerActions() throws Exception {
        var source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/bat/cobblemondaycare/client/screen/DaycareScreen.java"));
        assertTrue(source.indexOf("super.mouseClicked(x, y, button);") < source.indexOf("var route = NurseryPcLayout.clickRoute"));
        assertTrue(source.contains("if (helpOpen) drawHelpPage"));
        assertTrue(source.contains("if (helpOpen) {"));
        assertTrue(source.contains("NurseryPcLayout.helpButton()"));
        var button=NurseryPcLayout.helpButton();
        // Cobblemon PC GUI -> StorageWidget (85,27) -> ReleaseButton (+194,+124), native size 58x16.
        assertEquals(new DaycareLayout.Rect(279,151,58,16),button);
        assertEquals(0,NurseryPcLayout.helpAtlasRow(false));
        assertEquals(16,NurseryPcLayout.helpAtlasRow(true));
        assertTrue(source.contains("pcArt(\"pc_release_button\")"));
        assertTrue(source.contains("button -> setHelp(true)"));
        assertTrue(button.y()>NurseryPcLayout.partySlot(5).y()+NurseryPcLayout.partySlot(5).height());
        assertTrue(button.y()+button.height()<NurseryPcLayout.exit().y());
        assertTrue(button.x()>NurseryPcLayout.eggs().x()+NurseryPcLayout.eggs().width());
        for (int w : new int[]{239,349,600}) {
            var frame=NurseryPcLayout.fit(w,230);
            assertTrue(NurseryPcLayout.buttonHit(button,frame.localX(frame.screenX(button.x()+29)),
                   frame.localY(frame.screenY(button.y()+8))));
        }
    }
    @org.junit.jupiter.api.Test void tutorialUsesStockDefaultWallpaperUnderAllCardsAndUncompressedCaptionGlyphs() throws Exception {
        var source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/bat/cobblemondaycare/client/screen/DaycareScreen.java"));
        assertTrue(source.contains("drawHelpCaption(g,"));
        assertTrue(source.contains("PCBoxWallpaperRepository.INSTANCE.getDefaultWallpaper()"));
        assertFalse(source.contains("drawHelpPortraitBackdrop"));
        assertFalse(source.contains("helpWallpaperSlices"));
        assertTrue(source.contains("pc_screen_overlay"));
        assertTrue(source.contains("helpPage == 0 && i == 0 ? YARD_WALLPAPER_TEXTURES[0] : wallpaper"));
        assertTrue(source.contains("glow/"));
        int helpPage = source.indexOf("private void drawHelpPage");
        int baseLayer = source.indexOf("g.blit(HELP_TEXTURE", helpPage);
        int glowLayer = source.indexOf("g.blit(glow,", helpPage);
        int sideTrim = source.indexOf("g.blit(HELP_SIDE_LEFT_TEXTURE", helpPage);
        assertTrue(helpPage >= 0 && baseLayer > helpPage && glowLayer > baseLayer && sideTrim > glowLayer,
                "Draw the original glow above the base but below the authored trim");
        assertTrue(source.contains("helpHeading(helpPage == 1 ?"));
        assertTrue(source.contains("drawHelpTitle(g, helpHeading("));
        assertTrue(source.contains("titleWeight = 0.5F"));
        assertTrue(source.contains("toUpperCase(java.util.Locale.ROOT)"));
        assertTrue(source.contains("NurseryGhostProjection.fitStage(fit,()->renderPokemonModel"));
        assertTrue(source.contains("g.pose().scale(scale,scale,1)"), "Caption scaling must preserve glyph proportions");
        assertTrue(source.contains("HELP_MOTHER = \"cobblemon:togekiss\""));
        assertTrue(source.contains("HELP_FATHER = \"cobblemon:raichu\""));
        assertTrue(source.contains("HELP_CHILD = \"cobblemon:togepi\""));
        assertTrue(source.contains("resolve(HELP_CHILD, \"normal\").modelData()"));
        assertFalse(source.contains("cobblemon:charmander"));
        assertTrue(source.contains("help:pair_left\", HELP_MOTHER"));
        assertTrue(source.contains("help:pair_right\", HELP_FATHER"));
    }
    @org.junit.jupiter.api.Test void guideTitlesAndCaptionsShareTheAuthoredVisualCenterAtBothFits() {
        var face=NurseryPcLayout.helpDoneFace();
        assertEquals(new DaycareLayout.Rect(146,172,57,21),face);
        for(int frameWidth:new int[]{278,854}) {
            var frame=NurseryPcLayout.fit(frameWidth,frameWidth==278?151:480);
            for(int w:new int[]{18,26,32}) {
                var label=NurseryPcLayout.helpDoneText(w,9);
                assertEquals(frame.screenX(face.x()+face.width()/2.0),frame.screenX(label.x()+w/2.0),.0001);
                assertEquals(frame.screenY(face.y()+face.height()/2.0),frame.screenY(label.y()+4.5),.0001);
            }
            var card=NurseryPcLayout.helpCard(2);
            for(int glyph:new int[]{18,75,91}) {
                double left=NurseryPcLayout.helpCaptionLeft(card.x()+5,card.width()-10,glyph);
                assertEquals(frame.screenX(card.x()+card.width()/2.0),frame.screenX(left+glyph/2.0),.0001);
            }
        }
    }
    @org.junit.jupiter.api.Test void hatchIllustrationReusesTheTwoPulseNestWaveWithRestingGaps() throws Exception {
        var roll=NurseryPcLayout.class.getDeclaredMethod("helpEggRoll",long.class);
        java.util.function.LongToDoubleFunction wave=t-> {
            try { return (double)roll.invoke(null,t); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        };
        assertEquals(0,wave.applyAsDouble(100),.0001);
        assertTrue(Math.abs(wave.applyAsDouble(600))>1);
        assertTrue(Math.abs(wave.applyAsDouble(950))>1);
        assertEquals(0,wave.applyAsDouble(1500),.0001);
        assertEquals(wave.applyAsDouble(600),wave.applyAsDouble(3600),.0001);
        var source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/bat/cobblemondaycare/client/screen/DaycareScreen.java"));
        assertEquals(2,source.split("drawHelpHatchingEgg\\(",-1).length-1);
        assertTrue(source.contains("helpEggAnimationStart"));
        assertTrue(source.contains("renderHelpNestScene"));
        assertTrue(source.contains("pc_arrow_previous"));
        assertTrue(source.contains("pc_arrow_next"));
    }

    @org.junit.jupiter.api.Test void tutorialUsesTerminalModelNativeHeadingsAndOfficialHover() throws Exception {
        var path=java.nio.file.Path.of("src/main/resources/assets/cobblemondaycare/textures/gui/nursery_help_egg_slot.png");
        var png=javax.imageio.ImageIO.read(path.toFile());
        assertEquals(24,png.getWidth());
        assertEquals(24,png.getHeight());
        assertEquals(0,png.getRGB(0,0)>>>24);
        assertEquals(0,png.getRGB(23,23)>>>24);
        var source=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/bat/cobblemondaycare/client/screen/DaycareScreen.java"));
        assertTrue(source.contains("new ItemStack(NurseryRegistry.NURSERY_TERMINAL_ITEM)"));
        assertTrue(source.contains("void drawHelpCardTitle"));
        assertTrue(source.contains("void drawHelpParentTag"));
        assertFalse(source.contains("void drawHelpSpeciesNote"));
        assertFalse(source.contains("void drawHelpChildExample"));
        assertTrue(source.contains("cx - 21, y, cx + 21, y + 14, 0x80000000"));
        assertTrue(source.contains("isHovered() ? 14 : 0"));
        assertTrue(source.contains("x,y,17,1.5F,0xFFFFFFFF"));
        assertTrue(source.contains("g.blit(START_HOVER_TEXTURE,face.x(),face.y(),1,1"));
        assertFalse(source.contains("g.setColor(1.26F"));
    }
    @org.junit.jupiter.api.Test void tutorialSideTrimPreservesAuthoredPixelsAndMirrorsExactly() throws Exception {
        var dir=java.nio.file.Path.of("src/main/resources/assets/cobblemondaycare/textures/gui");
        var left=javax.imageio.ImageIO.read(dir.resolve("nursery_help_side_left.png").toFile());
        var right=javax.imageio.ImageIO.read(dir.resolve("nursery_help_side_right.png").toFile());
        assertEquals(13,left.getWidth());
        assertEquals(93,left.getHeight());
        assertEquals(13,right.getWidth());
        assertEquals(93,right.getHeight());
        for(int y=0;y<93;y++) {
            for(int x=0;x<13;x++) assertEquals(left.getRGB(x,y),right.getRGB(12-x,y));
        }
    }

    @org.junit.jupiter.api.Test void tutorialWallpaperScaleFitsAndRevisedFrameKeepsTitleRecessAndApertures() throws Exception {
        var shell=javax.imageio.ImageIO.read(java.nio.file.Path.of("src/main/resources/assets/cobblemondaycare/textures/gui/nursery_help.png").toFile());
        assertEquals(349,shell.getWidth());
        assertEquals(205,shell.getHeight());
        assertEquals(255,shell.getRGB(174,NurseryPcLayout.HELP_TITLE_TOP)>>>24);
        for(int index=0;index<3;index++){
            var card=NurseryPcLayout.helpCard(index);
            assertEquals(0,shell.getRGB(card.centerX(),card.centerY())>>>24);
            for(int y=card.y();y<card.y()+card.height();y++) {
                boolean open=false;
                for(int x=card.x();x<card.x()+card.width();x++) open |= (shell.getRGB(x,y)>>>24)==0;
                assertTrue(open,"The wallpaper needs to cover every row of the authored opening");
            }
        }
    }
}
