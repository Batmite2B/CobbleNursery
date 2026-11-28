package bat.cobblemondaycare.client.screen;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NurseryPcNativeTextTest {
    @Test void stockHeaderUsesTheSameNativeGlyphColorsScaleAndFractionalAnchors() {
        var actual = new ArrayList<NurseryPcNativeText.Draw>();
        NurseryPcNativeText.header("FEMALE", actual::add);
        assertEquals(List.of(
            new NurseryPcNativeText.Draw("level_label", 6, 1.5, 1, true, true, false, true, 0xFFFFFFFF),
            new NurseryPcNativeText.Draw("level", 19, 1.5, 1, true, true, false, true, 0xFFFFFFFF),
            new NurseryPcNativeText.Draw("name", 12, 11.5, 1, true, true, false, true, 0xFFFFFFFF),
            new NurseryPcNativeText.Draw("\u2640", 69, 11.5, 1, true, true, false, true, 0xFC5454)), actual);
        actual.clear();
        NurseryPcNativeText.header("MALE", actual::add);
        assertEquals("\u2642", actual.getLast().key());
        assertEquals(0x32CBFF, actual.getLast().color());
        actual.clear();
        NurseryPcNativeText.header("GENDERLESS", actual::add);
        assertEquals(3, actual.size());
    }

    @Test void bodyReplaysStockNativeArgumentsForEveryHeadingValueAndStat() {
        var actual = new ArrayList<NurseryPcNativeText.Draw>();
        NurseryPcNativeText.info(actual::add);
        assertEquals(List.of("ui.info.nature", "ui.info.ability", "ui.moves", "nature", "ability", "move0", "move1", "move2", "move3"),
            actual.stream().map(NurseryPcNativeText.Draw::key).toList());
        assertEquals(List.of(129.5, 146.5, 163.5, 137.0, 154.0, 170.5, 177.5, 184.5, 191.5),
            actual.stream().map(NurseryPcNativeText.Draw::y).toList());
        assertTrue(actual.stream().allMatch(d -> !d.large() && d.scale() == .5F && d.x() == 40 && d.centered() && d.color() == 0xFFFFFFFF));
        assertTrue(actual.subList(0,3).stream().allMatch(d -> d.bold() && !d.shadow()));
        assertTrue(actual.subList(3,9).stream().allMatch(d -> !d.bold() && d.shadow()));
        for (boolean iv : new boolean[]{true, false}) {
            actual.clear();
            NurseryPcNativeText.stats(iv, actual::add);
            assertEquals(iv ? "ui.stats.ivs" : "ui.stats.evs", actual.getFirst().key());
            assertEquals(13, actual.size());
            for (int i = 0; i < 6; i++) {
                var label = actual.get(1+i*2);
                var value = actual.get(2+i*2);
                assertEquals(13, label.x());
                assertEquals(65, value.x());
                assertEquals(139 + i*10, label.y());
                assertEquals(label.y(), value.y());
                assertFalse(label.centered());
                assertTrue(value.centered());
                assertTrue(label.shadow() && value.shadow());
                assertEquals(.5F,label.scale());
                assertEquals(.5F,value.scale());
            }
        }
    }

    @Test void fittedGuiAppliesOriginAndScaleOnlyOnceToNativeCoordinates() {
        for (int width : new int[]{278, 600}) {
            var f = NurseryPcLayout.fit(width, 245);
            var actual = new ArrayList<NurseryPcNativeText.Draw>();
            NurseryPcNativeText.header("MALE", actual::add);
            for (var text : actual) {
                assertEquals(text.x(), f.localX(f.screenX(text.x())), 1e-9);
                assertEquals(text.y(), f.localY(f.screenY(text.y())), 1e-9);
            }
        }
    }
    @Test void lockedHabitatsShowTwoCenteredLargeRowsAcrossLocalesAndCosts() throws Exception {
        var stage = NurseryPcLayout.stage();
        for (var locale : List.of("en_us", "es_es", "es_mx")) {
            var path = java.nio.file.Path.of("src/main/resources/assets/cobblemondaycare/lang/" + locale + ".json");
            try (var reader = java.nio.file.Files.newBufferedReader(path)) {
                var lang = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                for (int habitat = 2; habitat <= 4; habitat++) {
                    var title = lang.get("gui.cobblemondaycare.pc.locked_" + habitat).getAsString();
                    for (int price : new int[]{0, (habitat-1)*64, Integer.MAX_VALUE}) {
                        var label = lang.get("gui.cobblemondaycare.pc.price").getAsString().formatted(price);
                        // Conservative font advances; drawing measures actual styled glyphs.
                        int titleWidth = title.length()*7, priceWidth = label.length()*7;
                        var rows = NurseryPcNativeText.lockedHabitat(stage, "locked_" + habitat,
                                titleWidth, priceWidth, 9, 0xFFFFFFFF);
                        assertEquals(List.of("locked_" + habitat, "price"), rows.stream().map(NurseryPcNativeText.Draw::key).toList());
                        var top = rows.getFirst();
                        var bottom = rows.getLast();
                        assertTrue(top.scale() > 1 && bottom.scale() > .5F);
                        assertTrue(top.y() + 10*top.scale() < bottom.y());
                        assertEquals(stage.centerX(), top.x());
                        assertEquals(stage.centerX(), bottom.x());
                        assertTrue(titleWidth*top.scale() <= stage.width()-12);
                        assertTrue(priceWidth*bottom.scale() <= stage.width()-12);
                        assertEquals(stage.y()+stage.height()/2.0, (top.y()+bottom.y()+9*bottom.scale())/2, 0.01);
                    }
                }
            }
        }
        var longRows = NurseryPcNativeText.lockedHabitat(stage, "locked_2", 600, 460, 9, 0xFFFFFFFF);
        assertTrue(longRows.getFirst().scale()*600 <= stage.width()-12);
        assertTrue(longRows.getLast().scale()*460 <= stage.width()-12);
    }
    @Test void unlockMotionRequiresItsOwnFreshSuccessfulLockedToOpenResponse() {
        var seen = new NurseryHabitatFx();
        var random = new java.util.Random(3);
        seen.request(2, 1_000);
        assertFalse(seen.confirm(2, false, true, true, "", 1_020, random));
        assertFalse(seen.confirm(1, false, true, true, "Unlocked yard 2.", 1_025, random));
        seen.request(2, 1_028);
        assertTrue(seen.confirm(2, false, true, true, "Unlocked yard 3.", 1_030, random));
        assertFalse(seen.confirm(2, false, true, true, "Unlocked yard 3.", 1_040, random));
        var first = seen.frame(2, 1_050, 200);
        assertEquals(0, first.dy(), .001);
        var frame = seen.frame(2, 1_110, 200);
        assertTrue(frame.dy() < 0 && Math.abs(frame.dx()) > 0 && Math.abs(frame.rotation()) > 0);
        assertNotEquals(frame.dy(), seen.frame(2, 1_118, 200).dy());
        assertTrue(seen.frame(2, 1_230, 200).landscape() > 0);
        assertTrue(seen.frame(2, 2_510, 200).dy() > 190);
        assertNull(seen.frame(1, 1_701, 200));
        seen.request(3, 2_000);
        assertFalse(seen.confirm(3, false, false, false, "Not enough.", 2_015, random));
        assertFalse(seen.confirm(3, false, true, true, "Unlocked yard 4.", 2_030, random));
    }

    @Test void crySchedulerUsesIrregularSpacedSingleAttemptsOnlyForVisibleCandidates() {
        var timer = new NurseryHabitatFx.CryClock(new java.util.Random(3));
        assertFalse(timer.ready(1, true, 1000));
        assertFalse(timer.ready(1, false, 40_000));
        assertTrue(timer.ready(1, true, 41_000));
        assertFalse(timer.ready(1, true, 41_000));
        assertFalse(timer.ready(2, true, 41_001));
        assertTrue(timer.ready(2, true, 60_000));
    }
    @Test void fallbackCryRequiresAnActualRegisteredFormOrSpeciesSound() {
        var form = "pokemon.raichu_alolan.cry";
        var base = "pokemon.raichu.cry";
        assertEquals(form, NurseryHabitatFx.registeredCry("raichu", "Alolan", id -> id.equals(form) || id.equals(base)));
        assertEquals(base, NurseryHabitatFx.registeredCry("raichu", "Alolan", base::equals));
        assertNull(NurseryHabitatFx.registeredCry("raichu", "Alolan", id -> false));
    }
    @Test void lockFallStartsGentlyAndAcceleratesWithoutViewportDependentGravity() {
        for (int distance : new int[]{220, 350, 620}) {
            var fx = new NurseryHabitatFx();
            fx.request(2, 1000);
            assertTrue(fx.confirm(2,false,true,true,"Unlocked yard 3.",1000,new java.util.Random(42)));
            assertNotNull(fx.frame(2, 1000, distance));
            double apex = fx.frame(2,1132,distance).dy();
            assertEquals(-10,apex,.01);
            assertTrue(Math.abs(fx.frame(2,1148,distance).dy()-apex) < .1);
            assertEquals(4.6,fx.frame(2,1332,distance).dy()-apex,.01);
            assertEquals(18.4,fx.frame(2,1532,distance).dy()-apex,.01);
            assertEquals(1,fx.frame(2,1532,distance).landscape());
        }
    }
    @Test void lockDriftsAtTheSameSpeedThroughApexAndPastFormerLateralLimit() {
        for (boolean positive : new boolean[]{false,true}) {
            var fx = new NurseryHabitatFx();
            fx.request(2, 1000);
            var random = new java.util.Random(42) {
                @Override public boolean nextBoolean() { return positive; }
            };
            assertTrue(fx.confirm(2,false,true,true,"Unlocked yard 3.",1000,random));
            assertEquals(0.0,fx.frame(2,1000,350).dx(),.00001);
            int sign = positive ? 1 : -1;
            double speed = sign * fx.frame(2,1050,350).dx()/50;
            assertTrue(speed >= .035 && speed <= .080);
            for (long elapsed : new long[]{120,132,160,550,900,1050,1400}) {
                var frame = fx.frame(2,1000+elapsed,350);
                var later = fx.frame(2,1000+elapsed+16,350);
                assertEquals(sign*speed*16,later.dx()-frame.dx(),.00001);
            }
            assertTrue(Math.abs(fx.frame(2,2400,350).dx()) > 29);
        }
    }
}