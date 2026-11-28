package bat.cobblemondaycare.client.render;

import bat.cobblemondaycare.nest.IncubationNestBlockEntity;
import bat.cobblemondaycare.nest.NestLifeVisual;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Client-only onset detection for the visible egg motion; no chunk scanning or network traffic. */
final class NestWiggleSound {
    private static final SoundEvent SHAKE=SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblemon","poke_ball.shake"));
    private static final SoundEvent READY_SHAKE=SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblemon","poke_ball.shake.ancient"));
    private final WeakHashMap<IncubationNestBlockEntity,Slot[]> history=new WeakHashMap<>();
    private static final class Slot {
        long observed, remaining, played=Long.MIN_VALUE;
        int seed;
    }

    void observe(IncubationNestBlockEntity nest,int slot,long remaining,int seed,long now) {
        var minecraft=Minecraft.getInstance();
        var center=Vec3.atCenterOf(nest.getBlockPos());
        if(minecraft.level==null || minecraft.player==null || minecraft.player.distanceToSqr(center)>81) return;
        var slots=history.computeIfAbsent(nest,key -> new Slot[4]);
        Slot old=slots[slot];
        if(old==null) slots[slot]=old=new Slot();
        long elapsed=NestLifeVisual.shakeElapsed(remaining,seed);
        long onset=remaining+elapsed;
        if(old.seed!=seed) old.played=Long.MIN_VALUE;
        if(old.seed==seed && old.observed>0 && now-old.observed<=180
                && elapsed>=0 && elapsed<=175 && old.remaining>onset && old.played!=onset) {
            old.played=onset;
            // The native event chooses one of Cobblemon's four stock files.
            minecraft.level.playLocalSound(center.x,center.y,center.z,remaining<=0 ? READY_SHAKE : SHAKE,SoundSource.BLOCKS,.45F,1F,false);
        }
        old.observed=now;
        old.remaining=remaining;
        old.seed=seed;
    }
}

