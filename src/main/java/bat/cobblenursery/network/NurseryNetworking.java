package bat.cobblenursery.network;

import bat.cobblenursery.entity.NurseryNurseEntity;
import bat.cobblenursery.registry.NurseryRegistry;
import bat.cobblenursery.terminal.NurseryTerminalBlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.GeneratedEggData;
import bat.cobblenursery.nursery.model.ParentSnapshot;
import bat.cobblenursery.nursery.service.NurseryService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NurseryNetworking {

    // Server-thread only; keyed by player instance, so reconnects and respawns cannot inherit a session.
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private static final Map<ServerPlayer, Map<ParentSnapshot, java.util.Optional<DaycareParentPreview>>> PARENT_PREVIEWS = new WeakHashMap<>();

    private enum Entry { TERMINAL, LEGACY_BLOCK, NURSE, COMMAND }
    private record Session(ResourceKey<Level> dimension, BlockPos pos, Entry entry, UUID nurseId) {}

    private NurseryNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenDaycareScreenS2CPayload.TYPE, OpenDaycareScreenS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DaycareRefreshC2SPayload.TYPE, DaycareRefreshC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleTerminalEggExportC2SPayload.TYPE, ToggleTerminalEggExportC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalEggExportS2CPayload.TYPE, TerminalEggExportS2CPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleTerminalEggExportC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                var player = context.player();
                var terminal = currentTerminal(player, payload.nurseryPos());
                var playerState = bat.cobblenursery.nursery.storage.NurserySavedData.get(player.server).find(player.getUUID()).orElse(null);
                if (terminal != null && playerState != null && playerState.selectedYardIndex() == payload.yardIndex()) {
                    terminal.setEggExport(player, payload.yardIndex(), payload.enabled());
                }
                sendEggExportState(player, payload.nurseryPos(), terminal);
            })
        );
        PayloadTypeRegistry.playC2S().register(SelectDaycareYardC2SPayload.TYPE, SelectDaycareYardC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MoveNurseryPokemonC2SPayload.TYPE, MoveNurseryPokemonC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StartDaycareYardC2SPayload.TYPE, StartDaycareYardC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StopDaycareYardC2SPayload.TYPE, StopDaycareYardC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimDaycareEggsC2SPayload.TYPE, ClaimDaycareEggsC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ForceReadyDaycareYardC2SPayload.TYPE, ForceReadyDaycareYardC2SPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(UnlockDaycareYardC2SPayload.TYPE, UnlockDaycareYardC2SPayload.STREAM_CODEC);



        ServerPlayNetworking.registerGlobalReceiver(UnlockDaycareYardC2SPayload.TYPE, (payload, context) ->
                context.player().server.execute(() -> {
                    if (!canUseNursery(context.player(), payload.nurseryPos())) {
                        return;
                    }

                    NurseryService.ActionResult result = NurseryService.getInstance().unlockYard(context.player(), payload.yardIndex());
                    openScreen(context.player(), payload.nurseryPos(), result.message(), result.success(), payload.yardIndex());
                })
        );



        ServerPlayNetworking.registerGlobalReceiver(DaycareRefreshC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (canUseNursery(context.player(), payload.nurseryPos())) {
                    openScreen(context.player(), payload.nurseryPos(), "", true);
                }
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(SelectDaycareYardC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) {
                    return;
                }
                NurseryService.ActionResult result = NurseryService.getInstance().selectYard(context.player(), payload.yardIndex());
                openScreen(context.player(), payload.nurseryPos(), result.success() ? "" : result.message(), result.success());
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(MoveNurseryPokemonC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) return;
                NurseryService.ActionResult result = NurseryService.getInstance().moveNurseryPokemon(context.player(), payload.move());
                openScreen(context.player(), payload.nurseryPos(), result.message(), result.success());
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(StartDaycareYardC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) {
                    return;
                }
                NurseryService.ActionResult result = NurseryService.getInstance().startYard(context.player(), payload.yardIndex());
                openScreen(context.player(), payload.nurseryPos(), result.message(), result.success());
            })
        );



        ServerPlayNetworking.registerGlobalReceiver(StopDaycareYardC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) return;
                var result = NurseryService.getInstance().stopYard(context.player(), payload.yardIndex());
                openScreen(context.player(), payload.nurseryPos(), result.message(), result.success());
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(ClaimDaycareEggsC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) {
                    return;
                }
                NurseryService.ClaimEggsResult result = NurseryService.getInstance().claimEggTray(context.player(), payload.yardIndex());
                openScreen(context.player(), payload.nurseryPos(), result.message(), result.success());
            })
        );

        ServerPlayNetworking.registerGlobalReceiver(ForceReadyDaycareYardC2SPayload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                if (!canUseNursery(context.player(), payload.nurseryPos())) {
                    return;
                }
                if (!context.player().hasPermissions(2)) {
                    openScreen(context.player(), payload.nurseryPos(), "You do not have permission to force nursery completion.", false);
                    return;
                }
                NurseryService.ActionResult result = NurseryService.getInstance().forceReadyYard(context.player(), payload.yardIndex());
                openScreen(context.player(), payload.nurseryPos(), result.message(), result.success());
            })
        );
    }

    public static void openTerminal(ServerPlayer player, BlockPos pos) {
        beginSession(player, pos, Entry.TERMINAL, null);
    }

    public static void openLegacyBlock(ServerPlayer player, BlockPos pos) {
        beginSession(player, pos, Entry.LEGACY_BLOCK, null);
    }

    public static void openNurse(ServerPlayer player, NurseryNurseEntity nurse, BlockPos pos) {
        if (nurse.level() == player.level()) {
            beginSession(player, pos, Entry.NURSE, nurse.getUUID());
        }
    }

    public static void openCommandScreen(ServerPlayer player) {
        beginSession(player, player.blockPosition(), Entry.COMMAND, null);
    }

    private static void beginSession(ServerPlayer player, BlockPos pos, Entry entry, UUID nurseId) {
        if (!ServerPlayNetworking.canSend(player, OpenDaycareScreenS2CPayload.TYPE)) {
            SESSIONS.remove(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback(
                    "gui.cobblenursery.pc.protocol_mismatch", "Use the same CobbleNursery version on the client and server."), false);
            return;
        }
        SESSIONS.put(player, new Session(player.level().dimension(), pos.immutable(), entry, nurseId));
        if (!canUseNursery(player, pos)) {
            SESSIONS.remove(player);
            return;
        }
        openScreen(player, pos, "", true);
    }

    // Only the successful unlock handler opts into this machine acknowledgement.
    // Successful payload messages are not shown as user feedback by the daycare screen.
    static String daycareResponseMessage(String message, boolean success, int confirmedUnlockYard) {
        if (!success) return message;
        return confirmedUnlockYard > 0 && confirmedUnlockYard < 4
                ? "Unlocked yard " + (confirmedUnlockYard + 1) + "." : "";
    }

    private static void openScreen(ServerPlayer player, BlockPos nurseryPos, String message, boolean success) {
        openScreen(player, nurseryPos, message, success, -1);
    }

    private static void openScreen(ServerPlayer player, BlockPos nurseryPos, String message, boolean success,
                                   int confirmedUnlockYard) {
        NurseryService.DaycareStateView state = NurseryService.getInstance().getDaycareState(player);

        List<DaycarePartyPokemonPayload> partySlots = state.partySlots().stream()
            .map(slot -> new DaycarePartyPokemonPayload(
                slot.slotIndex(),
                slot.occupied(),
                slot.pokemonId(),
                slot.speciesId(),
                slot.formId(),
                slot.gender(),
                slot.heldItemId(),
                slot.level(),
                slot.hpIv(),
                slot.attackIv(),
                slot.defenceIv(),
                slot.specialAttackIv(),
                slot.specialDefenceIv(),
                slot.speedIv(),
                slot.hpEv(),
                slot.attackEv(),
                slot.defenceEv(),
                slot.specialAttackEv(),
                slot.specialDefenceEv(),
                slot.speedEv()
            ))
            .toList();

        List<DaycareYardPayload> yards = state.yards().stream()
            .map(yard -> new DaycareYardPayload(
                yard.yardIndex(),
                yard.unlocked(),
                yard.unlockCost(),
                yard.unlocked() || NurseryService.getInstance().canAffordYardUnlock(player, yard.yardIndex()),
                yard.visualState().name(),
                toParentPayload(player, yard.parentA(), yard.itemA()),
                toParentPayload(player, yard.parentB(), yard.itemB()),
                yard.compatibility().compatible(),
                yard.compatibility().reason(),
                toEggPreview(yard.previewEgg(), yard.parentA(), yard.parentB()),
                yard.inheritedIvCount(),
                yard.inheritedNatureId(),
                yard.remainingMs(),
                yard.progress(),
                yard.completedCycles(),
                yard.eggCount(),
                yard.eggCapacity()
            ))
            .toList();

        List<DaycareEggTrayPayload> eggTray = state.eggTray().stream()
            .map(entry -> new DaycareEggTrayPayload(
                entry.entryId(),
                entry.sourceYardIndex(),
                entry.childSpeciesId(),
                entry.childFormId(),
                entry.inheritedBallId(),
                entry.createdAtEpochMs()
            ))
            .toList();

        ServerPlayNetworking.send(player, new OpenDaycareScreenS2CPayload(
            nurseryPos,
            player.hasPermissions(2),
            partySlots,
            yards,
            eggTray,
            state.selectedYardIndex(),
            state.eggTrayCapacity(),
            bat.cobblenursery.config.ServerNurserySettings.current().unlockItem(),
            bat.cobblenursery.config.ServerNurserySettings.current().breeding().shinyOddsDenominator(),
            daycareResponseMessage(message, success, confirmedUnlockYard),
            success
        ));
        sendEggExportState(player, nurseryPos, currentTerminal(player, nurseryPos));
    }

    private static NurseryTerminalBlockEntity currentTerminal(ServerPlayer player, BlockPos pos) {
        var session = SESSIONS.get(player);
        if (session == null || session.entry() != Entry.TERMINAL || !canUseNursery(player, pos)) return null;
        return player.serverLevel().getBlockEntity(pos) instanceof NurseryTerminalBlockEntity terminal ? terminal : null;
    }

    private static void sendEggExportState(ServerPlayer player, BlockPos pos, NurseryTerminalBlockEntity terminal) {
        if (!ServerPlayNetworking.canSend(player, TerminalEggExportS2CPayload.TYPE)) return;
        boolean available = terminal != null && terminal.canConfigureEggExport(player);
        ServerPlayNetworking.send(player, new TerminalEggExportS2CPayload(pos, available,
                available ? terminal.eggExportMask(player) : 0));
    }

    private static DaycareParentPayload toParentPayload(ServerPlayer player, ParentSnapshot snapshot, BreedingItemSnapshot itemSnapshot) {
        if (snapshot == null) {
            return null;
        }
        return new DaycareParentPayload(
            snapshot.originalPokemonId(),
            snapshot.speciesId(),
            snapshot.formId(),
            snapshot.gender(),
            snapshot.abilityId(),
            snapshot.natureId(),
            snapshot.ballId(),
            itemSnapshot == null ? "" : itemSnapshot.itemId(),
            snapshot.eggGroupIds(),
            parentPreview(player, snapshot)
        );
    }

    // Reconstruct only once per immutable deposited snapshot and owner, never every refresh.
    // Values are a typed presentation projection: no raw Pokémon NBT or owner identifiers travel here.
    private static DaycareParentPreview parentPreview(ServerPlayer player, ParentSnapshot snapshot) {
        return PARENT_PREVIEWS.computeIfAbsent(player, ignored -> new WeakHashMap<>())
            .computeIfAbsent(snapshot, parent -> {
                try {
                    var pokemon = new com.cobblemon.mod.common.pokemon.Pokemon().loadFromNBT(
                            player.server.registryAccess(), parent.pokemonData().copy());
                    if (!parent.originalPokemonId().equals(pokemon.getUuid())) return java.util.Optional.empty();
                    var item = pokemon.getCosmeticItem();
                    String cosmetic = item.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(item.getItem()).toString();
                    return java.util.Optional.of(DaycareParentPreview.fromPokemon(pokemon, cosmetic));
                } catch (RuntimeException invalid) {
                    return java.util.Optional.empty();
                }
            }).orElse(null);
    }

    private static DaycareEggPreviewPayload toEggPreview(GeneratedEggData egg,
                                                         ParentSnapshot parentA, ParentSnapshot parentB) {
        if (egg == null) {
            return null;
        }
        return new DaycareEggPreviewPayload(
            egg.childSpeciesId(),
            egg.childFormId(),
            "", // Keep the established v3 field, but do not send a seeded nature roll.
            egg.inheritedAbilityId(),
            egg.inheritedBallId(),
            parentA == null || parentB == null ? java.util.List.of()
                    : new bat.cobblenursery.breeding.service.EggMoveInheritanceService()
                    .resolveInheritedEggMoves(parentA, parentB, egg.childSpeciesId(), egg.childFormId())
                    .stream().distinct().limit(4).toList(),
            0, 0, 0, 0, 0, 0, // Reserved v3 fields; no per-player minimum.
            egg.ivs().getOrDefault("hp", 0),
            egg.ivs().getOrDefault("attack", 0),
            egg.ivs().getOrDefault("defence", 0),
            egg.ivs().getOrDefault("special_attack", 0),
            egg.ivs().getOrDefault("special_defence", 0),
            egg.ivs().getOrDefault("speed", 0)
        );
    }

    private static boolean canUseNursery(ServerPlayer player, BlockPos nurseryPos) {
        Session session = SESSIONS.get(player);
        boolean active = session != null;
        boolean sameDimension = active && session.dimension().equals(player.level().dimension());
        boolean samePosition = active && session.pos().equals(nurseryPos);
        Vec3 center = Vec3.atCenterOf(nurseryPos);
        double distanceSquared = player.distanceToSqr(center.x, center.y, center.z);
        if (!NurseryAccessPolicy.allows(active, sameDimension, samePosition, distanceSquared, true)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        boolean liveEndpoint = switch (session.entry()) {
            case COMMAND -> true; // Explicit existing /nursery daycare access, never opened by C2S.
            case TERMINAL -> level.hasChunkAt(session.pos())
                && level.getBlockState(session.pos()).is(NurseryRegistry.NURSERY_TERMINAL);
            case LEGACY_BLOCK -> level.hasChunkAt(session.pos())
                && level.getBlockState(session.pos()).is(NurseryRegistry.NURSERY_BLOCK);
            case NURSE -> level.getEntity(session.nurseId()) instanceof NurseryNurseEntity nurse
                && nurse.isAlive() && player.distanceToSqr(nurse) <= 64.0D;
        };
        if (!liveEndpoint) {
            SESSIONS.remove(player);
        }
        return NurseryAccessPolicy.allows(active, sameDimension, samePosition, distanceSquared, liveEndpoint);
    }
}
