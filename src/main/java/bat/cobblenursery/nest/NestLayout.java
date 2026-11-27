package bat.cobblenursery.nest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Visual rank is packed by occupancy; slot, egg identity and timer never move. All pose values are model pixels. */
public final class NestLayout {
    public record Pose(int slot,double x,double z,float scale,float yaw,float pitch,float roll,double sole) {
        public Pose(int slot,double x,double z,float scale) { this(slot,x,z,scale,0,0,0,1.9); }
        public double halfWidth() { return 5.15 * scale; }
        public double top() { return sole + 14.03 * scale; }
        public Quaternionf rotation() {
            return new Quaternionf().rotateY((float)Math.toRadians(yaw))
                    .rotateX((float)Math.toRadians(pitch)).rotateZ((float)Math.toRadians(roll));
        }
        public Vec3 modelToNest(double mx,double my,double mz) {
            Vector3f v = rotation().transform(new Vector3f((float)(mx-8),(float)(my-1),(float)(mz-8)).mul(scale));
            return new Vec3((x+v.x)/16,(sole+v.y)/16,(z+v.z)/16);
        }
        Vec3 nestToModel(Vec3 p) {
            Vector3f v = rotation().conjugate().transform(
                    new Vector3f((float)(p.x*16-x),(float)(p.y*16-sole),(float)(p.z*16-z))).div(scale);
            return new Vec3(v.x+8,v.y+1,v.z+8);
        }
    }
    // Shared typed-egg model's six crossed bands: bottom, top, center half-axes and cap half-width.
    private static final double[][] EGG_BANDS = {
        {1,2.4,3.10,2.30,2.30},
        {2.4,4.75,4.32,3.27,3.27},
        {4.75,8.55,5.15,3.94,3.94},
        {8.55,11.48,4.79,3.69,3.69},
        {11.48,13.65,3.92,3.00,3.00},
        {13.65,15.03,2.72,2.00,2.00}
    };
    public record Box(double x0,double y0,double z0,double x1,double y1,double z1) {
        boolean contains(double x,double y,double z) {
            double e=.001; // 1/16,000 block: accept real edges, not air beyond the model.
            return x>=x0-e && x<=x1+e && y>=y0-e && y<=y1+e && z>=z0-e && z<=z1+e;
        }
    }

    private static final List<Box> RAW_BOXES = rawEggBoxes();
    private static List<Box> rawEggBoxes() {
        List<Box> boxes=new ArrayList<>(18);
        for (double[] band:EGG_BANDS) {
            boxes.add(new Box(8-band[2],band[0],8-band[3],8+band[2],band[1],8+band[3]));
            boxes.add(new Box(8-band[4],band[0],8-band[2],8+band[4],band[1],8-band[3]));
            boxes.add(new Box(8-band[4],band[0],8+band[3],8+band[4],band[1],8+band[2]));
        }
        return List.copyOf(boxes);
    }

    /** Conservative native outline; the server ray uses exact inverse-rotated cuboids, below. */
    public static List<Box> eggBoxes(Pose pose) {
        List<Box> result=new ArrayList<>(18);
        for (Box b : RAW_BOXES) {
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0;
            double x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            for (double x:new double[]{b.x0,b.x1}) for (double y:new double[]{b.y0,b.y1})
                for (double z:new double[]{b.z0,b.z1}) {
                    Vec3 p=pose.modelToNest(x,y,z).scale(16);
                    x0=Math.min(x0,p.x); y0=Math.min(y0,p.y); z0=Math.min(z0,p.z);
                    x1=Math.max(x1,p.x); y1=Math.max(y1,p.y); z1=Math.max(z1,p.z);
                }
            result.add(new Box(x0,y0,z0,x1,y1,z1));
        }
        return result;
    }

    static double eggRay(Pose pose,Vec3 start,Vec3 end) {

        Vec3 local=pose.nestToModel(start), delta=pose.nestToModel(end).subtract(local);
        double first=Double.POSITIVE_INFINITY;
        for (Box box: RAW_BOXES) first=Math.min(first,boxEntry(box,local,delta));
        return first;
    }
    private static double boxEntry(Box b,Vec3 start,Vec3 move) {
        double enter=0,exit=1;
        double[] p={start.x,start.y,start.z}, d={move.x,move.y,move.z},
                lo={b.x0,b.y0,b.z0}, hi={b.x1,b.y1,b.z1};
        for(int i=0;i<3;i++) {
            if (Math.abs(d[i])<1e-8) {
                if(p[i]<lo[i]-.0001 || p[i]>hi[i]+.0001) return Double.POSITIVE_INFINITY;
            } else {
                double a=(lo[i]-p[i])/d[i], z=(hi[i]-p[i])/d[i];
                enter=Math.max(enter,Math.min(a,z));
                exit=Math.min(exit,Math.max(a,z));
                if (enter>exit+.000001) return Double.POSITIVE_INFINITY;
            }
        }
        return enter;
    }
    public static int hitRay(int mask,Vec3 start,Vec3 end,double blockingFraction) {
        double best=Math.min(blockingFraction+1e-7,1.0000001);
        int slot=-1;
        for (Pose pose:poses(mask)) {
            double entry=eggRay(pose,start,end);
            if (entry<best) { best=entry; slot=pose.slot(); }
        }
        return slot;
    }

    private static final double[][][] SHAPES = {
        {},
        {{8,8,.55,0,0,0,1.9}},
        {{6.65,5.77,.50,-4,-7,6,2.20},{9.20,10.28,.47,3,10,-11,2.35}},
        {{5.74,6.15,.44,-4,-6,8,2.18},{10.39,6.55,.44,5,-8,-4,2.15},{8.20,10.55,.43,-6,15,-5,2.32}},
        {{6.05,5.75,.420,-6,-10,9,2.26},{10.25,6.25,.390,8,-7,-6,2.13},
         {6.35,9.62,.385,4,15,20,2.51},{10.10,10.12,.405,-3,8,-9,2.21}}
    };
    private static final List<List<Pose>> LAYOUTS = buildLayouts();
    private NestLayout() {}

    private static List<List<Pose>> buildLayouts() {
        List<List<Pose>> all = new ArrayList<>(16);
        for (int mask=0;mask<16;mask++) {
            var shape=SHAPES[Integer.bitCount(mask)];
            List<Pose> poses = new ArrayList<>(shape.length);
            for (int slot=0;slot<NestIncubation.SEATS;slot++) if ((mask & (1 << slot))!=0) {
                var p=shape[poses.size()];
                poses.add(new Pose(slot,p[0],p[1],(float)p[2],(float)p[3],(float)p[4],(float)p[5],p[6]));
            }
            all.add(List.copyOf(poses));
        }
        return List.copyOf(all);
    }

    public static List<Pose> poses(int mask) { return LAYOUTS.get(mask & 15); }
    private static final double[][] FOUR_CLEARANCE = {{.18,.04,.19},{-.12,.045,.10},{0,0,0},{-.13,.045,-.14}};
    private static final double[] TRIANGLE_CLEARANCE = {.10,0,.12};
    public static Pose sized(Pose base,int tier) { return sized(base,tier,1); }
    public static Pose sized(Pose base,int tier,int mask) {
        int count=Integer.bitCount(mask), rank=Integer.bitCount(mask & ((1<<base.slot())-1));
        double scale = NestLifeVisual.scale(tier), contact = 1.89;
        double x=base.x(), z=base.z(), sole=contact+(base.sole()-contact)*scale;
        if (count==4 || count==3 && rank==0) {
            // Both endpoint layouts remain unchanged. Clear the rim at intermediate scales.
            double bell=Math.sin(Math.PI*(scale-.80)/.20);
            double[] offset=count==4 ? FOUR_CLEARANCE[rank] : TRIANGLE_CLEARANCE;
            x+=offset[0]*bell; sole+=offset[1]*bell; z+=offset[2]*bell;
        }
        return new Pose(base.slot(),x,z,(float)(base.scale()*scale),
                base.yaw(),base.pitch(),base.roll(),sole);
    }
    public static Pose moving(Pose stable,int count,long remaining,int seed) {
        double shake = NestLifeVisual.shake(remaining,seed);
        if (shake==0) return stable;
        return new Pose(stable.slot(),stable.x(),stable.z(),stable.scale(),stable.yaw(),stable.pitch(),
                stable.roll()+(float)(shake*amplitude(count)),stable.sole());
    }
    private static double amplitude(int count) { return count == 4 ? 1 : 2; }
    public static double sweepMargin(Pose pose,int count) {
        return (14.03+5.15)*pose.scale()*Math.sin(Math.toRadians(amplitude(count)))+.01;
    }

    public static int hitSizedRay(int mask,Vec3 start,Vec3 end,double blockingFraction,
                                  java.util.function.IntUnaryOperator tier,
                                  java.util.function.IntToLongFunction remaining,
                                  java.util.function.IntUnaryOperator seed) {
        int count=Integer.bitCount(mask), direct=-1, near=-1;
        double best=Math.min(blockingFraction+1e-7,1.0000001);
        double[] depths={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        boolean ambiguous=false;
        for (Pose base:poses(mask)) {
            int slot=base.slot();
            var pose=sized(base,tier.applyAsInt(slot),mask);
            long time=remaining.applyAsLong(slot);
            int animationSeed=seed.applyAsInt(slot);
            double entry=eggRay(moving(pose,count,time,animationSeed),start,end);
            if (entry<best) { best=entry; direct=slot; }
            if (!NestLifeVisual.nearMotion(time,animationSeed)) continue;
            // Network delay can leave the player looking at a slightly earlier phase.
            // Use only exact poses (not the conservative outline) and reject ambiguity.
            for(int offset=-90;offset<=270;offset+=45) {
                long sample=time+offset;
                depths[slot]=Math.min(depths[slot],eggRay(moving(pose,count,sample,animationSeed),start,end));
            }
            if (depths[slot]>=blockingFraction) continue;
            if (near>=0 && near!=slot) ambiguous=true;
            near=slot;
        }
        if (direct>=0) {
            for (int slot=0;slot<depths.length;slot++) if (slot!=direct && depths[slot]<best) return -1;
            return direct;
        }
        return ambiguous ? -1 : near;
    }

    public static int firstFree(int mask) {
        int free = (~mask) & 15;
        return free==0 ? -1 : Integer.numberOfTrailingZeros(free);
    }
    public static int hit(int mask,double x,double y,double z) {
        if (!(x>=0 && x<=1 && y>=0 && y<=1 && z>=0 && z<=1)) return -1;
        for (Pose p:poses(mask)) {
            Vec3 q=p.nestToModel(new Vec3(x,y,z));
            for (Box box:RAW_BOXES) if(box.contains(q.x,q.y,q.z)) return p.slot();
        }
        return -1;
    }
}