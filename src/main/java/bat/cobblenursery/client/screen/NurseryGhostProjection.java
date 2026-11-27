package bat.cobblenursery.client.screen;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Scopes the exact posed-geometry probe to a single carried model render. */
public final class NurseryGhostProjection {
    private static final ThreadLocal<Target> TARGET = new ThreadLocal<>();

    static void centerGhost(double x, double y, double gap, Runnable renderer) {
        centerGhost(x, y, gap, 0, renderer);
    }
    static void centerGhost(double x, double y, double gap, float roll, Runnable renderer) {
        Target previous = TARGET.get();
        TARGET.set(new Target(x, y, gap, roll));
        try {
            renderer.run();
        } finally {
            if (previous == null) TARGET.remove();
            else TARGET.set(previous);
        }
    }

    record Stage(double center, double floor, double left, double right, double height,
                 double top, double guiScale, long renderMs, NurseryLandingMotion landing,
                 java.util.function.Consumer<StageBounds> publish) {
        Stage(double center,double floor,double left,double right,double height,
              java.util.function.Consumer<StageBounds> publish) {
            this(center,floor,left,right,height,Double.NEGATIVE_INFINITY,1,0,null,publish);
        }
    }
    record StageBounds(double x0,double y0,double x1,double y1) {}

    static void fitStage(Stage stage, Runnable renderer) {
        Target previous = TARGET.get();
        var target = new Target(0,0,0,0);
        target.stage = stage;
        TARGET.set(target);
        try { renderer.run(); }
        finally { if (previous == null) TARGET.remove(); else TARGET.set(previous); }
    }

    static boolean fittingStage() {
        Target target = TARGET.get();
        return target != null && target.stage != null;
    }

    static void alignStageSprite(PoseStack pose) {
        var target = TARGET.get();
        if (target == null || target.stage == null || target.sampled) return;
        target.sampled = true;
        alignStageGeometry(pose,target.stage,(m,out)->{
            for(int x:new int[]{-18,18}) for(int y:new int[]{0,36}) out.addVertex(m.last().pose(),x,y,0);
        });
    }

    static boolean alignStageGeometry(PoseStack renderPose, Stage stage, Geometry geometry) {
        var probe = new PoseStack();
        probe.last().pose().set(renderPose.last().pose());
        probe.last().normal().set(renderPose.last().normal());
        var bounds = new Bounds();
        try { geometry.emit(probe,new BoundsConsumer(bounds)); }
        catch (RuntimeException ignored) { return false; }
        if (!Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY)) return false;
        double width = bounds.maxX-bounds.minX;
        double usable = stage.right-stage.left;
        var fit = NurseryPcLayout.fitStageMesh(bounds.minX,bounds.minY,bounds.maxX,bounds.maxY,
                stage.center,stage.floor,usable,stage.height);
        double renderedHeight = (bounds.maxY-bounds.minY)*fit.scale();
        // Exact mesh top guard: animation only changes translation, never size, and
        // cannot send the current posed geometry past the aperture's top.
        double lift = stage.landing == null ? 0 : stage.guiScale * stage.landing.lift(stage.renderMs,
                (stage.floor-stage.top-renderedHeight)/stage.guiScale);
        double floor = stage.floor-lift;
        double half = width*fit.scale()*.5;
        double center = Math.clamp(stage.center,stage.left+half,Math.max(stage.left+half,stage.right-half));
        double tx = fit.tx() + center - stage.center;
        var matrix = renderPose.last().pose();
        matrix.scaleLocal((float)fit.scale(),(float)fit.scale(),1);
        matrix.m30(matrix.m30()+(float)tx);
        matrix.m31(matrix.m31()+(float)(fit.ty()-lift));
        stage.publish.accept(new StageBounds(center-half,floor-renderedHeight,center+half,floor));
        return true;
    }

    static boolean sampling() { return TARGET.get() != null; }

    public static void alignPosedModel(PosableModel model, RenderContext context, PoseStack renderPose,
                                      int light, int overlay, int color) {
        Target target = TARGET.get();
        if (target == null || target.sampled) return;
        target.sampled = true;
        if (target.stage != null) {
            alignStageGeometry(renderPose,target.stage,
                (probe,vertices)->model.getRootPart().render(context,probe,vertices,light,overlay,color));
            return;
        }
        alignGeometry(renderPose, target.x, target.y, target.gap, target.roll,
                (probe, vertices) -> model.getRootPart().render(context, probe, vertices, light, overlay, color));
    }

    interface Geometry { void emit(PoseStack pose, VertexConsumer consumer); }

    static boolean alignGeometry(PoseStack renderPose, double x, double y, double gap, Geometry geometry) {
        return alignGeometry(renderPose, x, y, gap, 0, geometry);
    }

    static boolean alignGeometry(PoseStack renderPose, double x, double y, double gap, float roll, Geometry geometry) {
        var render = renderPose.last();
        var oriented = new org.joml.Matrix4f(render.pose());
        if (roll != 0) oriented.rotateLocalZ(roll);
        var probe = new PoseStack();
        probe.last().pose().set(oriented);
        probe.last().normal().set(render.normal());
        var bounds = new Bounds();
        try {
            // Walk the already-posed geometry on a private matrix and a CPU-only buffer.
            geometry.emit(probe, new BoundsConsumer(bounds));
        } catch (RuntimeException ignored) {
            return false; // Unsupported custom poser: keep rendering, not a broken screen.
        }
        var shift = bounds.shiftTo(x, y, gap);
        if (shift == null) return false;
        var pose = render.pose().set(oriented);
        if (roll != 0) render.normal().set(new org.joml.Matrix3f().rotationZ(roll).mul(render.normal()));
        // After the complete stock transform, the captured vertices are in GUI space.
        pose.m30(pose.m30() + (float) shift.x());
        pose.m31(pose.m31() + (float) shift.y());
        return true;
    }

    static void alignNativeSprite(PoseStack pose, double x, double y, double gap, float roll) {
        // Cobblemon's PROFILE sprite submits a 2x2 quad after native render scale 20;
        // its vertices are x=[-20,20], y=[0,40] in the entering draw pose.
        alignGeometry(pose, x, y, gap, roll, (m, vertices) -> {
            for (int px : new int[]{-20,20}) for (int py : new int[]{0,40})
                vertices.addVertex(m.last().pose(), px, py, 0);
        });
    }

    private static final class Target {
        final double x, y, gap;
        final float roll;
        boolean sampled;
        Stage stage;
        Target(double x, double y, double gap, float roll) { this.x = x; this.y = y; this.gap = gap; this.roll = roll; }
    }

    record Shift(double x, double y) {}

    static final class Bounds {
        private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        void include(double x, double y) {
            if (!Double.isFinite(x) || !Double.isFinite(y)) return;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        double midX() { return minX + (maxX - minX) * .5; }
        double minY() { return minY; }
        Shift shiftTo(double x, double y, double gap) {
            if (!Double.isFinite(minX) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(gap)) return null;
            return new Shift(x - midX(), y + gap - minY);
        }
    }

    private record BoundsConsumer(Bounds bounds) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { bounds.include(x,y); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    private NurseryGhostProjection() {}
}