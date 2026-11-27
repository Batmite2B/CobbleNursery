package bat.cobblenursery.client.screen;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NurseryInspectorAndGhostTest {
    @Test void activeHoverOverridesOnlyWhileOccupiedAndIdleSticksLikePc() {
        assertTrue(NurseryInspectorChoice.production("BREEDING", true));
        assertTrue(NurseryInspectorChoice.production("PAUSED_NO_SPACE", true));
        assertFalse(NurseryInspectorChoice.production("PAUSED_NO_SPACE", false));
        assertFalse(NurseryInspectorChoice.production("READY_TO_START", true));
        assertEquals("hover", NurseryInspectorChoice.resolve(true, "offspring", null, "hover", "last"));
        assertEquals("offspring", NurseryInspectorChoice.resolve(true, "offspring", "selected", null, "last"));
        assertEquals("selected", NurseryInspectorChoice.resolve(false, "unused", "selected", "hover", "last"));
        assertEquals("hover", NurseryInspectorChoice.resolve(false, null, null, "hover", "last"));
        assertEquals("last", NurseryInspectorChoice.resolve(false, null, null, null, "last"));
        assertNull(NurseryInspectorChoice.resolve(false, null, null, null, null));
    }

    @Test void asymmetricTallWideAndLeanProjectedVerticesAllAnchorToCursor() {
        for (var model : List.of(List.of(new double[]{-1, -8},new double[]{2, 9}),
                                 List.of(new double[]{-15, -2},new double[]{4, 1}),
                                 List.of(new double[]{-1, -1},new double[]{1, 1}))) {
            for (double angle : new double[]{-.35, 0, .28}) for (double scale : new double[]{.61, 1}) {
                var box = new NurseryGhostProjection.Bounds();
                for (var vertex : model) {
                    double x = 107 + scale * (Math.cos(angle)*vertex[0] - Math.sin(angle)*vertex[1]);
                    double y = 61 + scale * (Math.sin(angle)*vertex[0] + Math.cos(angle)*vertex[1]);
                    box.include(x, y);
                }
                var n = box.shiftTo(122.75, 91.5, 4);
                assertNotNull(n);
                assertEquals(122.75, box.midX()+n.x(), 1e-9);
                assertEquals(95.5, box.minY()+n.y(), 1e-9);
            }
        }
        var empty = new NurseryGhostProjection.Bounds();
        empty.include(Double.NaN, 2);
        assertNull(empty.shiftTo(5, 6, 4));
    }
    @Test void exactVertexConsumerPathShiftsOnlyGuiTranslationAndCleansExceptionalScope() {
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.translate(103, 49, 320);
        pose.scale(.67F, .67F, 1F);
        pose.mulPose(new org.joml.Quaternionf().rotationXYZ(.15F, .36F, -.26F));
        var before = new org.joml.Matrix4f(pose.last().pose());
        var normal = new org.joml.Matrix3f(pose.last().normal());
        float[][] mesh = {{-2,-9,1},{3,11,-2},{13,2,9},{-8,6,-2}};
        assertTrue(NurseryGhostProjection.alignGeometry(pose, 162, 90, 4, (sample, consumer) -> {
            assertNotSame(pose, sample);
            for (var v : mesh) consumer.addVertex(sample.last().pose(), v[0], v[1], v[2]);
            sample.translate(123, 456, 789); // A custom probe cannot change the live stack.
        }));
        var after = pose.last().pose();
        assertEquals(before.m00(),after.m00()); assertEquals(before.m01(),after.m01());
        assertEquals(before.m10(),after.m10()); assertEquals(before.m11(),after.m11());
        assertEquals(before.m32(),after.m32());
        assertEquals(normal, pose.last().normal());
        var shifted = new NurseryGhostProjection.Bounds();
        for (var v : mesh) {
            var xy = after.transformPosition(v[0], v[1], v[2], new org.joml.Vector3f());
            shifted.include(xy.x, xy.y);
        }
        assertEquals(162, shifted.midX(), .001);
        assertEquals(94, shifted.minY(), .001);
        assertFalse(NurseryGhostProjection.sampling());
        assertThrows(IllegalStateException.class, () -> NurseryGhostProjection.centerGhost(1,2,4, () -> {
            assertTrue(NurseryGhostProjection.sampling());
            throw new IllegalStateException("render failed");
        }));
        assertFalse(NurseryGhostProjection.sampling());

        var saved = new org.joml.Matrix4f(after);
        assertFalse(NurseryGhostProjection.alignGeometry(pose, 162, 90, 4, (sample, consumer) -> {
            sample.translate(1,2,3);
            throw new IllegalStateException("custom model probe failed");
        }));
        assertEquals(saved, pose.last().pose());
    }
    @Test void screenSpaceLeanOpposesPointerTravelAtBothGuiScales() {
        for (double guiScale : new double[]{.61,1}) {
            for (int direction : new int[]{-1,1}) {
                var sway = new NurseryCursorInteraction.Sway();
                sway.sample(0,0,10);
                var tilt = sway.sample(direction*5,0,26);
                var pose = new com.mojang.blaze3d.vertex.PoseStack();
                pose.translate(141,56,320);
                pose.scale((float)(20*guiScale),(float)(20*guiScale),-20);
                pose.mulPose(NurseryCursorInteraction.heldModelRotation(tilt));
                float[][] mesh={{0,-1,0},{0,1,0},{-1.5F,.3F,.2F},{.5F,-.1F,-.1F}};
                assertTrue(NurseryGhostProjection.alignGeometry(pose, 160,80,4,tilt.roll(), (m,out) -> {
                    for(var v:mesh) out.addVertex(m.last().pose(),v[0],v[1],v[2]);
                }));
                var geometry = new NurseryGhostProjection.Bounds();
                var actual = pose.last().pose();
                for(var v:mesh) {var a=actual.transformPosition(v[0],v[1],v[2], new org.joml.Vector3f());geometry.include(a.x,a.y);}
                assertEquals(160,geometry.midX(),.001);
                assertEquals(84,geometry.minY(),.001);
                var top=actual.transformPosition(0,-1,0,new org.joml.Vector3f());
                var bottom=actual.transformPosition(0,1,0,new org.joml.Vector3f());
                assertTrue(direction * (bottom.x - top.x) < -8*guiScale, "hanging lower body must lag opposite the mouse relative to the top");
            }
        }
    }
    @Test void realShellosLandmarksSwingFromOppositeSideOnTheFirstReversal() {
        var s = new NurseryCursorInteraction.Sway();
        s.sample(0,0,10);
        NurseryCursorInteraction.Tilt before = s.sample(180,0,26);
        for (int t=30;t<=58;t+=4) before=s.sample(180+(t-26)*14,0,t);
        // Move slightly left after a fast right flick, at 250Hz.
        var left=s.sample(626,0,62);
        assertTrue(left.roll() < 0, "leftward movement must replace a stale right sign on the first sample");
        assertTrue(before.roll() > 0);

        // East Shellos stock 1.8.4: head pivot [0,11.5,-3.5], torso [0,4.5,0],
        // head cube [-4,10.5,-8] size [8,7,8], main torso [-5,1,-6.5] size [10,7,9].
        // Convert Bedrock model y-up and pixel units to renderer coordinates.
        float[] head={0,-11.5F/16,-3.5F/16}, torso={0,-4.5F/16,0};
        for (double fit:new double[]{.68,1}) {
            double neutral=projectShellosSeparation(new NurseryCursorInteraction.Tilt(0,0),fit,head,torso);
            assertTrue(projectShellosSeparation(left,fit,head,torso)-neutral > 1.0*fit);
            assertTrue(projectShellosSeparation(before,fit,head,torso)-neutral < -1.0*fit);
        }
    }
    private static double projectShellosSeparation(NurseryCursorInteraction.Tilt tilt,double fit,float[] head,float[] torso){
        var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(120,50,320);
        pose.scale((float)(20*fit),(float)(20*fit),-20);
        pose.mulPose(NurseryCursorInteraction.heldModelRotation(tilt));
        assertTrue(NurseryGhostProjection.alignGeometry(pose,160,80,4,tilt.roll(),(m,out)->{
            var mx=m.last().pose();
            for (float x:new float[]{-5,5}) for(float y:new float[]{-1,-17.5F}) for(float z:new float[]{-8,2.5F})
                out.addVertex(mx,x/16,y/16,z/16);
        }));
        var mx=pose.last().pose();
        var top=mx.transformPosition(head[0],head[1],head[2],new org.joml.Vector3f());
        var bottom=mx.transformPosition(torso[0],torso[1],torso[2],new org.joml.Vector3f());
        return bottom.x-top.x;
    }
    @Test void offspringNatureIsShownOnlyForValidCertainBaseNatures() {
        var stone = new bat.cobblenursery.nursery.model.BreedingItemSnapshot("cobblemon:everstone",1);
        var none = bat.cobblenursery.nursery.model.BreedingItemSnapshot.EMPTY;
        assertEquals("cobblemon:adamant", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                "cobblemon:adamant",stone,"cobblemon:modest",none));
        assertEquals("cobblemon:modest", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                "cobblemon:adamant",none,"cobblemon:modest",stone));
        assertEquals("cobblemon:adamant", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                "cobblemon:adamant",stone,"cobblemon:adamant",stone));
        for (var invalid : new String[]{"", "not:a_nature", "cobblemon:modest_mint", "illegal id", null}) {
            assertEquals("", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                    invalid,stone,"cobblemon:adamant",none));
            assertEquals("", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                    invalid,stone,invalid,stone));
        }
        assertEquals("", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                "cobblemon:adamant",stone,"cobblemon:modest",stone));
        assertEquals("", bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                "cobblemon:adamant",none,"cobblemon:modest",none));
    }
    @Test void minimumIvsUseOnlyTheCurrentEquippedParents() {
        var minimums = bat.cobblenursery.breeding.service.IvInheritanceService.guaranteedMinimums(
                "cobblemon:power_bracer", List.of(31,7,12,14,16,20),
                "cobblemon:power_bracer", List.of(5,28,27,25,23,21));
        assertEquals(List.of(0,28,0,0,0,0),minimums);
        assertEquals(List.of(31,0,0,0,0,21),
                bat.cobblenursery.breeding.service.IvInheritanceService.guaranteedMinimums(
                        "cobblemon:power_weight", List.of(31,31,31,31,31,31),
                        "cobblemon:power_anklet", List.of(21,21,21,21,21,21)));
        assertEquals(List.of(0,0,0,0,0,0),
                bat.cobblenursery.breeding.service.IvInheritanceService.guaranteedMinimums(
                        "cobblemon:destiny_knot", List.of(31,31,31,31,31,31),
                        "cobblemon:power_weight", List.of()));
    }
}