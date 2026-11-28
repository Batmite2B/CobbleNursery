package bat.cobblemondaycare.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NurseryActorAndPickupTest {
    @Test
    void projectedStockModelsAndHitRegionsStayWithinStageAfterViewportScaling() {
        var stage = NurseryPcLayout.stage();
        // Installed Cobblemon 1.8.4: Shellos west/east poser, WooperModel, Onix poser.
        double[][] profiles = {{0.89, 0.61}, {0.60, 0.75}, {1.23, 0.24}};
        assertTrue(112 + (0.89 + 1.5 * 0.61) * 18 * 1.25 > stage.y() + stage.height());
        for (var p : profiles) for (int bob : new int[]{-2, 0, 5}) {
            var actor = NurseryPcLayout.stageModel(142, 0, p[0], p[1], bob);
            assertEquals(actor.projectedY(), actor.anchorY() + (p[0] + 1.5 * p[1]) * 18 * 0.94, 1e-8);
            assertTrue(actor.hit(142, actor.projectedY()));
            assertFalse(actor.hit(142, stage.y() + 1));
            assertFalse(actor.hit(93, actor.projectedY()));
            for (var view : new NurseryPcLayout.Frame[]{NurseryPcLayout.fit(854,480), NurseryPcLayout.fit(280,160)}) {
                assertTrue(view.screenY(stage.y()) < view.screenY(actor.projectedY()));
                assertTrue(view.screenY(actor.projectedY()) < view.screenY(stage.y() + stage.height()));
                assertTrue(view.screenY(actor.hitTop()) >= view.screenY(stage.y()));
                assertTrue(view.screenY(actor.hitBottom()) <= view.screenY(stage.y() + stage.height()));
            }
        }
    }

    @Test
    void clickIsTheOnlyPickupOrDropDecision() {
        assertEquals(NurseryCursorInteraction.Click.PICK, NurseryCursorInteraction.decide(false, true, false));
        assertEquals(NurseryCursorInteraction.Click.PLACE, NurseryCursorInteraction.decide(true, false, true));
        assertEquals(NurseryCursorInteraction.Click.CANCEL, NurseryCursorInteraction.decide(true, true, false));
        assertEquals(NurseryCursorInteraction.Click.IGNORE, NurseryCursorInteraction.decide(false, false, false));
    }

    @Test
    void pointerMovementTiltsInSignedDirectionsAndRestDecaysToNeutral() {
        var sway = new NurseryCursorInteraction.Sway();
        assertEquals(0, sway.sample(0, 0, 10).roll(), 1e-8);
        var right = sway.sample(25, 10, 26);
        assertTrue(right.roll() > 0 && right.pitch() > 0);
        var left = sway.sample(-30, -20, 58);
        assertTrue(left.roll() < 0 && left.pitch() < 0);
        var still = left;
        for (int t = 74; t < 400; t += 16) still = sway.sample(-30, -20, t);
        assertEquals(0, still.roll(), 0.006);
        assertEquals(0, still.pitch(), 0.006);
    }
    @Test
    void guiBackdropFlushesBeforeActorLayerAndInspectorOriginFitsItsClip() {
        var order = new java.util.ArrayList<String>();
        NurseryPcLayout.drawActorLayer(() -> order.add("flush"), () -> order.add("actor"));
        assertEquals(java.util.List.of("flush", "actor", "flush"), order);
        double shift = NurseryPcLayout.profileShiftY(0.89, 0.61, 18 * 1.1);
        assertTrue(64 + shift > 79);
        double adjusted = 59 - shift;
        assertTrue(adjusted + shift > 32 && adjusted + shift < 79);
    }

    @Test
    void nativePortraitUsesOneAbsoluteClipAtCenteredAndScaledViewports() {
        var local = NurseryPcLayout.portrait();
        for (var frame : new NurseryPcLayout.Frame[]{NurseryPcLayout.fit(854,480),
                NurseryPcLayout.fit(427,240), NurseryPcLayout.fit(285,159),
                new NurseryPcLayout.Frame(70,121,.8)}) {
            var nativePortrait = NurseryPcLayout.nativePortrait(frame);
            var screen = nativePortrait.rect();
            assertTrue(screen.width() > 0 && screen.height() > 0);
            assertEquals(2 * frame.scale(), nativePortrait.baseScale(), 1e-6);
            assertEquals(-10 * frame.scale(), nativePortrait.offsetY(), 1e-9);
            assertTrue(screen.contains(frame.screenX(39), frame.screenY(60)));
            assertTrue(screen.x() <= frame.screenX(local.x()));
            assertTrue(screen.x()+screen.width() >= frame.screenX(local.x()+local.width()));
            assertTrue(screen.y() <= frame.screenY(local.y()));
            assertTrue(screen.y()+screen.height() >= frame.screenY(local.y()+local.height()));
            assertEquals(frame.screenX(39), screen.x()+screen.width()/2.0, .95);
        }
        var centered = NurseryPcLayout.nativePortrait(NurseryPcLayout.fit(854,480)).rect();
        assertTrue(centered.x() >= local.x()+local.width(), "old local inner clip would be disjoint");
        assertEquals(new DaycareLayout.Rect(6,27,66,66), NurseryPcLayout.nativePortrait(new NurseryPcLayout.Frame(0,0,1)).rect());
    }

    @Test
    void undoingFitRestoresTheEnclosingPoseWithoutMovingTheNativeClipTwice() {
        for (var frame : new NurseryPcLayout.Frame[]{NurseryPcLayout.fit(854,480),NurseryPcLayout.fit(280,160)}) {
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            pose.translate(0, 0, 10);
            var entry = new org.joml.Matrix4f(pose.last().pose());
            pose.translate(frame.left(),frame.top(),0);
            pose.scale((float)frame.scale(), (float)frame.scale(),1);
            pose.pushPose();
            frame.undoFit(pose);
            assertTrue(entry.equals(pose.last().pose(), .0002f));
            pose.popPose();
            assertEquals(frame.left(), pose.last().pose().m30(), .001);
            assertEquals(frame.top(), pose.last().pose().m31(), .001);
            assertEquals(frame.scale(), pose.last().pose().m00(), .001);
        }
    }
    @Test
    void ordinaryMotionProducesMuchStrongerRollWithoutIdleSpin() {
        var sway = new NurseryCursorInteraction.Sway();
        sway.sample(0,0,10);
        var push = sway.sample(8,6,26);
        assertTrue(push.pitch() > .09 && push.roll() > .35,
                "ordinary movement must visibly respond; prior roll was only ~0.03 rad");
        var pull = sway.sample(0,0,42);
        assertTrue(pull.pitch() < 0 && pull.roll() < 0);
        for (int t=58;t<900;t+=16) pull = sway.sample(0,0,t);
        assertEquals(0,pull.roll(),.001);
        var flick = sway.sample(10_000, 10_000, 900);
        assertTrue(Math.abs(flick.pitch()) <= .47 && Math.abs(flick.roll()) <= .75);
    }
    @Test void patioFitsLongTallAndTinyGeometryOnFloorOrAlongConfirmedLanding() {
        for (double[] b : new double[][]{{-5,-12,5,0},{-8,-210,8,0},{-180,-20,180,0}}) {
            var fit = NurseryPcLayout.fitStageMesh(b[0],b[1],b[2],b[3],173,111,84,70);
            assertEquals(173, (b[0]+b[2])*fit.scale()/2+fit.tx(), .001);
            assertEquals(111, b[3]*fit.scale()+fit.ty(),.001);
            assertTrue((b[2]-b[0])*fit.scale() <= 84.001);
            assertTrue((b[3]-b[1])*fit.scale() <= 70.001);
            if(b[3]-b[1]<20) assertTrue(fit.scale() > 1.4);
        }
    }
    @Test void onlyVerifiedDepositCanTriggerALanding() {
        var uuid=java.util.UUID.randomUUID();
        var move=new bat.cobblemondaycare.nursery.model.NurseryTransfer(true,2,4,true,uuid);
        assertTrue(NurseryPcLayout.landingConfirmed(move,2,true,null,uuid));
        assertFalse(NurseryPcLayout.landingConfirmed(move,2,true,uuid,uuid));
        assertFalse(NurseryPcLayout.landingConfirmed(move,2,false,null,uuid));
        assertFalse(NurseryPcLayout.landingConfirmed(move,1,true,null,uuid));
        assertFalse(NurseryPcLayout.landingConfirmed(move,2,true,null,java.util.UUID.randomUUID()));
        assertFalse(NurseryPcLayout.landingConfirmed(null,2,true,null,uuid));
    }
    @Test void actualStageProjectionPublishesTheRenderedBoundsAndKeepsItsBottomOnTheFloor() {
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.translate(100,60,13);
        pose.scale(1.6F,1.6F,1);
        var result = new java.util.ArrayList<NurseryGhostProjection.StageBounds>();
        var stage = new NurseryGhostProjection.Stage(118,105,95,147,42,result::add);
        assertTrue(NurseryGhostProjection.alignStageGeometry(pose,stage,(m,out)->{
            for (int x:new int[]{-30,30}) for (int y:new int[]{-80,10})
                out.addVertex(m.last().pose(),x,y,0);
        }));
        var projected=pose.last().pose();
        var a=projected.transformPosition(-30,-80,0,new org.joml.Vector3f());
        var b=projected.transformPosition(30,10,0,new org.joml.Vector3f());
        var box=result.getFirst();
        assertEquals(a.x,box.x0(),.001);
        assertEquals(a.y,box.y0(),.001);
        assertEquals(b.x,box.x1(),.001);
        assertEquals(b.y,box.y1(),.001);
        assertEquals(105,b.y,.001);
        assertTrue(a.x>=95 && b.x<=147);
        assertTrue(b.y-a.y<=42.001);
    }
    @Test void higherLandingIsContinuousAndUsesRealModelHeadroomAtSeveralFps() {
        for (double guiScale : new double[]{.62,1}) for (int modelHeight : new int[]{22,42,140}) {
            var first=new NurseryLandingMotion(75);
            double initialY=stageLandingFoot(first,100,guiScale,modelHeight);
            assertTrue(initialY < 100*guiScale-17*guiScale);
            double midY=stageLandingFoot(first,410,guiScale,modelHeight);
            assertTrue(midY > initialY && midY < 100*guiScale);
            double previous=initialY;
            for (int t=110;t<=720;t+=10) {
                double y=stageLandingFoot(first,t,guiScale,modelHeight);
                assertTrue(y >= previous-.001 && y <= 100*guiScale+.001);
                previous=y;
            }
            assertEquals(100*guiScale, stageLandingFoot(first,720,guiScale,modelHeight),.001);
            assertTrue(stageLandingFoot(first,719,guiScale,modelHeight) > 100*guiScale-.01);
            for (int fps : new int[]{20,60,144}) {
                var replay = new NurseryLandingMotion(75);
                stageLandingFoot(replay,100,guiScale,modelHeight);
                for(double t=100+1000.0/fps;t<410;t+=1000.0/fps)
                    stageLandingFoot(replay,(long)t,guiScale,modelHeight);
                assertEquals(midY,stageLandingFoot(replay,410,guiScale,modelHeight),.001);
            }
        }
    }

    private static double stageLandingFoot(NurseryLandingMotion landing,long time,double scale,int modelHeight) {
        var pose=new com.mojang.blaze3d.vertex.PoseStack();
        pose.scale((float)scale,(float)scale,1);
        var published=new java.util.ArrayList<NurseryGhostProjection.StageBounds>();
        var stage=new NurseryGhostProjection.Stage(70*scale,100*scale,10*scale,130*scale,70*scale,
                10*scale,scale,time,landing,published::add);
        assertTrue(NurseryGhostProjection.alignStageGeometry(pose,stage,(m,out)->{
            for (int x:new int[]{-18,18}) for (int y:new int[]{-modelHeight,0})
                out.addVertex(m.last().pose(),x,y,0);
        }));
        var b=published.getFirst();
        assertTrue(b.y0()>=10*scale-.001);
        assertEquals(b.y1(),pose.last().pose().m31(),.001);
        return b.y1();
    }
    @Test void projectedDropAdvancesSeveralTimesWithinASingleServerTick() {
        for(int fps:new int[]{60,120}) {
            var drop=new NurseryLandingMotion(100);
            stageLandingFoot(drop,100,1,31);
            double previous=stageLandingFoot(drop,350,1,31);
            for(int i=1;i<=2;i++) {
                double next=stageLandingFoot(drop,350+(long)(i*1000.0/fps),1,31);
                assertTrue(next>previous+.12,"different render frames within one 50ms tick must move");
                previous=next;
            }
        }
        var first=new NurseryLandingMotion(100);
        var delayed=new NurseryLandingMotion(100);
        assertEquals(stageLandingFoot(first,100,1,31),stageLandingFoot(delayed,180,1,31),.001);
    }
}
