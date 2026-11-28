package bat.cobblemondaycare.nest;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.function.IntUnaryOperator;

/** Small native targeting/hover envelope; actual egg interactions still use rotated mesh rays. */
public final class NestOutline {
    private static VoxelShape box(double x0,double y0,double z0,double x1,double y1,double z1) {
        return Shapes.box(x0/16,y0/16,z0/16,x1/16,y1/16,z1/16);
    }
    public static final VoxelShape BASE=Shapes.or(box(1,0,1,15,1.9,15),
            box(1,0,1,3.1,3.8,15),box(12.9,0,1,15,3.8,15),
            box(3.1,0,1,12.9,3.8,3.1),box(3.1,0,12.9,12.9,3.8,15));
    private NestOutline() {}
    public static int turn(Direction facing) {
        return switch(facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }
    static Vec3 point(Vec3 point,int turn) {
        return switch(turn & 3) {
            case 1 -> new Vec3(1-point.z,point.y,point.x);
            case 2 -> new Vec3(1-point.x,point.y,1-point.z);
            case 3 -> new Vec3(point.z,point.y,1-point.x);
            default -> point;
        };
    }
    static VoxelShape rotated(VoxelShape shape,int turn) {
        if (turn==0) return shape;
        VoxelShape result=Shapes.empty();
        for(var b:shape.toAabbs()) {
            var a=point(new Vec3(b.minX,b.minY,b.minZ),turn);
            var c=point(new Vec3(b.maxX,b.maxY,b.maxZ),turn);
            result=Shapes.or(result,Shapes.box(Math.min(a.x,c.x),a.y,Math.min(a.z,c.z),
                    Math.max(a.x,c.x),c.y,Math.max(a.z,c.z)));
        }
        return result;
    }
    public static VoxelShape occupied(int mask,IntUnaryOperator tier) {
        VoxelShape shape=BASE;
        int count=Integer.bitCount(mask);
        for (var base:NestLayout.poses(mask)) {
            var pose=NestLayout.sized(base,tier.applyAsInt(base.slot()),mask);
            double margin=NestLayout.sweepMargin(pose,count);
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0;
            double x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            for (var b:NestLayout.eggBoxes(pose)) {
                x0=Math.min(x0,b.x0()); y0=Math.min(y0,b.y0()); z0=Math.min(z0,b.z0());
                x1=Math.max(x1,b.x1()); y1=Math.max(y1,b.y1()); z1=Math.max(z1,b.z1());
            }
            // Merge nearby planes on a quarter-pixel grid, conservatively outward.
            shape=Shapes.or(shape,box(Math.floor((x0-margin)*4)/4,Math.floor((y0-margin)*4)/4,
                    Math.floor((z0-margin)*4)/4,Math.ceil((x1+margin)*4)/4,
                    Math.ceil((y1+margin)*4)/4,Math.ceil((z1+margin)*4)/4));
        }
        return shape;
    }
}