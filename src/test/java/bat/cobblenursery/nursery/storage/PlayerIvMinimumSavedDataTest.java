package bat.cobblenursery.nursery.storage;

import bat.cobblenursery.nursery.model.LineageRecord;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerIvMinimumSavedDataTest {
    @Test
    void minimumsSurviveReloadAndStayIsolatedByPlayer() {
        var data = new LineageSavedData();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        data.put(LineageRecord.createDefault(first).withRaisedIv("hp", 24));
        data.put(LineageRecord.createDefault(second).withRaisedIv("speed", 30));

        var reloaded = LineageSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(24, reloaded.find(first).orElseThrow().hpIv());
        assertEquals(0, reloaded.find(first).orElseThrow().speedIv());
        assertEquals(0, reloaded.find(second).orElseThrow().hpIv());
        assertEquals(30, reloaded.find(second).orElseThrow().speedIv());
    }

    @Test
    void oldSpeciesRecordsAreNotImported() {
        var owner = UUID.randomUUID();
        var bySpecies = new CompoundTag();
        bySpecies.put("cobblemon:togepi", LineageRecord.createDefault(owner).withRaisedIv("hp", 31).save());
        var players = new CompoundTag();
        players.put(owner.toString(), bySpecies);
        var oldSave = new CompoundTag();
        oldSave.put("players", players);

        assertEquals("cobblenursery_breeding_iv_minimums", LineageSavedData.DATA_ID);
        assertTrue(LineageSavedData.load(oldSave, null).find(owner).isEmpty());
    }
    @Test void patioRoutesSurviveReloadAndRespectOwnerLocationAndInstallation() throws Exception {
        var owner = java.util.UUID.randomUUID();
        var second = java.util.UUID.randomUUID();
        var id = java.util.UUID.randomUUID();
        var overworld = new NurserySavedData.EggOutlet("minecraft:overworld", 23L, id);
        var nether = new NurserySavedData.EggOutlet("minecraft:the_nether", 23L, id);
        var replacement = new NurserySavedData.EggOutlet("minecraft:overworld", 23L, java.util.UUID.randomUUID());
        var saved = new NurserySavedData();
        saved.put(bat.cobblenursery.nursery.model.NurseryPlayerState.createDefault(owner).withUnlockedYardSlots(4));
        saved.getOrCreate(second);
        assertEquals(0,saved.eggOutletMask(owner,overworld));
        org.junit.jupiter.api.Assertions.assertFalse(saved.setEggOutlet(second,1,overworld,true));
        assertTrue(saved.setEggOutlet(owner,0,overworld,true));
        assertTrue(saved.setEggOutlet(owner,1,nether,true));
        assertTrue(saved.setEggOutlet(owner,3,overworld,true));
        assertTrue(saved.setEggOutlet(second,0,replacement,true));
        var registries = net.minecraft.core.RegistryAccess.EMPTY;
        var tag = saved.save(new net.minecraft.nbt.CompoundTag(),registries);
        var loader = NurserySavedData.class.getDeclaredMethod("load",net.minecraft.nbt.CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        loader.setAccessible(true);
        var loaded = (NurserySavedData)loader.invoke(null,tag,registries);
        assertEquals(9,loaded.eggOutletMask(owner,overworld));
        assertEquals(2,loaded.eggOutletMask(owner,nether));
        assertEquals(0,loaded.eggOutletMask(owner,replacement));
        assertEquals(1,loaded.eggOutletMask(second,replacement));
        assertEquals(0,loaded.eggOutletMask(second,overworld));
        loaded.removeEggOutlet(owner,overworld);
        assertEquals(0,loaded.eggOutletMask(owner,overworld));
        assertEquals(2,loaded.eggOutletMask(owner,nether));
        tag.remove("yard_egg_outlets");
        var legacy = (NurserySavedData)loader.invoke(null,tag,registries);
        assertEquals(0,legacy.eggOutletMask(owner,overworld));
        assertEquals(0,legacy.eggOutletMask(owner,nether));
    }
}
