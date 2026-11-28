package bat.cobblemondaycare.client.screen;

import bat.cobblemondaycare.network.DaycareParentPayload;
import bat.cobblemondaycare.network.DaycareYardPayload;
import bat.cobblemondaycare.network.OpenDaycareScreenS2CPayload;
import com.cobblemon.mod.common.client.gui.pc.IconButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DaycareWidgetMountTest {
    @Test void hopperAppearsOnlyForAuthorizedEndpointAndUsesServerConfirmedState() throws Exception {
        var screen=new DaycareScreen(payload(0));
        screen.width=854; screen.height=480; screen.init();
        var pos=payload(0).nurseryPos();
        var rect=NurseryPcLayout.hopperExport();
        int original=screen.children().size();
        screen.applyEggExportState(new bat.cobblemondaycare.network.TerminalEggExportS2CPayload(pos.above(),true,1));
        assertEquals(original,screen.children().size());
        screen.applyEggExportState(new bat.cobblemondaycare.network.TerminalEggExportS2CPayload(pos,true,0));
        var button=widgetAt(screen,rect);
        assertTrue(accepts(button,rect));
        assertEquals("gui.cobblemondaycare.pc.hopper_enable",button.getMessage().getString());
        screen.applyEggExportState(new bat.cobblemondaycare.network.TerminalEggExportS2CPayload(pos,true,1));
        assertSame(button,widgetAt(screen,rect));
        assertEquals("gui.cobblemondaycare.pc.hopper_disable",button.getMessage().getString());
        screen.applyEggExportState(new bat.cobblemondaycare.network.TerminalEggExportS2CPayload(pos,false,0));
        assertEquals(original,screen.children().size());
        assertFalse(screen.children().contains(button));
    }

    @Test void vanillaResizeRecentersTheOpenTerminalAndKeepsTheHelpPage() throws Exception {
        var screen = new DaycareScreen(payload(0));
        screen.width = 420;
        screen.height = 270;
        screen.init();
        var frameField = DaycareScreen.class.getDeclaredField("pcFrame");
        frameField.setAccessible(true);

        screen.resize(null, 960, 540);
        var frame = (NurseryPcLayout.Frame) frameField.get(screen);
        assertEquals(480, frame.screenX(NurseryPcLayout.WIDTH / 2.0), 1.0);
        assertEquals(270, frame.screenY((NurseryPcLayout.HEIGHT + EggGroupTab.HEIGHT) / 2.0), 1.0);
        assertEquals(1.0, frame.scale());
        assertEquals(9, screen.children().size());

        ((net.minecraft.client.gui.components.Button) widgetAt(screen, NurseryPcLayout.helpButton())).onPress();
        ((net.minecraft.client.gui.components.Button) widgetAt(screen, NurseryPcLayout.helpNavigation(false))).onPress();
        screen.resize(null, 278, 151);
        frame = (NurseryPcLayout.Frame) frameField.get(screen);
        assertEquals(139, frame.screenX(NurseryPcLayout.WIDTH / 2.0), 1.0);
        assertEquals(75.5, frame.screenY((NurseryPcLayout.HEIGHT + EggGroupTab.HEIGHT) / 2.0), 1.0);
        assertTrue(frame.scale() < 1.0);
        assertEquals(3, screen.children().size());
        assertTrue(accepts(widgetAt(screen, NurseryPcLayout.helpNavigation(true)), NurseryPcLayout.helpNavigation(true)));
        assertTrue(accepts(widgetAt(screen, NurseryPcLayout.helpDone()), NurseryPcLayout.helpDone()));
    }

    @Test void realScreenInitMountsNativeArrowFourTabsAndBothActionTargets() throws Exception {
        for (int[] viewport : new int[][]{{854,480},{278,151}}) {
            var screen = new DaycareScreen(payload(0));
            screen.width = viewport[0];
            screen.height = viewport[1];
            screen.init();

            var children = screen.children();
            assertEquals(9, children.size());
            var renderables = net.minecraft.client.gui.screens.Screen.class.getDeclaredField("renderables");
            renderables.setAccessible(true);
            assertEquals(children, renderables.get(screen), "same real controls must be mounted for mouse input and drawing");
            var arrow = children.stream().filter(IconButton.class::isInstance).map(IconButton.class::cast).findFirst().orElseThrow();
            assertEquals("switch", arrow.getMessage().getString());
            assertNull(arrow.getTooltipKey());
            for (int i=0;i<4;i++) {
                var tab = widgetAt(screen,NurseryPcLayout.tab(i));
                assertTrue(tab instanceof Renderable);
                assertTrue(accepts(tab,NurseryPcLayout.tab(i)));
            }

            var action=widgetAt(screen,NurseryPcLayout.action());
            var claim=widgetAt(screen,NurseryPcLayout.eggs());
            assertEquals("gui.cobblemondaycare.pc.start", action.getMessage().getString());
            assertTrue(accepts(action,NurseryPcLayout.action()));
            assertTrue(accepts(claim,NurseryPcLayout.eggs()));
            screen.rebuildWidgets();
            assertEquals(9, screen.children().size(), "remount must not lose or duplicate controls");
        }
    }

    @Test void helpButtonOpensOneRealModalAndReturnsToTheSameHabitat() throws Exception {
        for (int[] view : new int[][]{{854,480},{278,151}}) {
            var screen=new DaycareScreen(payload(1));
            screen.width=view[0]; screen.height=view[1]; screen.init();
            var help=widgetAt(screen,NurseryPcLayout.helpButton());
            assertEquals("?",help.getMessage().getString());
            assertTrue(accepts(help,NurseryPcLayout.helpButton()));
            ((net.minecraft.client.gui.components.Button) help).onPress(); // native click callback, sans sound manager
            assertEquals(2,screen.children().size());
            var next = widgetAt(screen,new DaycareLayout.Rect(207,172,23,21));
            assertEquals("gui.cobblemondaycare.help.next",next.getMessage().getString());
            assertTrue(accepts(next,NurseryPcLayout.helpNavigation(false)));
            ((net.minecraft.client.gui.components.Button) next).onPress();
            assertEquals(3, screen.children().size());
            var previous = widgetAt(screen,new DaycareLayout.Rect(119,172,23,21));
            assertEquals("gui.cobblemondaycare.help.previous",previous.getMessage().getString());
            assertTrue(accepts(previous,NurseryPcLayout.helpNavigation(true)));
            var navFrame=NurseryPcLayout.fit(view[0],view[1]);
            var nextThird=widgetAt(screen,new DaycareLayout.Rect(207,172,23,21));
            assertTrue(accepts(nextThird,new DaycareLayout.Rect(207,172,23,21)));
            ((net.minecraft.client.gui.components.Button) nextThird).onPress();
            assertEquals(2,screen.children().size());
            assertTrue(screen.mouseClicked(navFrame.screenX(218.5),navFrame.screenY(182.5),0));
            assertEquals(2,screen.children().size());
            ((net.minecraft.client.gui.components.Button)widgetAt(screen,NurseryPcLayout.helpNavigation(true))).onPress();
            previous=widgetAt(screen,NurseryPcLayout.helpNavigation(true));
            ((net.minecraft.client.gui.components.Button) previous).onPress();
            assertEquals("gui.cobblemondaycare.help.next",widgetAt(screen,new DaycareLayout.Rect(207,172,23,21)).getMessage().getString());
            var done=widgetAt(screen,NurseryPcLayout.helpDone());
            assertTrue(accepts(done,NurseryPcLayout.helpDone()));
            var nativeFrame=NurseryPcLayout.fit(view[0],view[1]);
            var action=NurseryPcLayout.action();
            assertTrue(screen.mouseClicked(nativeFrame.screenX(action.x()+4),nativeFrame.screenY(action.centerY()),0));
            assertEquals(2,screen.children().size(),"background click must not mount confirmation or another patio");
            assertTrue(screen.keyPressed(256,0,0));
            assertEquals(8,screen.children().size());
            assertEquals("gui.cobblemondaycare.pc.unlock",widgetAt(screen,action).getMessage().getString());

            ((net.minecraft.client.gui.components.Button) widgetAt(screen,NurseryPcLayout.helpButton())).onPress();
            ((net.minecraft.client.gui.components.Button)widgetAt(screen,NurseryPcLayout.helpNavigation(false))).onPress();
            ((net.minecraft.client.gui.components.Button)widgetAt(screen,NurseryPcLayout.helpNavigation(false))).onPress();
            screen.applyPayload(payload(1));
            assertEquals("gui.cobblemondaycare.help.previous",widgetAt(screen,NurseryPcLayout.helpNavigation(true)).getMessage().getString());
            ((net.minecraft.client.gui.components.Button) widgetAt(screen,NurseryPcLayout.helpDone())).onPress();
            assertEquals(8,screen.children().size());
            assertEquals("gui.cobblemondaycare.pc.unlock",widgetAt(screen,action).getMessage().getString());
        }
    }

    @Test void guidePoseStateSurvivesRepeatedServerRefreshWhilePageIsOpen() throws Exception {
        var screen=new DaycareScreen(payload(1));
        screen.width=854; screen.height=480; screen.init();
        ((net.minecraft.client.gui.components.Button)widgetAt(screen,NurseryPcLayout.helpButton())).onPress();
        // Store a sentinel: a refresh may not inspect, replace, or erase the client-only animation state.
        var field=assertDoesNotThrow(()->DaycareScreen.class.getDeclaredField("helpRenderStates"));
        field.setAccessible(true);
        @SuppressWarnings("unchecked") var poses=(java.util.Map<String,Object>)field.get(screen);
        Object idle=new Object();
        poses.put("help:pair_left",idle);
        for(int i=0;i<3;i++){
            screen.applyPayload(payload(1)); // same entry point as each one-second server refresh
            assertSame(idle,poses.get("help:pair_left"));
            assertEquals(2,screen.children().size());
        }
    }

    @Test void realLockedScreenMountsOnlyEligibleUnlockAction() throws Exception {
        var eligible=new DaycareScreen(payload(1)); eligible.width=854; eligible.height=480; eligible.init();
        assertEquals(8, eligible.children().size());
        var unlock=widgetAt(eligible,NurseryPcLayout.action());
        assertEquals("gui.cobblemondaycare.pc.unlock", unlock.getMessage().getString());
        assertTrue(accepts(unlock,NurseryPcLayout.action()));
        ((net.minecraft.client.gui.components.Button) unlock).onPress();
        var confirmation = widgetAt(eligible,NurseryPcLayout.action());
        assertEquals("gui.cobblemondaycare.pc.confirm",confirmation.getMessage().getString());
        assertNotSame(unlock,confirmation);
        assertTrue(accepts(confirmation,NurseryPcLayout.action()));
        assertEquals(8, eligible.children().size(), "first unlock click only mounts confirmation");
        var locked=new DaycareScreen(payload(2)); locked.width=854; locked.height=480; locked.init();
        assertFalse(accepts(widgetAt(locked,NurseryPcLayout.action()),NurseryPcLayout.action()));
    }

    @Test void activeAndPausedMountTheSameDockAsStopNotStart() throws Exception {
        for (String state : List.of("BREEDING","PAUSED_NO_SPACE")) {
            var prior=payload(0);
            var y=prior.yards().getFirst();
            var active=new DaycareYardPayload(0,true,0,true,state,y.parentA(),y.parentB(),
                    true,"",null,0,"",1,.5f,0,1,4);
            var screen=new DaycareScreen(new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,
                    List.of(),List.of(active,prior.yards().get(1),prior.yards().get(2),prior.yards().get(3)),
                    List.of(),0,4,"",true));
            screen.width=854;screen.height=480;screen.init();
            var stop=widgetAt(screen,NurseryPcLayout.action());
            assertEquals("gui.cobblemondaycare.pc.stop",stop.getMessage().getString());
            assertFalse(accepts(stop,NurseryPcLayout.action()),"no negotiated stop support in headless client");
            assertTrue(accepts(widgetAt(screen,NurseryPcLayout.eggs()),NurseryPcLayout.eggs()));
        }
    }

    @Test void purchaseReplyRoundTripStartsActualScreenFrameButRefreshDoesNot() throws Exception {
        var before = payload(1);
        var yards = new java.util.ArrayList<>(before.yards());
        yards.set(1, new DaycareYardPayload(1,true,64,true,"EMPTY",null,null,false,"",null,0,"",0,0,0,0,4));
        var field = DaycareScreen.class.getDeclaredField("habitatFx");
        field.setAccessible(true);
        var failed = new DaycareScreen(before);
        failed.width=854; failed.height=480; failed.init();
        ((NurseryHabitatFx)field.get(failed)).request(1,net.minecraft.Util.getMillis());
        failed.applyPayload(new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,List.of(),before.yards(),List.of(),1,4,
                "Not enough.",false));
        assertNull(failed.sampleUnlockFrame(1,net.minecraft.Util.getMillis()));
        assertNotEquals("II",widgetAt(failed,NurseryPcLayout.tab(1)).getMessage().getString());
        for (boolean purchase : new boolean[]{false,true}) {
            var screen = new DaycareScreen(before);
            screen.width=854; screen.height=480; screen.init();
            var fx = (NurseryHabitatFx)field.get(screen);
            fx.request(1, net.minecraft.Util.getMillis());
            // Round-trip the same outgoing message policy and wire codec as the production purchase response.
            String message;
            try {
                var policy = bat.cobblemondaycare.network.NurseryNetworking.class
                        .getDeclaredMethod("daycareResponseMessage", String.class, boolean.class, int.class);
                policy.setAccessible(true);
                message = (String)policy.invoke(null,"Unlocked yard 2.",true,purchase ? 1 : -1);
            } catch (NoSuchMethodException oldPolicy) {
                message = ""; // F525's unconditional successful-message suppression.
            }
            var sent = new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,List.of(),yards,List.of(),1,4,message,true);
            var bytes = new net.minecraft.network.RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
            try {
                OpenDaycareScreenS2CPayload.STREAM_CODEC.encode(bytes,sent);
                screen.applyPayload(OpenDaycareScreenS2CPayload.STREAM_CODEC.decode(bytes));
            } finally { bytes.release(); }
            long frameStart = net.minecraft.Util.getMillis();
            var first = screen.sampleUnlockFrame(1,frameStart);
            if (!purchase) assertNull(first);
            else {
                assertNotNull(first,"an actual confirmed wire reply must arm the screen's first unlock frame");
                assertEquals(0,first.landscape());
                screen.rebuildWidgets(); // live applyPayload already remounts when Minecraft is present
                assertEquals("II", widgetAt(screen,NurseryPcLayout.tab(1)).getMessage().getString());
                var reopened = new DaycareScreen(sent);
                reopened.width=854; reopened.height=480; reopened.init();
                assertNull(reopened.sampleUnlockFrame(1,frameStart),
                        "opening an already unlocked screen must not replay the purchase effect");
                var next = screen.sampleUnlockFrame(1,frameStart+180);
                assertTrue(next.landscape() > 0);
                assertNotEquals(0,next.rotation());
                NurseryHabitatFx.Frame last = next;
                long stop = 181;
                for (; stop < 4000; stop++) {
                    var candidate = screen.sampleUnlockFrame(1,frameStart + stop);
                    if (candidate == null) break;
                    last = candidate;
                }
                assertTrue(stop > 900 && stop < 4000);
                var frame = NurseryPcLayout.fit(screen.width,screen.height);
                var tab = NurseryPcLayout.tab(1);
                assertTrue(frame.screenY(tab.centerY()+last.dy()-8) > screen.height,
                        "the full rotated sprite must exit the actual window before removal");
                assertNull(screen.sampleUnlockFrame(1,frameStart+stop));
            }
        }
    }

    @Test void liveInspectorFollowsHoverAndNeverPinsDuringBreedingOrFullPause() throws Exception {
        var prior = payload(0);
        var parent = new DaycareParentPayload(UUID.randomUUID(),"?", "normal","FEMALE",
                "","","","",List.of(),null);
        var party = new bat.cobblemondaycare.network.DaycarePartyPokemonPayload(0,true,UUID.randomUUID(),
                "?", "normal","MALE","",1,0,0,0,0,0,0,0,0,0,0,0,0);
        var child = new bat.cobblemondaycare.network.DaycareEggPreviewPayload("cobblemon:wooper","normal","","","",
                List.of(),0,0,0,0,0,0,0,0,0,0,0,0);
        var resolution = DaycareScreen.class.getDeclaredMethod("inspectorSubject",DaycareYardPayload.class,
                int.class,int.class,java.util.function.Predicate.class);
        resolution.setAccessible(true);
        java.util.function.Predicate<Object> available = candidate -> true, missing = candidate -> false;
        var actorFactory = DaycareScreen.class.getDeclaredMethod("getOrCreateYardActorState",DaycareYardPayload.class,
                boolean.class,DaycareParentPayload.class);
        actorFactory.setAccessible(true);
        var partyBounds = NurseryPcLayout.partySlot(0);
        DaycareScreen screen = new DaycareScreen(prior);
        screen.width=854; screen.height=480; screen.init();
        var frame = NurseryPcLayout.fit(screen.width,screen.height);
        for (String state : List.of("READY_TO_START","BREEDING","PAUSED_NO_SPACE","READY_TO_START")) {
            var yard = new DaycareYardPayload(0,true,0,true,state,parent,null,true,"",child,0,"",0,.5f,0,1,4);
            screen.applyPayload(new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,List.of(party),
                    List.of(yard,prior.yards().get(1),prior.yards().get(2),prior.yards().get(3)),List.of(),0,4,"",true));
            Object partyHover = resolution.invoke(screen,yard,partyBounds.centerX(),partyBounds.centerY(),available);
            assertNotNull(partyHover);
            assertNotSame(child,partyHover);
            var movesHeader = (net.minecraft.network.chat.contents.TranslatableContents)
                    DaycareScreen.inspectorMovesHeader(partyHover instanceof bat.cobblemondaycare.network.DaycareEggPreviewPayload).getContents();
            assertEquals("cobblemon.ui.moves",movesHeader.getKey());
            if (state.equals("READY_TO_START")) {
                assertEquals(partyHover,resolution.invoke(screen,yard,5,5,available));
                continue;
            }
            assertSame(child,resolution.invoke(screen,yard,5,5,available));
            var offspringHeader = (net.minecraft.network.chat.contents.TranslatableContents)
                    DaycareScreen.inspectorMovesHeader(true).getContents();
            assertEquals("gui.cobblemondaycare.pc.egg_moves",offspringHeader.getKey());
            assertSame(child,resolution.invoke(screen,yard,partyBounds.centerX(),partyBounds.centerY(),missing));
            var actor = actorFactory.invoke(screen,yard,false,parent);
            var projected = actor.getClass().getDeclaredField("projected");
            projected.setAccessible(true);
            projected.set(actor,new NurseryGhostProjection.StageBounds(100,70,125,100));
            Object patioHover = resolution.invoke(screen,yard,110,85,available);
            assertNotNull(patioHover);
            assertNotSame(child,patioHover);
            assertNotEquals(partyHover,patioHover);
            assertTrue(screen.mouseClicked(frame.screenX(110),frame.screenY(85),0)); // pickup only
            assertSame(child,resolution.invoke(screen,yard,110,85,available)); // now hidden at source
            assertSame(child,resolution.invoke(screen,yard,5,5,available)); // held model doesn't pin
            assertTrue(screen.keyPressed(256,0,0)); // cancel, no transfer
            assertSame(child,resolution.invoke(screen,yard,5,5,available));
            assertEquals(patioHover,resolution.invoke(screen,yard,110,85,available));
            assertSame(child,resolution.invoke(screen,yard,5,5,available));
        }
    }

    @Test void inheritedNatureUsesExistingNativeInfoSlotAndRejectsOldSeededReplies() throws Exception {
        var a = new DaycareParentPayload(UUID.randomUUID(),"cobblemon:ditto","normal","GENDERLESS",
                "limber","cobblemon:adamant","cobblemon:poke_ball","cobblemon:everstone",List.of(),null);
        var normal = new DaycareParentPayload(UUID.randomUUID(),"cobblemon:wooper","normal","FEMALE",
                "damp","cobblemon:modest","cobblemon:poke_ball","",List.of(),null);
        var secondStone = new DaycareParentPayload(normal.pokemonId(),normal.speciesId(),normal.formId(),normal.gender(),
                normal.abilityId(),normal.natureId(),normal.ballId(),"cobblemon:everstone",List.of(),null);
        var child = new bat.cobblemondaycare.network.DaycareEggPreviewPayload("cobblemon:wooper","normal","","","",
                List.of(),0,0,0,0,0,0,0,0,0,0,0,0);
        for (var b : List.of(normal,secondStone)) {
            var yard = new DaycareYardPayload(0,true,0,true,"BREEDING",a,b,true,"",child,0,
                    "cobblemon:adamant",0,.5f,0,0,4);
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                    net.minecraft.core.RegistryAccess.EMPTY);
            DaycareYardPayload result;
            try { DaycareYardPayload.write(buf,yard); result=DaycareYardPayload.read(buf); } finally { buf.release(); }
            var label=DaycareScreen.offspringNatureLabel(result);
            assertTrue(result.eggPreview().natureId().isBlank());
            if (b == normal) {
                var contents = assertInstanceOf(net.minecraft.network.chat.contents.TranslatableContents.class,label.getContents());
                assertEquals("cobblemon.nature.adamant", contents.getKey());
            } else assertEquals("\u2014",label.getString());
        }
    }

    @Test void offspringMinimumIvRowsIgnoreLegacyFloorsAndSeededResults() {
        var profile = new bat.cobblemondaycare.network.DaycareParentPreview("",5,"",List.of(),List.of(),
                List.of(27,15,9,19,23,25), List.of(0,0,0,0,0,0));
        var a = new DaycareParentPayload(UUID.randomUUID(),"cobblemon:ditto","normal","GENDERLESS",
                "limber","cobblemon:adamant","cobblemon:poke_ball","cobblemon:power_weight",List.of(),profile);
        var b = new DaycareParentPayload(UUID.randomUUID(),"cobblemon:wooper","normal","FEMALE",
                "damp","cobblemon:modest","cobblemon:poke_ball","cobblemon:power_band",List.of(),null);
        for (int seedValue : new int[]{0,31}) {
            var child = new bat.cobblemondaycare.network.DaycareEggPreviewPayload("cobblemon:wooper","normal","","",
                    "",List.of(),0,0,13,0,0,31,seedValue,seedValue,seedValue,seedValue,seedValue,seedValue);
            var yard = new DaycareYardPayload(0,true,0,true,"BREEDING",a,b,true,"",child,0,"",0,.5f,0,0,4);
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                    net.minecraft.core.RegistryAccess.EMPTY);
            DaycareYardPayload decoded;
            try { DaycareYardPayload.write(buf,yard); decoded=DaycareYardPayload.read(buf); } finally { buf.release(); }
            var minimums = DaycareScreen.offspringMinimums(decoded);
            assertEquals(List.of(27,0,0,0,0,0),minimums);
            assertEquals("gui.cobblemondaycare.pc.minimum_ivs",
                    assertInstanceOf(net.minecraft.network.chat.contents.TranslatableContents.class,
                            DaycareScreen.offspringStatLabel("ui.stats.ivs",minimums).getContents()).getKey());
            assertEquals("cobblemon.ui.stats.evs",
                    assertInstanceOf(net.minecraft.network.chat.contents.TranslatableContents.class,
                            DaycareScreen.offspringStatLabel("ui.stats.evs",null).getContents()).getKey());
            assertEquals("27",DaycareScreen.offspringStatLabel("stat0",minimums).getString());
            assertEquals("\u2014",DaycareScreen.offspringStatLabel("stat1",minimums).getString());
            assertEquals("\u2014",DaycareScreen.offspringStatLabel("stat0",null).getString());
        }
    }

    @Test void stockArrowSkipsOffspringEvAndOrdinaryEvRemainsAvailable() throws Exception {
        var base = payload(0);
        var child = new bat.cobblemondaycare.network.DaycareEggPreviewPayload("cobblemon:wooper","normal",
                "","","",List.of(),0,0,0,0,0,0,0,0,0,0,0,0);
        var y=base.yards().getFirst();
        var screen=new DaycareScreen(base);
        screen.width=854; screen.height=480; screen.init();
        var arrow=screen.children().stream().filter(IconButton.class::isInstance).map(IconButton.class::cast)
                .findFirst().orElseThrow();
        assertNull(arrow.getTooltipKey());
        var page=DaycareScreen.class.getDeclaredField("inspector");
        page.setAccessible(true);
        arrow.onPress();
        arrow.onPress();
        assertEquals("EV",page.get(screen).toString());
        var producing=new DaycareYardPayload(0,true,0,true,"BREEDING",y.parentA(),y.parentB(),true,
                "",child,0,"",0,.5f,0,0,4);
        screen.applyPayload(new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,List.of(),List.of(producing,
                base.yards().get(1),base.yards().get(2),base.yards().get(3)),List.of(),0,4,"",true));
        assertEquals("IV", screen.offspringInspectorPage().toString()); // immediately, preserving ordinary EV
        assertEquals("EV",page.get(screen).toString());
        arrow.onPress();
        assertEquals("INFO",screen.offspringInspectorPage().toString());
        arrow.onPress();
        assertEquals("IV",screen.offspringInspectorPage().toString());
        arrow.onPress();
        assertEquals("INFO",screen.offspringInspectorPage().toString());
    }

    private static AbstractWidget widgetAt(DaycareScreen screen, DaycareLayout.Rect r) {
        return screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(w -> w.getX()==r.x() && w.getY()==r.y()).findFirst().orElseThrow();
    }

    private static boolean accepts(AbstractWidget widget, DaycareLayout.Rect r) throws Exception {
        var m=widget.getClass().getDeclaredMethod("clicked",double.class,double.class);
        m.setAccessible(true);
        boolean inside=(boolean)m.invoke(widget,(double)r.centerX(),(double)r.centerY());
        assertFalse((boolean)m.invoke(widget,(double)(r.x()+r.width()),(double)r.centerY()));
        return inside;
    }

    private static OpenDaycareScreenS2CPayload payload(int tab) {
        var parent=new DaycareParentPayload(UUID.randomUUID(),"cobblemon:wooper","normal","FEMALE",
                "damp","hardy","cobblemon:poke_ball","",List.of(),null);
        var ready=new DaycareYardPayload(0,true,0,true,"READY_TO_START",parent,parent,true,"",null,0,"",0,0,0,1,4);
        var locked1=new DaycareYardPayload(1,false,64,true,"LOCKED",null,null,false,"",null,0,"",0,0,0,0,4);
        var locked2=new DaycareYardPayload(2,false,128,true,"LOCKED",null,null,false,"",null,0,"",0,0,0,0,4);
        var locked3=new DaycareYardPayload(3,false,192,false,"LOCKED",null,null,false,"",null,0,"",0,0,0,0,4);
        return new OpenDaycareScreenS2CPayload(BlockPos.ZERO,false,List.of(),List.of(ready,locked1,locked2,locked3),
                List.of(),tab,4,"",true);
    }
    @Test void hopperControlFollowsSelectedPatioAndDoesNotMountOnLockedPatio() {
        var base = payload(0);
        var screen = new DaycareScreen(base);
        screen.width=854; screen.height=480; screen.init();
        var yards = new java.util.ArrayList<>(base.yards());
        yards.set(1, new DaycareYardPayload(1,true,0,true,"EMPTY",null,null,false,"",null,0,"",0,0,0,0,4));
        var rect = NurseryPcLayout.hopperExport();
        screen.applyEggExportState(new bat.cobblemondaycare.network.TerminalEggExportS2CPayload(base.nurseryPos(),true,2));
        assertEquals("gui.cobblemondaycare.pc.hopper_enable",widgetAt(screen,rect).getMessage().getString());
        screen.applyPayload(new OpenDaycareScreenS2CPayload(base.nurseryPos(),false,List.of(),yards,List.of(),1,4,"",true));
        assertEquals("gui.cobblemondaycare.pc.hopper_disable",widgetAt(screen,rect).getMessage().getString());
        screen.applyPayload(new OpenDaycareScreenS2CPayload(base.nurseryPos(),false,List.of(),yards,List.of(),2,4,"",true));
        assertThrows(java.util.NoSuchElementException.class,()->widgetAt(screen,rect));
        screen.applyPayload(base);
        assertEquals("gui.cobblemondaycare.pc.hopper_enable",widgetAt(screen,rect).getMessage().getString());
    }
}
