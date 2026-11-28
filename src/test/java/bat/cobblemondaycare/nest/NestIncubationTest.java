package bat.cobblemondaycare.nest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NestIncubationTest {
    private static Map<String,Integer> uniform(int v) {
        return Map.of("hp",v,"attack",v,"defence",v,"special_attack",v,"special_defence",v,"speed",v);
    }
    @Test void nestRackIsAConditionalModelInTheSameBlock() throws Exception {
        var root=java.nio.file.Path.of("src/main/resources");
        var path=root.resolve("assets/cobblemondaycare/blockstates/incubation_nest.json");
        var model=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
        assertTrue(model.has("multipart"), "frame must coexist with the nest model in one block");
        var layers=model.getAsJsonArray("multipart");
        assertEquals(5,layers.size());
        String[] directions={"north","east","south","west"};
        for (int i=0;i<directions.length;i++) {
            var piece=layers.get(i).getAsJsonObject();
            assertEquals(directions[i],piece.getAsJsonObject("when").get("facing").getAsString());
            assertEquals(i*90,piece.getAsJsonObject("apply").get("y").getAsInt());
        }
        assertEquals("true",layers.get(4).getAsJsonObject().getAsJsonObject("when").get("framed").getAsString());
        assertEquals("minecraft:block/scaffolding_stable",
                layers.get(4).getAsJsonObject().getAsJsonObject("apply").get("model").getAsString());
        assertFalse(java.nio.file.Files.readString(root.resolve("fabric.mod.json")).contains("\"cobblemondaycare.mixins.json\""),
                "normal scaffolding must not turn the plain nest into a foundation");
    }

    @Test void nestPlacementRequiresSupportDirectlyBelowIncludingStableScaffolding() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        String blockCode = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/bat/cobblemondaycare/nest/IncubationNestBlock.java"));
        assertTrue(blockCode.contains("boolean canSurvive("), "nest placement must check its underside");
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var water = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        var scaffolding = net.minecraft.world.level.block.Blocks.SCAFFOLDING.defaultBlockState();
        var stable = scaffolding.setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE, 0);
        var unstable = scaffolding.setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE, 7);
        var pos = net.minecraft.core.BlockPos.ZERO;
        var support = new java.util.concurrent.atomic.AtomicReference<>(air);
        var level = new net.minecraft.world.level.BlockGetter() {
            @Override public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos p) {
                return p.equals(pos.below()) ? support.get() : p.equals(pos.east()) ? stone : air;
            }
            @Override public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(net.minecraft.core.BlockPos p) { return null; }
            @Override public net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.core.BlockPos p) {
                return getBlockState(p).getFluidState();
            }
            @Override public int getHeight() { return 384; }
            @Override public int getMinBuildHeight() { return -64; }
        };
        assertFalse(IncubationNestBlock.hasPlacementSupport(level, pos), "a neighboring block does not count");
        support.set(stone);
        assertTrue(IncubationNestBlock.hasPlacementSupport(level, pos));
        support.set(water);
        assertFalse(IncubationNestBlock.hasPlacementSupport(level, pos));
        support.set(stable);
        assertTrue(IncubationNestBlock.hasPlacementSupport(level, pos));
        support.set(unstable);
        assertFalse(IncubationNestBlock.hasPlacementSupport(level, pos));
    }

    @Test void approvedRealTimeFormulaRoundsUp() {
        assertEquals(60, NestIncubation.durationSeconds(uniform(0)));
        assertEquals(1046, NestIncubation.durationSeconds(uniform(30)));
        assertEquals(1080, NestIncubation.durationSeconds(uniform(31)));
        assertEquals(180, NestIncubation.durationSeconds(Map.of("hp",31)));
        assertEquals(360, NestIncubation.durationSeconds(Map.of("hp",31,"speed",31)));
        assertFalse(NestIncubation.validIvs(uniform(32)));
        assertFalse(NestIncubation.validIvs(Map.of("hp",31)));
        assertEquals(1, NestIncubation.secondsRemaining(10_001,10_000));
        assertEquals(0, NestIncubation.secondsRemaining(10_000,10_000));
    }
    @Test void occupiedBreakIsOnlyPreflightedBeforeVanillaAndRecoversAfterSuccess() throws Exception {
        String code=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/bat/cobblemondaycare/nest/IncubationNestBlock.java"));
        assertTrue(code.contains("PlayerBlockBreakEvents.AFTER.register"));
        assertTrue(code.contains("PlayerBlockBreakEvents.CANCELED.register"));
        assertFalse(code.contains("return state.getValue(SEATS)!=0 ? 0"));
    }
    @Test void ownerBreakRecoveryPreservesMaturePendingLegacyEggsAndCustomData() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var access=net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        var dataKey=net.minecraft.core.component.DataComponents.CUSTOM_DATA;
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EGG);
        var original=new net.minecraft.nbt.CompoundTag();
        original.putString("egg_payload","preserved");
        stack.set(dataKey,net.minecraft.world.item.component.CustomData.of(original));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(11));
        long now=2_000_000;
        var owner=java.util.UUID.randomUUID();
        var queue=new net.minecraft.nbt.ListTag();
        for (long deadline:new long[]{now-1,now+200_000,now+1_700_000}) {
            var id=java.util.UUID.randomUUID();
            var seat=new IncubationNestBlockEntity.Seat(id,deadline,stack);
            var recovered=IncubationNestBlock.stampedEgg(seat);
            var tag=recovered.get(dataKey).copyTag();
            var stamp=tag.getCompound("CobblemonDaycareIncubation");
            assertEquals(id,stamp.getUUID("hatch"));
            assertEquals(deadline,stamp.getLong("ready"));
            assertEquals(11,recovered.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value());
            assertTrue(NestIncubation.acceptsRecoveredDeadline(uniform(31),deadline,now));
            tag.remove("CobblemonDaycareIncubation");
            assertEquals(original,tag);
            assertEquals(original,stack.get(dataKey).copyTag());
            queue.add(recovered.save(access));
        }
        var document=new net.minecraft.nbt.CompoundTag(); document.put(owner.toString(),queue);
        assertEquals(document,NestRecovery.load(document,access).save(new net.minecraft.nbt.CompoundTag(),access));
    }

    @Test void recoveredLegacyDeadlinesStillFitTheOriginalWindow() {
        long now=2_000_000;
        assertTrue(NestIncubation.acceptsRecoveredDeadline(uniform(31),now+1_700_000,now));
        assertTrue(NestIncubation.acceptsRecoveredDeadline(uniform(31),now+1_800_000,now));
        assertTrue(NestIncubation.acceptsRecoveredDeadline(uniform(31),now-1,now));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(uniform(31),now+1_800_001,now));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(uniform(0),now+60_001,now));
        assertFalse(NestIncubation.acceptsRecoveredDeadline(uniform(31),0,now));
        assertEquals(1700,NestIncubation.secondsRemaining(now+1_700_000,now));
        assertTrue(NestLifeVisual.MAX_REMAINING_MS>=1_800_000);
    }
    @Test void visibleLayoutsResolveEveryPhysicalSlotEvenAfterSparseRemoval() {
        for (int mask=0;mask<16;mask++) {
            var poses= NestLayout.poses(mask);
            assertEquals(Integer.bitCount(mask),poses.size());
            for (var pose: poses) {
                assertTrue((mask & 1<<pose.slot())!=0);
                var tip=pose.modelToNest(8,15.03,8);
                assertEquals(pose.slot(),NestLayout.hit(mask,tip.x,tip.y,tip.z));
                var rayStart=pose.modelToNest(8,17,8);
                var rayEnd=pose.modelToNest(8,12,8);
                assertEquals(pose.slot(),NestLayout.hitRay(mask,rayStart,rayEnd,1));
                assertEquals(-1,NestLayout.hitRay(mask,rayStart,rayEnd,.10)); // rim or straw before target
            }
            if(mask!=15) assertEquals(Integer.numberOfTrailingZeros((~mask)&15),NestLayout.firstFree(mask));
            else assertEquals(-1,NestLayout.firstFree(mask));
        }
        var single=NestLayout.poses(8).getFirst();
        assertEquals(3,single.slot());
        assertEquals(8,single.x());
        assertEquals(8,single.z());
        var pair=NestLayout.poses(9);
        assertNotEquals(pair.get(0).x(),pair.get(1).x());
        assertNotEquals(pair.get(0).z(),pair.get(1).z());
        assertEquals(-1,NestLayout.hit(8,.05,.5,.05));
        assertEquals(-1,NestLayout.hit(0,.5,.5,.5));
    }
    @Test void ownerIsolatedAndPartialNativeStorageCannotConsumeAnEgg() {
        var owner=java.util.UUID.randomUUID();
        var other=java.util.UUID.randomUUID();
        assertTrue(NestIncubation.mayAccess(null,owner));
        assertTrue(NestIncubation.mayAccess(owner,owner));
        assertFalse(NestIncubation.mayAccess(owner,other));
        assertFalse(NestIncubation.mayAccess(null,null));
        var physical=new Object();
        assertNull(NestIncubation.requireCommitted(null,null,false));
        assertSame(physical,NestIncubation.requireCommitted(physical,physical,true));
        assertThrows(IllegalStateException.class,()->NestIncubation.requireCommitted(physical,null,false));
        assertThrows(IllegalStateException.class,()->NestIncubation.requireCommitted(physical,physical,false));
        assertThrows(IllegalStateException.class,()->NestIncubation.requireCommitted(null,physical,false));
    }
    @Test void taperedTopSideAndBareStrawDoNotShareHitTarget() {
        var pose=NestLayout.poses(8).getFirst();
        double top=pose.top()/16;
        double sideY=(1.9+(6-1)*pose.scale())/16;
        double broadSide=(pose.x()+pose.halfWidth())/16;
        assertEquals(3,NestLayout.hit(8,.5,top,.5));
        assertEquals(-1,NestLayout.hit(8,(pose.x()+2.2)/16,top,(pose.z()+2.2)/16));
        assertEquals(3,NestLayout.hit(8,broadSide,sideY,.5));
        assertEquals(-1,NestLayout.hit(8,.5,(1.9-.03)/16,.5));
        assertEquals(-1,NestLayout.hit(15,.5,1.9/16,.5)); // exposed straw between the four seats
    }
    @Test void eggBoxesMatchAllEighteenModelCuboidsAndNativeRayEdges() throws Exception {
        var base=java.nio.file.Path.of("src/main/resources/assets/cobblemondaycare/models/item/egg/base_shape.json");
        try (var reader=java.nio.file.Files.newBufferedReader(base)) {
            var elements=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements");
            var boxes=NestLayout.eggBoxes(new NestLayout.Pose(0,8,8,1));
            assertEquals(elements.size(),boxes.size());
            for(int i=0;i<boxes.size();i++) {
                var from=elements.get(i).getAsJsonObject().getAsJsonArray("from");
                var to=elements.get(i).getAsJsonObject().getAsJsonArray("to");
                var b=boxes.get(i);
                assertArrayEquals(new double[]{from.get(0).getAsDouble(),from.get(1).getAsDouble()+.9,from.get(2).getAsDouble(),
                                to.get(0).getAsDouble(),to.get(1).getAsDouble()+.9,to.get(2).getAsDouble()},
                        new double[]{b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()},.00001);
            }
        }
        var pose=NestLayout.poses(8).getFirst();
        var shape=net.minecraft.world.phys.shapes.Shapes.empty();
        for(var b:NestLayout.eggBoxes(pose))
            shape=net.minecraft.world.phys.shapes.Shapes.or(shape,net.minecraft.world.phys.shapes.Shapes.box(
                    b.x0()/16,b.y0()/16,b.z0()/16,b.x1()/16,b.y1()/16,b.z1()/16));
        var nativeHit=shape.clip(new net.minecraft.world.phys.Vec3(.99,.33,.5),
                new net.minecraft.world.phys.Vec3(.25,.33,.5),net.minecraft.core.BlockPos.ZERO);
        assertNotNull(nativeHit);
        var point=nativeHit.getLocation();
        assertEquals(3,NestLayout.hit(8,point.x,point.y,point.z));
    }
    @Test void rotatedShapeUsesExactCuboidRayAndRejectsOutlineOnlyCorners() {
        var pose = new NestLayout.Pose(3,8,8,.5F,0,28,-12,1.9);
        var centered = pose.modelToNest(8,15.03,8);
        var before = pose.modelToNest(8,20,8);
        var after = pose.modelToNest(8,0,8);
        var hit=NestLayout.eggRay(pose,before,after);
        assertTrue(Double.isFinite(hit));
        assertEquals(centered.x,before.lerp(after,hit).x,.001);
        assertEquals(centered.y,before.lerp(after,hit).y,.001);
        assertEquals(centered.z,before.lerp(after,hit).z,.001);
        assertEquals(Double.POSITIVE_INFINITY,NestLayout.eggRay(pose,pose.modelToNest(10.6,20,10.6),pose.modelToNest(10.6,14,10.6)));
    }
    @Test void fullNestIsIrregularAndEachSoleStillMeetsStraw() {
        var four=NestLayout.poses(15);
        assertTrue(four.stream().map(NestLayout.Pose::x).distinct().count()==4);
        assertTrue(four.stream().map(NestLayout.Pose::z).distinct().count()==4);
        assertTrue(four.stream().anyMatch(p -> Math.abs(p.roll())>=20));
        double[] contact={1.89,1.89,1.89,1.89};
        for (int i=0;i<four.size();i++) {
            var lowest=NestLayout.eggBoxes(four.get(i)).stream().mapToDouble(NestLayout.Box::y0).min().orElseThrow();
            assertEquals(contact[i],lowest,.07);
        }
    }
    @Test void visualSizeUsesAllRawIvsAndReadyEggsKeepWiggling() {
        assertEquals(.8,NestLifeVisual.scale(NestLifeVisual.sizeTier(uniform(0))),.0001);
        assertEquals(1,NestLifeVisual.scale(NestLifeVisual.sizeTier(uniform(31))),.0001);
        assertTrue(NestLifeVisual.scale(NestLifeVisual.sizeTier(uniform(30)))>.985);
        assertEquals(0,NestLifeVisual.shake(0,17),.0001);
        assertEquals(0,NestLifeVisual.shake(-1,17),.0001);
        int readyStarts=0;
        for(long ms=0;ms>-20_000;ms--) {
            if (NestLifeVisual.shakeElapsed(ms,17)==0) readyStarts++;
        }
        assertTrue(readyStarts>=4 && readyStarts<=5,"a ready egg should wiggle regularly until picked up");
        assertEquals(0,NestLifeVisual.pulse(0),.0001);
        assertNotEquals(0,NestLifeVisual.pulse(60));
        assertEquals(0,NestLifeVisual.pulse(290),.0001);
        assertNotEquals(0,NestLifeVisual.pulse(420));
        assertEquals(0,NestLifeVisual.pulse(700),.0001);
        int near = 0, far = 0;
        for (int i=1;i<=24_000;i+=10) {
            if (Math.abs(NestLifeVisual.shake(i,17))>.005) near++;
            if (Math.abs(NestLifeVisual.shake(i+150_000,17))>.005) far++;
        }
        assertTrue(near>2*far);
    }
    @Test void resizedRaySelectsActualSmallEggAndMotionNeverChangesSeatOrReadyPose() {
        var raw=NestLayout.poses(8).getFirst();
        var small=NestLayout.sized(raw,0);
        var large=NestLayout.sized(raw,NestLifeVisual.MAX_TIER);
        assertEquals(.8, small.scale()/large.scale(),.0001);
        var start=small.modelToNest(8,17,8);
        var end=small.modelToNest(8,12,8);
        assertEquals(3,NestLayout.hitSizedRay(8,start,end,1,k->0,k->0,k->19));
        assertEquals(-1,NestLayout.hitSizedRay(8,start,end,.01,k->0,k->0,k->19));
        var sizeOnlyStart=large.modelToNest(12.9,6,-1);
        var sizeOnlyEnd=large.modelToNest(12.9,6,15);
        assertEquals(3,NestLayout.hitSizedRay(8,sizeOnlyStart,sizeOnlyEnd,1,k->32,k->0,k->19));
        assertEquals(-1,NestLayout.hitSizedRay(8,sizeOnlyStart,sizeOnlyEnd,1,k->0,k->0,k->19));
        assertSame(small,NestLayout.moving(small,1,0,19));
        long sample=1;
        while(sample<24_000 && Math.abs(NestLifeVisual.shake(sample,19))<.5) sample++;
        assertTrue(sample<24_000);
        var moving=NestLayout.moving(small,1,sample,19);
        assertEquals(small.slot(),moving.slot());
        assertEquals(small.scale(),moving.scale());
        assertEquals(small.sole(),moving.sole());
        assertNotEquals(small.roll(),moving.roll());
        boolean differentSeeds=false;
        for (long ms=1;ms<18_000;ms+=15)
            if (Math.abs(NestLifeVisual.shake(ms,19)-NestLifeVisual.shake(ms,20))>.1) {
                differentSeeds=true; break;
            }
        assertTrue(differentSeeds);
    }
    @Test void hoveredOutlineStaysBoundedWithOneThroughFourDifferentEggSizes() {
        for (int mask=1;mask<16;mask++) {
            var outline=NestOutline.occupied(mask,slot->slot*10);
            int[] edges={0};
            outline.forAllEdges((x0,y0,z0,x1,y1,z1)->edges[0]++);
            assertTrue(edges[0]<140,"native outline edges for mask "+mask+": "+edges[0]);
            for (var pose:NestLayout.poses(mask)) {
                var p=NestLayout.sized(pose,pose.slot()*10,mask);
                var crown=p.modelToNest(8,15.03,8);
                var hit=outline.clip(crown.add(0,.25,0),crown.add(0,-.015,0),net.minecraft.core.BlockPos.ZERO);
                assertNotNull(hit,"visible egg crown must still target the nest");
                var outside=new net.minecraft.world.phys.Vec3(1.01,.90,.50);
                assertNull(outline.clip(outside,outside.add(0,-.9,0),net.minecraft.core.BlockPos.ZERO));
            }
        }
    }
    @Test void warmNestAddsIndependentBonusesAndNeverSpeedsUpOfflineHours() throws Exception {
        var cls=Class.forName("bat.cobblemondaycare.nest.NestWarmthRules");
        var mask=cls.getDeclaredMethod("mask",boolean.class,int.class,boolean.class);
        var bonus=cls.getDeclaredMethod("bonus",int.class);
        int full=(int)mask.invoke(null,true,10,true);
        assertEquals(60,bonus.invoke(null,full));
        assertEquals(15,bonus.invoke(null,mask.invoke(null,false,10,false)));
        assertEquals(15,bonus.invoke(null,mask.invoke(null,true,9,false)));
        assertEquals(30,bonus.invoke(null,mask.invoke(null,false,0,true)));
        var credit=cls.getDeclaredMethod("credit",long.class,long.class,long.class,long.class,int.class,int.class);
        assertEquals(600L,credit.invoke(null,1000L,2000L,20L,1000L,full,full));
        assertEquals(300L,credit.invoke(null,1000L,2000L,20L,1500L,full,full));
        assertEquals(0L,credit.invoke(null,1000L,3_601_000L,20L,1000L,full,full));
        assertEquals(0L,credit.invoke(null,1000L,2000L,5000L,1000L,full,full));
        assertEquals(0L,credit.invoke(null,1000L,2000L,20L,1000L,full,0));
        var qualifies=cls.getDeclaredMethod("qualifies",String.class,String.class);
        for(var species:java.util.List.of("togepi","exeggcute","chansey","blissey","happiny","vullaby","togetic","kangaskhan","maushold"))
            assertTrue((boolean)qualifies.invoke(null,"cobblemon:"+species,"runaway"));
        assertTrue((boolean)qualifies.invoke(null,"cobblemon:magmar","flamebody"));
        assertFalse((boolean)qualifies.invoke(null,"cobblemon:magmar","vitalspirit"));
        assertFalse((boolean)qualifies.invoke(null,"other:chansey","runaway"));
    }
    @Test void incubationStatusListsAllThreeBonusesAndMarksOnlyMissingConditions() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        String prefix="gui.cobblemondaycare.nest.";
        for (int mask=0;mask<8;mask++) {
            var result=NestStatusText.format(172_000L,100_000L,mask);
            var content=(net.minecraft.network.chat.contents.TranslatableContents)result.getContents();
            var args=content.getArgs();
            if (mask==0) {
                assertEquals(prefix+"remaining_normal",content.getKey());
                assertEquals("1:12",args[0]);
                assertEquals(2,args.length,"No boosts must still report all three conditions");
            } else {
                assertEquals(prefix+"remaining_details",content.getKey());
                assertEquals(3,args.length);
                assertEquals(NestWarmthRules.bonus(mask),args[1]);
                if (mask==7) assertEquals("0:45",args[0]);
            }
            var details=(net.minecraft.network.chat.Component)args[args.length-1];
            var entries=details.getSiblings().stream()
                    .map(net.minecraft.network.chat.Component::getContents)
                    .filter(net.minecraft.network.chat.contents.TranslatableContents.class::isInstance)
                    .map(net.minecraft.network.chat.contents.TranslatableContents.class::cast).toList();
            assertEquals(3,entries.size());
            var keys=java.util.List.of("room","light","caregiver");
            int[] boosts={15,15,30};
            for (int i=0;i<3;i++) {
                boolean active=(mask&(1<<i))!=0;
                assertEquals(prefix+"boost_"+keys.get(i)+(active ? "" : "_missing"),entries.get(i).getKey());
                if (active) assertArrayEquals(new Object[]{boosts[i]},entries.get(i).getArgs());
                else assertEquals(0,entries.get(i).getArgs().length);
            }
        }
    }
}
