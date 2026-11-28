package qa;

import bat.cobblemondaycare.item.NurseryEggItem;
import bat.cobblemondaycare.nest.IncubationNestBlockEntity;
import bat.cobblemondaycare.nursery.model.*;
import bat.cobblemondaycare.nursery.storage.NurserySavedData;
import bat.cobblemondaycare.registry.NurseryRegistry;
import bat.cobblemondaycare.terminal.NurseryTerminalBlockEntity;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;

public class HopperGameTest implements FabricGameTest {
    private static final GeneratedEggData EGG = new GeneratedEggData("cobblemon:pikachu","","hardy","static","cobblemon:poke_ball",List.of(),
        Map.of("hp",31,"attack",4,"defence",8,"special_attack",5,"special_defence",11,"speed",13),false);
    private static final BlockPos TERMINAL = new BlockPos(2,3,2), OUTPUT = TERMINAL.below(), CHEST = OUTPUT.below();
    private static final BlockPos NEST = new BlockPos(4,1,4), INTAKE = NEST.above();
    private static int eggs(NurserySavedData data, UUID id) { return data.find(id).orElseThrow().findYard(0).orElseThrow().eggCount(); }
    private static NurseryPlayerState tray(UUID id, int count) {
        var yard=NurseryYardRecord.empty(0);
        for(int i=0;i<count;i++) yard=yard.addEgg(new NurseryEggTrayEntry(UUID.randomUUID(),0,EGG,i));
        return NurseryPlayerState.createDefault(id).upsertYard(yard);
    }

    @GameTest(template=FabricGameTest.EMPTY_STRUCTURE, timeoutTicks=40)
    public void configuredUnlockCurrency(GameTestHelper h) {
        var previous=bat.cobblemondaycare.config.ServerNurserySettings.current();
        var settings=new bat.cobblemondaycare.config.ServerNurserySettings(
                new bat.cobblemondaycare.config.HabitatUnlockCosts(3,0,2), "minecraft:emerald",
                previous.breeding(), previous.incubation(), previous.bonuses(), previous.automation());
        var player=h.makeMockServerPlayerInLevel();
        var nursery=bat.cobblemondaycare.nursery.service.NurseryService.getInstance();
        var coins=net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse("cobblemon:relic_coin")).orElseThrow();
        try {
            bat.cobblemondaycare.config.ServerNurserySettings.install(settings);
            player.getInventory().setItem(0,new ItemStack(coins,10));
            player.getInventory().setItem(1,new ItemStack(Items.EMERALD,2));
            h.assertFalse(nursery.unlockYard(player,2).success(),"free unlock skipped locked habitat");
            h.assertFalse(nursery.unlockYard(player,1).success(),"relic coins counted as emeralds");
            h.assertTrue(player.getInventory().getItem(1).getCount()==2,"failed unlock consumed emeralds");
            player.getInventory().setItem(1,new ItemStack(Items.EMERALD,5));
            h.assertTrue(nursery.unlockYard(player,1).success(),"configured currency did not unlock habitat");
            h.assertTrue(player.getInventory().getItem(0).getCount()==10,"charged relic coins instead of emeralds");
            h.assertTrue(player.getInventory().getItem(1).getCount()==2,"charged the wrong number of emeralds");
            h.assertFalse(nursery.unlockYard(player,1).success(),"unlocked habitat could be purchased twice");
            h.assertTrue(nursery.unlockYard(player,2).success(),"free next habitat was not unlocked");
            h.assertTrue(player.getInventory().getItem(1).getCount()==2,"free unlock consumed emeralds");
        } finally {
            bat.cobblemondaycare.config.ServerNurserySettings.install(previous);
        }
        h.succeed();
    }

    @GameTest(template=FabricGameTest.EMPTY_STRUCTURE, timeoutTicks=180)
    public void realHoppers(GameTestHelper h) {
        var world=h.getLevel();
        h.setBlock(CHEST, Blocks.CHEST);
        h.setBlock(CHEST.north(), Blocks.REDSTONE_BLOCK); // keep adjacent hopper empty space check separate below
        h.setBlock(OUTPUT,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        h.setBlock(TERMINAL,NurseryRegistry.NURSERY_TERMINAL);
        var chest=(ChestBlockEntity)h.getBlockEntity(CHEST);
        var output=(HopperBlockEntity)h.getBlockEntity(OUTPUT);
        for(int i=0;i<output.getContainerSize();i++) output.setItem(i,new ItemStack(Items.STONE,64));
        for(int i=0;i<chest.getContainerSize();i++) chest.setItem(i,new ItemStack(Items.STONE,64));
        var terminal=(NurseryTerminalBlockEntity)h.getBlockEntity(TERMINAL);

        var owner=h.makeMockServerPlayerInLevel();
        owner.setPos(h.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(TERMINAL)));
        UUID other=UUID.randomUUID();
        var data=NurserySavedData.get(world.getServer());
        data.put(tray(owner.getUUID(), 5));
        data.put(tray(other, 1));
        h.assertFalse(terminal.setEggExport(owner,0,true),"unplaced terminal must be private to nobody");
        NurseryRegistry.NURSERY_TERMINAL.setPlacedBy(world,h.absolutePos(TERMINAL),h.getBlockState(TERMINAL),owner,ItemStack.EMPTY);
        h.assertTrue(terminal.setEggExport(owner,0,true),"placer could not enable export");
        h.assertFalse(((BlockEntity)terminal).getUpdateTag(world.registryAccess()).hasUUID("egg_export_placer"),"owner leaked to client");

        h.setBlock(NEST.below(),Blocks.STONE);
        h.setBlock(NEST,NurseryRegistry.INCUBATION_NEST);
        h.setBlock(INTAKE,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        var intake=(HopperBlockEntity)h.getBlockEntity(INTAKE);
        var nest=(IncubationNestBlockEntity)h.getBlockEntity(NEST);
        var storage=ItemStorage.SIDED.find(world,h.absolutePos(NEST),Direction.UP);
        var stamped=NurseryRegistry.createNurseryEgg(EGG);
        NurseryEggItem.bindAutomationOwner(stamped,owner.getUUID());
        try(var tx=Transaction.openOuter()) {
            h.assertTrue(storage.insert(ItemVariant.of(stamped),1,tx)==1,"aborted insertion was not staged");
        }
        h.assertTrue(!nest.occupied(),"aborted transaction left an egg");

        h.runAfterDelay(25,()->{
            h.assertTrue(eggs(data,owner.getUUID())==5,"full output consumed pending eggs");
            for(int i=0;i<output.getContainerSize();i++) output.setItem(i,ItemStack.EMPTY);
            for(int i=0;i<chest.getContainerSize();i++) chest.setItem(i,ItemStack.EMPTY);
        });

        h.runAfterDelay(85,()->{
            h.assertTrue(eggs(data,owner.getUUID())==0 && eggs(data,other)==1,"export mixed player trays");
            int n=0;
            for(int i=0;i<chest.getContainerSize();i++) {
                var egg=chest.getItem(i);
                if(!egg.is(NurseryRegistry.NURSERY_EGG_ITEM)) continue;
                h.assertTrue(owner.getUUID().equals(NurseryEggItem.automationOwner(egg)),"owner tag not preserved");
                intake.setItem(n++,egg.copy());
            }
            h.assertTrue(n==5,"cofre did not receive five eggs: "+n);
        });

        h.runAfterDelay(140,()->{
            h.assertTrue(nest.seatMask()==15,"nest did not fill four seats");
            h.assertTrue(!nest.mayAccess(other) && nest.mayAccess(owner.getUUID()),"nest owner is not protected");
            int left=0;
            for(int i=0;i<intake.getContainerSize();i++) if(!intake.getItem(i).isEmpty()) left++;
            h.assertTrue(left==1,"full nest did not leave overflow in hopper");
            for(int i=0;i<4;i++) h.assertTrue(nest.seat(i).readyAt()>System.currentTimeMillis(),"egg has no incubation deadline");
            h.succeed();
        });
    }
    private static BlockPos terminalAt(int n) { return new BlockPos(n,3,1); }
    private static NurseryTerminalBlockEntity build(GameTestHelper h, int i, net.minecraft.server.level.ServerPlayer owner) {
        var p=terminalAt(i);
        h.setBlock(p.below(2), Blocks.CHEST);
        h.setBlock(p.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        h.setBlock(p,NurseryRegistry.NURSERY_TERMINAL);
        if(owner != null) NurseryRegistry.NURSERY_TERMINAL.setPlacedBy(h.getLevel(),h.absolutePos(p),h.getBlockState(p),owner,ItemStack.EMPTY);
        return (NurseryTerminalBlockEntity)h.getBlockEntity(p);
    }
    private static List<Integer> markers(GameTestHelper h, int i, UUID owner) {
        var out=new ArrayList<Integer>();
        for(var p : List.of(terminalAt(i).below(),terminalAt(i).below(2))) {
            var container=(net.minecraft.world.Container)h.getBlockEntity(p);
            for(int j=0;j<container.getContainerSize();j++) {
                var stack=container.getItem(j);
                if(!stack.is(NurseryRegistry.NURSERY_EGG_ITEM)) continue;
                h.assertTrue(owner.equals(NurseryEggItem.automationOwner(stack)), "wrong owner stamped");
                out.add(NurseryEggItem.readEggData(stack).orElseThrow().ivs().get("hp"));
            }
        }
        Collections.sort(out);
        return out;
    }
    private static NurseryYardRecord marked(NurseryYardRecord record, int start, int end) {
        for(int k=start;k<=end;k++) {
            var egg=new GeneratedEggData("cobblemon:pikachu","","hardy","static","cobblemon:poke_ball",List.of(),
                Map.of("hp",k,"attack",4,"defence",8,"special_attack",5,"special_defence",11,"speed",13),false);
            record=record.addEgg(new NurseryEggTrayEntry(UUID.randomUUID(),record.yardIndex(),egg,k));
        }
        return record;
    }
    private static void expect(GameTestHelper h, int i, UUID owner, Integer... expected) {
        h.assertTrue(markers(h,i,owner).equals(List.of(expected)),
            "terminal "+i+" expected "+List.of(expected)+" but got "+markers(h,i,owner));
    }

    @GameTest(template=FabricGameTest.EMPTY_STRUCTURE, timeoutTicks=210)
    public void fourTerminalRouting(GameTestHelper h) {
        var world=h.getLevel();
        var owner=h.makeMockServerPlayerInLevel();
        var visitor=h.makeMockServerPlayerInLevel();
        var at=h.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(terminalAt(2)));
        owner.setPos(at); visitor.setPos(at);
        var data=NurserySavedData.get(world.getServer());
        var state=NurseryPlayerState.createDefault(owner.getUUID()).withUnlockedYardSlots(4);
        for(int yard=0;yard<4;yard++) state=state.upsertYard(marked(state.findYard(yard).orElseThrow(),yard*4+1,yard*4+4));
        data.put(state);
        data.put(tray(visitor.getUUID(),1));
        var terms=new ArrayList<NurseryTerminalBlockEntity>();
        for(int i=1;i<=4;i++) terms.add(build(h,i,i==4?null:owner));
        var t1=terms.get(0); var t2=terms.get(1); var t3=terms.get(2); var t4=terms.get(3);

        h.assertFalse(t1.setEggExport(visitor,0,true),"visitor enabled another owner's outlet");
        h.assertFalse(t4.setEggExport(owner,3,true),"public terminal enabled");
        t4.recordPlacer(owner.getUUID());
        h.assertFalse(t4.setEggExport(owner,4,true),"invalid patio activated");
        h.assertFalse(t4.setEggExport(owner,-1,true),"negative patio activated");
        for(var t : terms) h.assertTrue(t.eggExportMask(owner)==0,"outlets must start off");

        var fourth=(HopperBlockEntity)h.getBlockEntity(terminalAt(4).below());
        for(int j=0;j<fourth.getContainerSize();j++) fourth.setItem(j,new ItemStack(Items.STONE,64));
        var fourthChest=(ChestBlockEntity)h.getBlockEntity(terminalAt(4).below(2));
        for(int j=0;j<fourthChest.getContainerSize();j++) fourthChest.setItem(j,new ItemStack(Items.STONE,64));
        h.runAfterDelay(20,()->{
            for(int i=1;i<=4;i++) expect(h,i,owner.getUUID());
            for(int yard=0;yard<4;yard++) h.assertTrue(terms.get(yard).setEggExport(owner,yard,true),"could not enable patio "+yard);
            h.assertTrue(t1.eggExportMask(owner)==1 && t2.eggExportMask(owner)==2 &&
                t3.eggExportMask(owner)==4 && t4.eggExportMask(owner)==8,"routes or masks not isolated");
        });

        h.runAfterDelay(65,()->{
            expect(h,1,owner.getUUID(),1,2,3,4);
            expect(h,2,owner.getUUID(),5,6,7,8);
            expect(h,3,owner.getUUID(),9,10,11,12);
            expect(h,4,owner.getUUID());
            var current=data.find(owner.getUUID()).orElseThrow();
            h.assertTrue(current.findYard(3).orElseThrow().eggCount()==4,"full hopper lost eggs");
            h.assertTrue(eggs(data,visitor.getUUID())==1,"another player lost eggs");

            // Reassign to the third terminal while the old one is blocked; stale OFF cannot clear the new route.
            h.assertTrue(t3.setEggExport(owner,3,true),"reassignment failed");
            t4.setEggExport(owner,3,false);
            h.assertTrue(t3.eggExportMask(owner)==12 && t4.eggExportMask(owner)==0,"old outlet still owns reassigned route");
            for(int j=0;j<fourth.getContainerSize();j++) fourth.setItem(j,ItemStack.EMPTY);
            for(int j=0;j<fourthChest.getContainerSize();j++) fourthChest.setItem(j,ItemStack.EMPTY);

            h.assertTrue(t1.setEggExport(owner,0,false),"OFF failed");
            h.setBlock(terminalAt(2),Blocks.AIR);
            h.setBlock(terminalAt(2),NurseryRegistry.NURSERY_TERMINAL);
            var replacement=(NurseryTerminalBlockEntity)h.getBlockEntity(terminalAt(2));
            replacement.recordPlacer(owner.getUUID());
            h.assertTrue(replacement.eggExportMask(owner)==0,"new block inherited old route");
            data.put(current.upsertYard(marked(current.findYard(0).orElseThrow(),17,18))
                .upsertYard(marked(current.findYard(1).orElseThrow(),19,20))
                .upsertYard(marked(current.findYard(2).orElseThrow(),21,22)));
        });

        h.runAfterDelay(130,()->{
            expect(h,1,owner.getUUID(),1,2,3,4);
            expect(h,2,owner.getUUID(),5,6,7,8);
            expect(h,3,owner.getUUID(),9,10,11,12,13,14,15,16,21,22);
            expect(h,4,owner.getUUID());
            var current=data.find(owner.getUUID()).orElseThrow();
            h.assertTrue(current.findYard(0).orElseThrow().eggCount()==2,"disabled export removed eggs");
            h.assertTrue(current.findYard(1).orElseThrow().eggCount()==2,"replacement exported without opting in");
            h.assertTrue(current.findYard(2).orElseThrow().eggCount()==0 && current.findYard(3).orElseThrow().eggCount()==0,
                "exported egg also exists in tray");
            var replacement=(NurseryTerminalBlockEntity)h.getBlockEntity(terminalAt(2));
            h.assertTrue(replacement.setEggExport(owner,0,true) && replacement.setEggExport(owner,1,true),"cannot route multiple patios to a terminal");

            // An authorized manual claim consumes patio I; the hopper must not deliver those same eggs later.
            var claim=bat.cobblemondaycare.nursery.service.NurseryService.getInstance().claimEggTray(owner,0);
            h.assertTrue(claim.claimedEggCount()==2,"manual claim did not get the two pending eggs: "+claim);
        });
        h.runAfterDelay(175,()->{
            expect(h,1,owner.getUUID(),1,2,3,4);
            expect(h,2,owner.getUUID(),5,6,7,8,19,20);
            expect(h,3,owner.getUUID(),9,10,11,12,13,14,15,16,21,22);
            expect(h,4,owner.getUUID());
            h.assertTrue(data.find(owner.getUUID()).orElseThrow().yards().stream().mapToInt(NurseryYardRecord::eggCount).sum()==0,"leftover or duplicated tray eggs");
            h.assertTrue(eggs(data,visitor.getUUID())==1,"visitor tray changed");
            h.succeed();
        });
    }
    private static int warmth(IncubationNestBlockEntity nest) {
        try {
            var m=IncubationNestBlockEntity.class.getDeclaredMethod("warmthPercent");
            m.setAccessible(true);
            return (int)m.invoke(nest);
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }

    @GameTest(template=FabricGameTest.EMPTY_STRUCTURE, timeoutTicks=360)
    public void incubationEnvironment(GameTestHelper h) {
        var world=h.getLevel();
        var nestPos=new BlockPos(2,2,2);
        for(int y=1;y<=5;y++) for(int x=0;x<=4;x++) for(int z=0;z<=4;z++) {
            boolean shell=x==0 || x==4 || z==0 || z==4 || y==1 || y==5;
            h.setBlock(new BlockPos(x,y,z),shell?Blocks.GLASS:Blocks.AIR);
        }
        // Solid support for the nest; a glass shell still counts as closed.
        h.setBlock(nestPos.below(),Blocks.STONE);
        h.setBlock(nestPos,NurseryRegistry.INCUBATION_NEST);
        var nest=(IncubationNestBlockEntity)h.getBlockEntity(nestPos);
        var owner=h.makeMockServerPlayerInLevel();
        var friend=h.makeMockServerPlayerInLevel();
        owner.setPos(h.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(nestPos)));
        friend.setPos(h.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(nestPos.north())));
        h.assertTrue(bat.cobblemondaycare.nest.NestWarmth.enclosed(world,h.absolutePos(nestPos)),"glass room not closed");
        var doorway = new BlockPos(0,2,2);
        var door = Blocks.OAK_DOOR.defaultBlockState();
        var half=net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
        var open=net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN;
        world.setBlock(h.absolutePos(doorway),door,Block.UPDATE_CLIENTS);
        world.setBlock(h.absolutePos(doorway.above()),door.setValue(half,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),Block.UPDATE_CLIENTS);
        h.assertTrue(bat.cobblemondaycare.nest.NestWarmth.enclosed(world,h.absolutePos(nestPos)),"closed door did not count");
        for(int dy=0;dy<=12;dy++) world.setBlock(h.absolutePos(doorway.west().above(dy)),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        world.setBlock(h.absolutePos(doorway),door.setValue(open,true),Block.UPDATE_CLIENTS);
        world.setBlock(h.absolutePos(doorway.above()),door.setValue(open,true).setValue(half,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),Block.UPDATE_CLIENTS);
        h.assertTrue(world.getBlockState(h.absolutePos(doorway)).getValue(open),"fixture could not open door");
        h.assertFalse(bat.cobblemondaycare.nest.NestWarmth.enclosed(world,h.absolutePos(nestPos)),"open door gave shelter");
        h.setBlock(doorway,Blocks.GLASS);
        h.setBlock(doorway.above(),Blocks.GLASS);
        var egg=NurseryRegistry.createNurseryEgg(EGG);
        var hatchId=UUID.randomUUID();
        long deadline=System.currentTimeMillis()+100_000L;
        h.assertTrue(nest.insert(0,owner.getUUID(),new IncubationNestBlockEntity.Seat(hatchId,deadline,egg)),"could not seat egg");

        var lamp=new BlockPos(2,4,2);
        var props=com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("magmar ability=flamebody");
        var pokemon=props.create();
        h.assertTrue("flamebody".equals(pokemon.getAbility().getName()),"Magmar was not given its active Flame Body");
        com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(friend).add(pokemon);
        var center=h.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(nestPos));
        var caregiver=pokemon.sendOut(world,center.add(0,16,0),null,p->kotlin.Unit.INSTANCE);
        h.assertTrue(caregiver!=null && friend.getUUID().equals(caregiver.getOwnerUUID()),"test Pokemon is not a friend's owned Pokemon");
        caregiver.setNoAi(true);
        caregiver.setNoGravity(true);
        h.setBlock(lamp,Blocks.GLOWSTONE);
        var secondPokemon=com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("chansey").create();
        com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(owner).add(secondPokemon);
        var secondCaregiver=secondPokemon.sendOut(world,center.add(0,12,0),null,p->kotlin.Unit.INSTANCE);
        h.assertTrue(secondCaregiver!=null && owner.getUUID().equals(secondCaregiver.getOwnerUUID()),"second owner missing");
        secondCaregiver.setNoAi(true);
        secondCaregiver.setNoGravity(true);
        var afterAll=new java.util.concurrent.atomic.AtomicLong();
        var afterJustLight=new java.util.concurrent.atomic.AtomicLong();

        h.runAfterDelay(20,()->{
            h.assertTrue(warmth(nest)==0,"nests must not scan or activate before inspection");
            h.useBlock(nestPos,owner);
        });
        h.runAfterDelay(55,()->{
            h.assertTrue(warmth(nest)==60,"all three bonuses did not activate: "+warmth(nest));
            var seat=nest.seat(0);
            h.assertTrue(seat.readyAt()<deadline && seat.hatchId().equals(hatchId),"no acceleration or hatch identity changed");
            afterAll.set(seat.readyAt());
            secondPokemon.recall();
        });
        h.runAfterDelay(88,()->{
            h.useBlock(nestPos,owner);
            h.assertTrue(warmth(nest)==60,"exactly 16 blocks should count with only one caregiver: "+warmth(nest));
            caregiver.setPos(center.add(0,16.05,0));
        });
        h.runAfterDelay(121,()->{
            h.useBlock(nestPos,owner);
            h.assertTrue(warmth(nest)==30,"outside 16 blocks should not count: "+warmth(nest));
            h.setBlock(doorway,Blocks.AIR);
            h.assertFalse(bat.cobblemondaycare.nest.NestWarmth.enclosed(world,h.absolutePos(nestPos)),"roof alone or wall hole counted");
        });
        h.runAfterDelay(237,()->{
            h.useBlock(nestPos,owner);
            h.assertTrue(warmth(nest)==15,"removing caregiver and opening room did not leave just light: "+warmth(nest));
            h.assertTrue(nest.seat(0).readyAt()<afterAll.get(),"remaining bonus stopped working");
            h.setBlock(lamp,Blocks.AIR);
        });
        h.runAfterDelay(269,()->{
            h.useBlock(nestPos,owner);
            h.assertTrue(warmth(nest)==0,"dark room gave light bonus: "+warmth(nest));
            afterJustLight.set(nest.seat(0).readyAt());
        });
        h.runAfterDelay(301,()->{
            h.assertTrue(nest.seat(0).readyAt()==afterJustLight.get(),"baseline removed or granted extra progress");
            h.assertTrue(nest.seat(0).hatchId().equals(hatchId),"hatch identity did not survive");
            var tag=((BlockEntity)nest).saveWithFullMetadata(world.registryAccess());
            var loaded=(IncubationNestBlockEntity)BlockEntity.loadStatic(h.absolutePos(nestPos),h.getBlockState(nestPos),tag,world.registryAccess());
            h.assertTrue(loaded!=null && loaded.seat(0).hatchId().equals(hatchId),"reload lost egg");
            h.assertTrue(loaded.seat(0).readyAt()==afterJustLight.get(),"reload lost earned acceleration");
            h.assertTrue(loaded.serverRemaining(0,afterJustLight.get()-2_000)==2_000
                && loaded.serverRemaining(0,afterJustLight.get()-1_000)==1_000,"unloaded wall-clock progress was not 1:1");
            h.assertTrue(warmth(loaded)==0,"reload mistakenly credits a prior bonus offline");
            pokemon.recall();
            h.succeed();
        });
    }
}
