package bat.cobblenursery.nursery.service;

import bat.cobblenursery.terminal.TerminalDisplayState;

import bat.cobblenursery.breeding.service.BreedingCompatibilityService;
import bat.cobblenursery.breeding.service.BreedingResultFactory;
import bat.cobblenursery.breeding.service.LineageService;
import bat.cobblenursery.CobbleNursery;
import bat.cobblenursery.config.HabitatUnlockCosts;
import bat.cobblenursery.config.NurseryConfig;
import bat.cobblenursery.economy.BatcgEconomyService;
import bat.cobblenursery.economy.EconomyService;
import bat.cobblenursery.nursery.model.BreedingCompatibilityResult;
import bat.cobblenursery.nursery.model.BreedingItemSnapshot;
import bat.cobblenursery.nursery.model.GeneratedEggData;
import bat.cobblenursery.nursery.model.NurseryEggTrayEntry;
import bat.cobblenursery.nursery.model.NurseryPlayerState;
import bat.cobblenursery.nursery.model.NurseryProcessRecord;
import bat.cobblenursery.nursery.model.NurseryProcessStatus;
import bat.cobblenursery.nursery.model.NurseryYardRecord;
import bat.cobblenursery.nursery.model.NurseryTransfer;
import bat.cobblenursery.nursery.model.ParentSnapshot;
import bat.cobblenursery.nursery.storage.LineageSavedData;
import bat.cobblenursery.nursery.storage.NurserySavedData;
import bat.cobblenursery.registry.NurseryRegistry;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NurseryService {
    private static final NurseryService INSTANCE = new NurseryService();
    private volatile HabitatUnlockCosts habitatUnlockCosts = HabitatUnlockCosts.DEFAULT;

    private final BreedingCompatibilityService compatibilityService = new BreedingCompatibilityService();
    private final BreedingResultFactory breedingResultFactory = new BreedingResultFactory();
    private final LineageService lineageService = new LineageService();
    private final EconomyService economyService = new BatcgEconomyService();

    private NurseryService() {
    }

    public static NurseryService getInstance() {
        return INSTANCE;
    }

    public NurseryPlayerState getState(ServerPlayer player) {
        return loadStateOrDefault(player);
    }

    public int getStartCost() {
        return NurseryConfig.DEFAULT.startCostPokedollars();
    }

    public boolean canAffordStart(ServerPlayer player) {
        return economyService.canAfford(player, getStartCost());
    }

    public Optional<PreviewResult> preview(ServerPlayer player, int partySlotA, int partySlotB) {
        if (partySlotA == partySlotB) {
            return Optional.empty();
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon pokemonA = getPokemonAt(partyStore, partySlotA);
        Pokemon pokemonB = getPokemonAt(partyStore, partySlotB);
        if (pokemonA == null || pokemonB == null) {
            return Optional.empty();
        }

        var registries = player.server.registryAccess();
        ParentSnapshot parentA = ParentSnapshot.fromPokemon(registries, pokemonA);
        ParentSnapshot parentB = ParentSnapshot.fromPokemon(registries, pokemonB);
        BreedingItemSnapshot itemA = BreedingItemSnapshot.fromStack(pokemonA.heldItem());
        BreedingItemSnapshot itemB = BreedingItemSnapshot.fromStack(pokemonB.heldItem());
        BreedingCompatibilityResult compatibility = compatibilityService.evaluate(parentA, parentB);
        long seed = computeSeed(player, partySlotA, partySlotB);

        GeneratedEggData generatedEgg = compatibility.compatible()
            ? generateEggDataForPlayer(player, parentA, parentB, itemA, itemB, seed, false)
            : null;

        return Optional.of(new PreviewResult(parentA, parentB, itemA, itemB, compatibility, generatedEgg, seed));
    }

    public StartResult startProcess(ServerPlayer player, int partySlotA, int partySlotB) {
        if (partySlotA == partySlotB) {
            return new StartResult(false, "Choose two different party slots.", null);
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = savedData.getOrCreate(player.getUUID());
        int availableProcessSlot = state.firstAvailableProcessSlot();
        if (availableProcessSlot < 0) {
            return new StartResult(false, "You have no free nursery slots.", null);
        }

        PreviewResult preview = preview(player, partySlotA, partySlotB).orElse(null);
        if (preview == null) {
            return new StartResult(false, "Invalid party slots for nursery start.", null);
        }
        if (!preview.compatibility().compatible()) {
            return new StartResult(false, "Breeding failed: " + preview.compatibility().reason(), null);
        }
        if (!canAffordStart(player)) {
            return new StartResult(false, "You do not have enough batcg:pokedollar.", null);
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon pokemonA = getPokemonAt(partyStore, partySlotA);
        Pokemon pokemonB = getPokemonAt(partyStore, partySlotB);
        if (pokemonA == null || pokemonB == null) {
            return new StartResult(false, "One of the selected parents is no longer in the party.", null);
        }

        if (!economyService.withdraw(player, getStartCost())) {
            return new StartResult(false, "Could not withdraw the nursery fee.", null);
        }

        boolean removedA = partyStore.remove(pokemonA);
        boolean removedB = false;
        if (removedA) {
            removedB = partyStore.remove(pokemonB);
        }

        if (!removedA || !removedB) {
            if (removedA) {
                partyStore.add(pokemonA);
            }
            if (removedB) {
                partyStore.add(pokemonB);
            }
            economyService.deposit(player, getStartCost());
            return new StartResult(false, "Could not remove the selected parents from the party.", null);
        }

        long now = System.currentTimeMillis();
        long durationMs = NurseryConfig.DEFAULT.baseDurationMinutes() * 60_000L;
        NurseryProcessRecord process = new NurseryProcessRecord(
            UUID.randomUUID(),
            availableProcessSlot,
            NurseryProcessStatus.BREEDING,
            preview.parentA(),
            preview.parentB(),
            preview.itemA(),
            preview.itemB(),
            now,
            now + durationMs,
            preview.seed(),
            null
        );

        savedData.put(state.upsertProcess(process));
        return new StartResult(true, "Nursery process started.", process);
    }

    public NurseryProcessRecord resolveReadyProcess(ServerPlayer player, NurseryProcessRecord record) {
        if (record.generatedEgg() != null && record.status() == NurseryProcessStatus.READY) {
            return record.withGeneratedEgg(record.generatedEgg());
        }

        GeneratedEggData eggData = generateEggDataForPlayer(
            player,
            record.parentA(),
            record.parentB(),
            record.itemA(),
            record.itemB(),
            record.breedingSeed(),
            true
        );
        return record.withGeneratedEgg(eggData);
    }

    public ClaimResult claimProcess(ServerPlayer player, int processSlot) {
        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = savedData.getOrCreate(player.getUUID());
        NurseryProcessRecord record = state.findProcessBySlot(processSlot).orElse(null);
        if (record == null) {
            return new ClaimResult(false, "No nursery process was found in that slot.", null, ItemStack.EMPTY);
        }

        NurseryProcessRecord resolvedRecord = record;
        long now = System.currentTimeMillis();
        if (record.status() == NurseryProcessStatus.BREEDING && record.isReady(now)) {
            resolvedRecord = resolveReadyProcess(player, record);
            savedData.put(state.upsertProcess(resolvedRecord));
            state = savedData.getOrCreate(player.getUUID());
        }

        if (resolvedRecord.status() != NurseryProcessStatus.READY) {
            long remainingMs = Math.max(0L, resolvedRecord.readyAtEpochMs() - now);
            return new ClaimResult(
                false,
                "This nursery process is not ready yet. Remaining: " + ((remainingMs + 59_999L) / 60_000L) + " minute(s).",
                resolvedRecord,
                ItemStack.EMPTY
            );
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon restoredA = restorePokemon(player, resolvedRecord.parentA());
        Pokemon restoredB = restorePokemon(player, resolvedRecord.parentB());
        boolean addedA = partyStore.add(restoredA);
        boolean addedB = false;
        if (addedA) {
            addedB = partyStore.add(restoredB);
        }

        if (!addedA || !addedB) {
            if (addedA) {
                partyStore.remove(restoredA);
            }
            return new ClaimResult(
                false,
                "Could not return the parents to your party/PC, so the egg was not claimed.",
                resolvedRecord,
                ItemStack.EMPTY
            );
        }

        ItemStack eggStack = NurseryRegistry.createNurseryEgg(resolvedRecord.generatedEgg());
        if (!player.getInventory().add(eggStack) && !eggStack.isEmpty()) {
            player.drop(eggStack, false);
        }
        player.getInventory().setChanged();

        NurseryProcessRecord claimedRecord = resolvedRecord.withStatus(NurseryProcessStatus.CLAIMED);
        savedData.put(state.upsertProcess(claimedRecord));
        return new ClaimResult(true, "Nursery egg claimed from slot " + (processSlot + 1) + " and parents returned.", claimedRecord, eggStack.copy());
    }

    public ActionResult cancelProcess(ServerPlayer player, int processSlot) {
        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = savedData.getOrCreate(player.getUUID());
        NurseryProcessRecord record = state.findProcessBySlot(processSlot).orElse(null);
        if (record == null) {
            return new ActionResult(false, "No nursery process was found in that slot.");
        }
        if (record.status() == NurseryProcessStatus.CLAIMED || record.status() == NurseryProcessStatus.CANCELLED) {
            return new ActionResult(false, "That nursery slot is already closed.");
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon restoredA = restorePokemon(player, record.parentA());
        Pokemon restoredB = restorePokemon(player, record.parentB());

        boolean addedA = partyStore.add(restoredA);
        boolean addedB = false;
        if (addedA) {
            addedB = partyStore.add(restoredB);
        }

        if (!addedA || !addedB) {
            if (addedA) {
                partyStore.remove(restoredA);
            }
            return new ActionResult(false, "Could not return the parents to your party/PC.");
        }

        savedData.put(state.upsertProcess(record.withStatus(NurseryProcessStatus.CANCELLED)));
        return new ActionResult(true, "Nursery process cancelled and parents returned.");
    }

    public ActionResult forceReadyProcess(ServerPlayer player, int processSlot) {
        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = savedData.getOrCreate(player.getUUID());
        NurseryProcessRecord record = state.findProcessBySlot(processSlot).orElse(null);
        if (record == null) {
            return new ActionResult(false, "No nursery process was found in that slot.");
        }
        if (record.status() == NurseryProcessStatus.CLAIMED || record.status() == NurseryProcessStatus.CANCELLED) {
            return new ActionResult(false, "That nursery slot is already closed.");
        }
        if (record.status() == NurseryProcessStatus.READY) {
            return new ActionResult(true, "Nursery process is already ready.");
        }

        savedData.put(state.upsertProcess(resolveReadyProcess(player, record)));
        return new ActionResult(true, "Nursery process forced to READY for testing.");
    }

    public List<ProcessSlotView> getProcessSlots(ServerPlayer player) {
        NurseryPlayerState state = getState(player);
        long now = System.currentTimeMillis();

        return java.util.stream.IntStream.range(0, state.unlockedProcessSlots())
            .mapToObj(slotIndex -> {
                NurseryProcessRecord record = state.findProcessBySlot(slotIndex).orElse(null);
                if (record == null) {
                    return new ProcessSlotView(slotIndex, NurseryProcessStatus.EMPTY, "", 0L);
                }

                NurseryProcessStatus effectiveStatus = record.status() == NurseryProcessStatus.BREEDING && record.isReady(now)
                    ? NurseryProcessStatus.READY
                    : record.status();
                long remainingMs = effectiveStatus == NurseryProcessStatus.BREEDING
                    ? Math.max(0L, record.readyAtEpochMs() - now)
                    : 0L;
                String parentSummary = shortSpecies(record.parentA().speciesId()) + " + " + shortSpecies(record.parentB().speciesId());
                return new ProcessSlotView(slotIndex, effectiveStatus, parentSummary, remainingMs);
            })
            .sorted(Comparator.comparingInt(ProcessSlotView::slotIndex))
            .toList();
    }

    public List<PartySlotView> getPartySlots(ServerPlayer player) {
        PlayerPartyStore partyStore = getPartyStore(player);
        return java.util.stream.IntStream.range(0, 6)
            .mapToObj(slotIndex -> {
                Pokemon pokemon = partyStore.get(slotIndex);
                if (pokemon == null) {
                    return new PartySlotView(slotIndex, false, "", "", "", 0);
                }

                String heldItemId = pokemon.heldItem().isEmpty() || pokemon.heldItem().getItemHolder().unwrapKey().isEmpty()
                    ? ""
                    : pokemon.heldItem().getItemHolder().unwrapKey().orElseThrow().location().toString();

                return new PartySlotView(
                    slotIndex,
                    true,
                    pokemon.getSpecies().getResourceIdentifier().toString(),
                    pokemon.getForm().getName(),
                    heldItemId,
                    pokemon.getLevel()
                );
            })
            .toList();
    }

    /** Private and read-only: do not open/advance a nursery cycle merely by inspecting its display. */
    public TerminalDisplayState getTerminalDisplayState(ServerPlayer player) {
        NurserySavedData data = NurserySavedData.find(player.server);
        NurseryPlayerState state = data == null ? null : data.find(player.getUUID()).orElse(null);
        if (state == null) {
            return TerminalDisplayState.builder(1).build();
        }
        var result = TerminalDisplayState.builder(state.unlockedYardSlots());
        long now = System.currentTimeMillis();
        for (NurseryYardRecord yard : state.yards()) {
            boolean full = yard.eggCount() >= NurseryConfig.DEFAULT.yardEggTrayCapacity();
            // A completed cycle already blocked by capacity is paused even if the menu hasn't refreshed it.
            boolean paused = full && (yard.pausedForEggSpace() || (yard.active() && yard.isCycleReady(now)));
            result.yard(yard.yardIndex(), yard.active(), paused);
        }
        return result.build();
    }

    public DaycareStateView getDaycareState(ServerPlayer player) {
        NurseryPlayerState state = refreshYards(player);
        List<DaycarePartySlotView> partySlots = getDaycarePartySlots(player);
        List<DaycareYardView> yards = state.yards().stream()
            .sorted(Comparator.comparingInt(NurseryYardRecord::yardIndex))
            .map(yard -> buildYardView(player, state, yard))
            .toList();
        List<EggTrayEntryView> eggTray = state.selectedYardEggs().stream()
            .map(entry -> new EggTrayEntryView(
                entry.entryId(),
                entry.sourceYardIndex(),
                entry.eggData().childSpeciesId(),
                entry.eggData().childFormId(),
                entry.eggData().inheritedBallId(),
                entry.createdAtEpochMs()
            ))
            .toList();

        return new DaycareStateView(
            partySlots,
            yards,
            eggTray,
            state.selectedYardIndex(),
            NurseryConfig.DEFAULT.yardEggTrayCapacity()
        );
    }

    public ActionResult selectYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard selection.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState nextState = savedData.getOrCreate(player.getUUID()).withSelectedYard(yardIndex);
        savedData.put(nextState);
        return new ActionResult(true, "Selected yard " + (yardIndex + 1) + ".");
    }

    public ActionResult assignPartyPokemonToYard(ServerPlayer player, int yardIndex, int partySlot) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = refreshYards(player);
        NurseryYardRecord yard = state.findYard(yardIndex).orElse(NurseryYardRecord.empty(yardIndex));
        if (!isYardUnlocked(state, yardIndex)) {
            return new ActionResult(false, "This yard is still locked.");
        }
        if (yard.parentA() != null && yard.parentB() != null) {
            return new ActionResult(false, "That yard already has a full pair assigned.");
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon pokemon = getPokemonAt(partyStore, partySlot);
        if (pokemon == null) {
            return new ActionResult(false, "There is no Pokemon in that party slot.");
        }

        ParentSnapshot snapshot = ParentSnapshot.fromPokemon(player.server.registryAccess(), pokemon);
        BreedingItemSnapshot itemSnapshot = BreedingItemSnapshot.fromStack(pokemon.heldItem());
        if (!partyStore.remove(pokemon)) {
            return new ActionResult(false, "Could not move that Pokemon into the yard.");
        }

        NurseryYardRecord nextYard = yard.withAssignedParent(snapshot, itemSnapshot);
        savedData.put(state.upsertYard(nextYard).withSelectedYard(yardIndex));
        return new ActionResult(true, "Assigned " + shortSpecies(snapshot.speciesId()) + " to yard " + (yardIndex + 1) + ".");
    }

    public ActionResult takeYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = refreshYards(player);
        NurseryYardRecord yard = state.findYard(yardIndex).orElse(NurseryYardRecord.empty(yardIndex));
        if (!yard.hasAnyParent()) {
            return new ActionResult(false, "That yard is already empty.");
        }

        PlayerPartyStore partyStore = getPartyStore(player);
        Pokemon restoredA = yard.parentA() == null ? null : restorePokemon(player, yard.parentA());
        Pokemon restoredB = yard.parentB() == null ? null : restorePokemon(player, yard.parentB());
        boolean addedA = restoredA == null || partyStore.add(restoredA);
        boolean addedB = restoredB == null || partyStore.add(restoredB);
        if (!addedA || !addedB) {
            if (addedA && restoredA != null) {
                partyStore.remove(restoredA);
            }
            if (addedB && restoredB != null) {
                partyStore.remove(restoredB);
            }
            return new ActionResult(false, "Could not return the yard pair to your party/PC.");
        }

        savedData.put(state.upsertYard(yard.clearParents()));
        return new ActionResult(true, "Returned the pair from yard " + (yardIndex + 1) + ".");
    }

    public ActionResult moveNurseryPokemon(ServerPlayer player, NurseryTransfer move) {
        if (!move.hasValidIndices()) return new ActionResult(false, "Invalid move destination.");
        NurserySavedData saved = getOrCreateSavedData(player.server);
        NurseryPlayerState state = refreshYards(player);
        if (state.selectedYardIndex() != move.yardIndex() || !isYardUnlocked(state, move.yardIndex())) {
            return new ActionResult(false, "Select an unlocked habitat first.");
        }

        NurseryYardRecord yard = state.findYard(move.yardIndex()).orElse(NurseryYardRecord.empty(move.yardIndex()));
        ParentSnapshot parent = move.parentB() ? yard.parentB() : yard.parentA();
        PlayerPartyStore party = getPartyStore(player);
        Pokemon partyPokemon = party.get(move.partySlot());

        if (move.toHabitat()) {
            if (!move.matches(partyPokemon == null ? null : partyPokemon.getUuid(), parent != null)) {
                return new ActionResult(false, "The Pokemon or target space changed. Try again.");
            }
            ParentSnapshot snapshot = ParentSnapshot.fromPokemon(player.server.registryAccess(), partyPokemon);
            BreedingItemSnapshot item = BreedingItemSnapshot.fromStack(partyPokemon.heldItem());
            NurseryPlayerState next = state.upsertYard(yard.withParentAt(move.parentB(), snapshot, item));
            var transfer = new NurseryPartyMutation(player, party, move.partySlot(), partyPokemon).apply(false);
            if (transfer != PartyMutationGuard.Result.COMMITTED) return rejectedTransfer(transfer, move.expectedPokemonId());
            saved.put(next);
        } else {
            if (!move.matches(parent == null ? null : parent.originalPokemonId(), partyPokemon != null)) {
                return new ActionResult(false, "The Pokemon or party slot changed. Try again.");
            }
            if (partyContains(party, parent.originalPokemonId())) {
                return new ActionResult(false, "That Pokemon is already in your party.");
            }
            Pokemon restored = restorePokemon(player, parent);
            if (!parent.originalPokemonId().equals(restored.getUuid())) {
                return new ActionResult(false, "Could not restore that Pokemon.");
            }
            NurseryPlayerState next = state.upsertYard(yard.withParentAt(move.parentB(), null, BreedingItemSnapshot.EMPTY));
            var transfer = new NurseryPartyMutation(player, party, move.partySlot(), restored).apply(true);
            if (transfer != PartyMutationGuard.Result.COMMITTED) return rejectedTransfer(transfer, move.expectedPokemonId());
            saved.put(next);
        }
        return new ActionResult(true, "");
    }

    private static ActionResult rejectedTransfer(PartyMutationGuard.Result result, UUID id) {
        if (result == PartyMutationGuard.Result.INCONSISTENT) {
            CobbleNursery.LOGGER.error("Nursery did not commit transfer for Pokemon {}: party storage remained inconsistent after recovery.", id);
            return new ActionResult(false, "Party storage did not confirm the move. The habitat was not changed.");
        }
        return new ActionResult(false, "Could not move that Pokemon. The habitat was not changed.");
    }

    private static boolean partyContains(PlayerPartyStore party, UUID id) {
        if (party.get(id) != null) return true;
        for (int slot = 0; slot < 6; slot++) {
            Pokemon candidate = party.get(slot);
            if (candidate != null && id.equals(candidate.getUuid())) return true;
        }
        return false;
    }

    public boolean canAffordYardUnlock(ServerPlayer player, int yardIndex) {
        return isValidYardIndex(yardIndex) && economyService.canAfford(player, getYardUnlockCost(yardIndex));
    }

    public ActionResult stopYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) return new ActionResult(false, "Invalid yard.");
        NurserySavedData saved = getOrCreateSavedData(player.server);
        NurseryPlayerState state = saved.getOrCreate(player.getUUID());
        if (state.selectedYardIndex() != yardIndex || !isYardUnlocked(state, yardIndex)) {
            return new ActionResult(false, "Select the unlocked habitat before stopping breeding.");
        }
        var yard = state.findYard(yardIndex).orElse(null);
        if (yard == null || (!yard.active() && !yard.pausedForEggSpace())) {
            return new ActionResult(false, "That habitat is not breeding.");
        }
        // Stop before any timer refresh; an uncollected tray and both parents stay intact.
        saved.put(state.upsertYard(yard.stopCycle()));
        return new ActionResult(true, "");
    }

    public ActionResult startYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = refreshYards(player);
        NurseryYardRecord yard = state.findYard(yardIndex).orElse(NurseryYardRecord.empty(yardIndex));

        if (!isYardUnlocked(state, yardIndex)) {
            return new ActionResult(false, "Unlock that habitat before starting.");
        }

        if (!yard.hasPair()) {
            return new ActionResult(false, "That yard needs two parents before it can start.");
        }

        BreedingCompatibilityResult compatibility = compatibilityService.evaluate(yard.parentA(), yard.parentB());
        if (!compatibility.compatible()) {
            return new ActionResult(false, "This pair cannot breed: " + compatibility.reason());
        }

        if (yard.active()) {
            return new ActionResult(false, "That yard is already breeding.");
        }
        if (yard.eggCount() >= NurseryConfig.DEFAULT.yardEggTrayCapacity()) {
            return new ActionResult(false, "That yard is full. Claim its eggs first.");
        }

        long now = System.currentTimeMillis();
        NurseryYardRecord nextYard = yard.startCycle(now, now + cycleDurationMs(), computeYardSeed(player, yardIndex, yard.completedCycles()));
        savedData.put(state.upsertYard(nextYard).withSelectedYard(yardIndex));
        return new ActionResult(true, "Started breeding in yard " + (yardIndex + 1) + ".");
    }


    public ActionResult unlockYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = refreshYards(player);

        if (isYardUnlocked(state, yardIndex)) {
            return new ActionResult(false, "That yard is already unlocked.");
        }

        // Evita saltarse patios.
        if (yardIndex > state.unlockedYardSlots()) {
            return new ActionResult(false, "Unlock the previous yard first.");
        }

        int unlockCost = getYardUnlockCost(yardIndex);
        if (!economyService.canAfford(player, unlockCost)) {
            return new ActionResult(false, "You need " + unlockCost + " pokedollars to unlock this yard.");
        }

        if (!economyService.withdraw(player, unlockCost)) {
            return new ActionResult(false, "Could not pay the yard unlock fee.");
        }

        NurseryPlayerState nextState = state
                .withUnlockedYardSlots(yardIndex + 1)
                .withSelectedYard(yardIndex);

        savedData.put(nextState);
        return new ActionResult(true, "Unlocked yard " + (yardIndex + 1) + ".");
    }



    /** Called on the server thread by a bound terminal, including when the player is offline. */
    public boolean exportEggToHopper(MinecraftServer server, UUID owner, NurserySavedData.EggOutlet outlet,
                                     java.util.function.Predicate<ItemStack> deliver) {
        var saved = NurserySavedData.find(server);
        if (saved == null) return false;
        int yardMask = saved.eggOutletMask(owner, outlet);
        if (yardMask == 0) return false;
        var before = saved.find(owner).orElse(null);
        if (before == null) return false;
        var ready = refreshYards(server, owner, before);
        if (ready != before) saved.put(ready);
        return HopperEggOutput.transferOne(ready, yardMask, entry -> {
            var egg = NurseryRegistry.createNurseryEgg(entry.eggData());
            if (egg.isEmpty() || egg.getCount() != 1 || egg.getMaxStackSize() != 1) return false;
            bat.cobblenursery.item.NurseryEggItem.bindAutomationOwner(egg, owner);
            return deliver.test(egg);
        }, saved::put);
    }

    public ClaimEggsResult claimEggTray(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ClaimEggsResult(false, "Invalid yard.", 0);
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState before = savedData.getOrCreate(player.getUUID());
        if (before.selectedYardIndex() != yardIndex || !isYardUnlocked(before, yardIndex)) {
            return new ClaimEggsResult(false, "That yard is no longer selected or unlocked.", 0);
        }

        NurseryPlayerState state = refreshYards(player);
        NurseryYardRecord yard = state.findYard(yardIndex).orElse(NurseryYardRecord.empty(yardIndex));
        if (yard.eggs().isEmpty()) return new ClaimEggsResult(false, "There are no eggs waiting in that yard.", 0);

        var inventory = player.getInventory();
        var batch = EggClaimBatch.take(yard.eggs(), entry -> {
            int slot = inventory.getFreeSlot();
            if (slot < 0 || !inventory.getItem(slot).isEmpty()) return false;
            ItemStack eggStack = NurseryRegistry.createNurseryEgg(entry.eggData());
            if (eggStack.isEmpty() || eggStack.getCount() != 1 || eggStack.getMaxStackSize() != 1) return false;
            // Explicit empty main inventory slot; Inventory.add can consume a creative player's overflow.
            inventory.setItem(slot, eggStack);
            return inventory.getItem(slot) == eggStack;
        }, remaining -> savedData.put(state.upsertYard(yard.withEggs(remaining))));
        int claimedCount = batch.claimed();
        if (claimedCount == 0) return new ClaimEggsResult(false, "Make room in your inventory for an egg.", 0);
        inventory.setChanged();
        return new ClaimEggsResult(true, "Claimed " + claimedCount + " egg(s) from yard " + (yardIndex + 1) + ".", claimedCount);
    }

    public ActionResult forceReadyYard(ServerPlayer player, int yardIndex) {
        if (!isValidYardIndex(yardIndex)) {
            return new ActionResult(false, "Invalid yard.");
        }

        NurserySavedData savedData = getOrCreateSavedData(player.server);
        NurseryPlayerState state = savedData.getOrCreate(player.getUUID());
        NurseryYardRecord yard = state.findYard(yardIndex).orElse(NurseryYardRecord.empty(yardIndex));
        if (!yard.active()) {
            return new ActionResult(false, "That yard is not currently breeding.");
        }

        NurseryYardRecord forced = yard.startCycle(yard.cycleStartedAtEpochMs(), System.currentTimeMillis(), yard.cycleSeed());
        savedData.put(state.upsertYard(forced));
        refreshYards(player);
        return new ActionResult(true, "Forced yard " + (yardIndex + 1) + " to finish its current cycle.");
    }

    private PlayerPartyStore getPartyStore(ServerPlayer player) {
        return Cobblemon.INSTANCE.getStorage().getParty(player);
    }

    private Pokemon getPokemonAt(PlayerPartyStore partyStore, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 6) {
            return null;
        }
        return partyStore.get(slotIndex);
    }

    private Pokemon restorePokemon(ServerPlayer player, ParentSnapshot snapshot) {
        return new Pokemon().loadFromNBT(player.server.registryAccess(), snapshot.pokemonData().copy());
    }

    private long computeSeed(ServerPlayer player, int slotA, int slotB) {
        long mixed = player.getUUID().getMostSignificantBits()
            ^ player.getUUID().getLeastSignificantBits()
            ^ ((long) slotA << 32)
            ^ slotB;
        return mixed ^ System.currentTimeMillis();
    }

    private String shortSpecies(String speciesId) {
        int separator = speciesId.indexOf(':');
        return separator >= 0 ? speciesId.substring(separator + 1) : speciesId;
    }

    private DaycareYardView buildYardView(ServerPlayer player, NurseryPlayerState state, NurseryYardRecord yard) {
        boolean unlocked = isYardUnlocked(state, yard.yardIndex());
        BreedingCompatibilityResult compatibility = yard.hasPair()
            ? compatibilityService.evaluate(yard.parentA(), yard.parentB())
            : BreedingCompatibilityResult.failure(yard.parentA() == null && yard.parentB() == null
                ? "empty"
                : "waiting_for_second_parent");
        GeneratedEggData previewEgg = yard.hasPair() && compatibility.compatible()
            ? generateEggDataForPlayer(player, yard.parentA(), yard.parentB(), yard.itemA(), yard.itemB(), previewSeedForView(yard), false)
            : null;
        long now = System.currentTimeMillis();
        long remainingMs = yard.active() ? Math.max(0L, yard.cycleReadyAtEpochMs() - now) : 0L;
        float progress = yard.cycleProgressAt(now);

        YardVisualState visualState;
        if (!unlocked) {
            visualState = YardVisualState.LOCKED;
        } else if (!yard.hasAnyParent()) {
            visualState = YardVisualState.EMPTY;
        } else if (!yard.hasPair()) {
            visualState = YardVisualState.INCOMPLETE;
        } else if (yard.pausedForEggSpace()) {
            visualState = YardVisualState.PAUSED_NO_SPACE;
        } else if (yard.active()) {
            visualState = YardVisualState.BREEDING;
        } else if (!compatibility.compatible()) {
            visualState = YardVisualState.BLOCKED;
        } else {
            visualState = YardVisualState.READY_TO_START;
        }

        return new DaycareYardView(
            yard.yardIndex(),
            unlocked,
            getYardUnlockCost(yard.yardIndex()),
            visualState,
            yard.parentA(),
            yard.parentB(),
            yard.itemA(),
            yard.itemB(),
            compatibility,
            previewEgg,
            inheritedIvCount(yard),
            inheritedNatureId(yard, previewEgg),
            remainingMs,
            progress,
            yard.completedCycles(),
            yard.eggCount(),
            NurseryConfig.DEFAULT.yardEggTrayCapacity()
        );
    }

    private List<DaycarePartySlotView> getDaycarePartySlots(ServerPlayer player) {
        PlayerPartyStore partyStore = getPartyStore(player);
        return java.util.stream.IntStream.range(0, 6)
            .mapToObj(slotIndex -> {
                Pokemon pokemon = partyStore.get(slotIndex);
                if (pokemon == null) {
                    return new DaycarePartySlotView(slotIndex, false, null, "", "", "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
                }

                String heldItemId = pokemon.heldItem().isEmpty() || pokemon.heldItem().getItemHolder().unwrapKey().isEmpty()
                    ? ""
                    : pokemon.heldItem().getItemHolder().unwrapKey().orElseThrow().location().toString();

                return new DaycarePartySlotView(
                    slotIndex,
                    true,
                    pokemon.getUuid(),
                    pokemon.getSpecies().getResourceIdentifier().toString(),
                    pokemon.getForm().getName(),
                    pokemon.getGender().name(),
                    heldItemId,
                    pokemon.getLevel(),
                    pokemon.getIvs().getOrDefault(Stats.HP),
                    pokemon.getIvs().getOrDefault(Stats.ATTACK),
                    pokemon.getIvs().getOrDefault(Stats.DEFENCE),
                    pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK),
                    pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE),
                    pokemon.getIvs().getOrDefault(Stats.SPEED),
                    pokemon.getEvs().getOrDefault(Stats.HP),
                    pokemon.getEvs().getOrDefault(Stats.ATTACK),
                    pokemon.getEvs().getOrDefault(Stats.DEFENCE),
                    pokemon.getEvs().getOrDefault(Stats.SPECIAL_ATTACK),
                    pokemon.getEvs().getOrDefault(Stats.SPECIAL_DEFENCE),
                    pokemon.getEvs().getOrDefault(Stats.SPEED)
                );
            })
            .toList();
    }

    private NurseryPlayerState refreshYards(ServerPlayer player) {
        NurserySavedData savedData = NurserySavedData.find(player.server);
        NurseryPlayerState state = loadStateOrDefault(player);
        NurseryPlayerState refreshed = refreshYards(player.server, player.getUUID(), state);
        if (savedData != null && refreshed != state) {
            savedData.put(refreshed);
        }
        return refreshed;
    }

    private NurseryPlayerState refreshYards(MinecraftServer server, UUID playerId, NurseryPlayerState baseState) {
        NurseryPlayerState state = baseState;
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (NurseryYardRecord yard : new ArrayList<>(state.yards())) {
            NurseryYardRecord nextYard = yard;
            if (yard.active() && yard.isCycleReady(now)) {
                if (yard.eggCount() >= NurseryConfig.DEFAULT.yardEggTrayCapacity()) {
                    nextYard = yard.pauseForEggSpace();
                } else if (yard.hasPair()) {
                    GeneratedEggData egg = generateEggData(
                        server,
                        playerId,
                        yard.parentA(),
                        yard.parentB(),
                        yard.itemA(),
                        yard.itemB(),
                        yard.cycleSeed(),
                        true
                    );
                    NurseryEggTrayEntry yardEgg = new NurseryEggTrayEntry(UUID.randomUUID(), yard.yardIndex(), egg, now);
                    nextYard = yard.addEgg(yardEgg).markCycleCompleted().startCycle(
                        now,
                        now + cycleDurationMs(),
                        computeYardSeed(playerId, yard.yardIndex(), yard.completedCycles() + 1)
                    );
                } else {
                    nextYard = yard.stopCycle();
                }
                changed = true;
            } else if (yard.pausedForEggSpace() && yard.eggCount() < NurseryConfig.DEFAULT.yardEggTrayCapacity() && yard.hasPair()) {
                nextYard = yard.startCycle(
                    now,
                    now + cycleDurationMs(),
                    computeYardSeed(playerId, yard.yardIndex(), yard.completedCycles())
                );
                changed = true;
            }

            if (nextYard != yard) {
                state = state.upsertYard(nextYard);
            }
        }

        return changed ? state : baseState;
    }

    private long cycleDurationMs() {
        return NurseryConfig.DEFAULT.baseDurationMinutes() * 60_000L;
    }

    private NurserySavedData getOrCreateSavedData(MinecraftServer server) {
        ensurePersistentDataLoaded(server);
        return NurserySavedData.get(server);
    }

    private NurseryPlayerState loadStateOrDefault(ServerPlayer player) {
        NurserySavedData savedData = NurserySavedData.find(player.server);
        if (savedData == null) {
            return NurseryPlayerState.createDefault(player.getUUID());
        }
        return savedData.find(player.getUUID()).orElseGet(() -> NurseryPlayerState.createDefault(player.getUUID()));
    }

    private void ensurePersistentDataLoaded(MinecraftServer server) {
        NurserySavedData.get(server);
        LineageSavedData.get(server);
    }

    private boolean isValidYardIndex(int yardIndex) {
        return yardIndex >= 0 && yardIndex < NurseryPlayerState.MAX_YARDS;
    }

    private boolean isYardUnlocked(NurseryPlayerState state, int yardIndex) {
        return yardIndex < state.unlockedYardSlots();
    }

    public void setHabitatUnlockCosts(HabitatUnlockCosts costs) {
        this.habitatUnlockCosts = costs;
    }

    private int getYardUnlockCost(int yardIndex) {
        return habitatUnlockCosts.forYardIndex(yardIndex);
    }

    private long computeYardSeed(ServerPlayer player, int yardIndex, int cycleIndex) {
        return computeYardSeed(player.getUUID(), yardIndex, cycleIndex);
    }

    private long computeYardSeed(UUID playerId, int yardIndex, int cycleIndex) {
        long mixed = playerId.getMostSignificantBits()
            ^ playerId.getLeastSignificantBits()
            ^ ((long) yardIndex << 32)
            ^ cycleIndex;
        return mixed ^ System.currentTimeMillis();
    }

    private long previewSeedForView(NurseryYardRecord yard) {
        return yard.cycleSeed() != 0L ? yard.cycleSeed() : ((long) yard.yardIndex() << 32) ^ yard.completedCycles();
    }

    private GeneratedEggData generateEggDataForPlayer(ServerPlayer player, ParentSnapshot parentA, ParentSnapshot parentB,
                                                      BreedingItemSnapshot itemA, BreedingItemSnapshot itemB,
                                                      long seed, boolean updateLineage) {
        return generateEggData(player.server, player.getUUID(), parentA, parentB, itemA, itemB, seed, updateLineage);
    }

    private GeneratedEggData generateEggData(MinecraftServer server, UUID playerId,
                                             ParentSnapshot parentA, ParentSnapshot parentB,
                                             BreedingItemSnapshot itemA, BreedingItemSnapshot itemB,
                                             long seed, boolean updateLineage) {
        var rollFloor = lineageService.getLineageRollFloor(server, playerId);
        GeneratedEggData generatedEgg = breedingResultFactory.generate(
            parentA,
            parentB,
            itemA,
            itemB,
            seed,
            rollFloor
        );
        // Parent inheritance can overwrite a rolled stat, so enforce the floor on the final egg.
        var adjustedIvs = new java.util.LinkedHashMap<>(generatedEgg.ivs());
        rollFloor.forEach((stat, minimum) -> adjustedIvs.merge(stat, minimum, Math::max));

        GeneratedEggData finalEgg = new GeneratedEggData(
            generatedEgg.childSpeciesId(),
            generatedEgg.childFormId(),
            generatedEgg.natureId(),
            generatedEgg.inheritedAbilityId(),
            generatedEgg.inheritedBallId(),
            generatedEgg.inheritedMoveIds(),
            adjustedIvs,
            generatedEgg.shiny()
        );

        if (updateLineage) {
            lineageService.updateLineageFromPowerItems(
                server,
                playerId,
                finalEgg.ivs(),
                itemA,
                itemB
            );
        }

        return finalEgg;
    }

    private int inheritedIvCount(NurseryYardRecord yard) {
        return yard.itemA().matchesSuffix("destiny_knot") || yard.itemB().matchesSuffix("destiny_knot") ? 5 : 3;
    }

    private String inheritedNatureId(NurseryYardRecord yard, GeneratedEggData previewEgg) {
        if (previewEgg == null) {
            return "";
        }
        return bat.cobblenursery.breeding.service.NatureInheritanceService.guaranteedNature(
                yard.parentA().natureId(), yard.itemA(), yard.parentB().natureId(), yard.itemB());
    }

    public record PreviewResult(
        ParentSnapshot parentA,
        ParentSnapshot parentB,
        BreedingItemSnapshot itemA,
        BreedingItemSnapshot itemB,
        BreedingCompatibilityResult compatibility,
        GeneratedEggData generatedEgg,
        long seed
    ) {
    }

    public record StartResult(
        boolean success,
        String message,
        NurseryProcessRecord process
    ) {
    }

    public record ClaimResult(
        boolean success,
        String message,
        NurseryProcessRecord process,
        ItemStack eggStack
    ) {
    }

    public record ActionResult(
        boolean success,
        String message
    ) {
    }

    public record ProcessSlotView(
        int slotIndex,
        NurseryProcessStatus status,
        String parentSummary,
        long remainingMs
    ) {
        public long remainingMinutes() {
            return remainingMs <= 0 ? 0 : (remainingMs + 59_999L) / 60_000L;
        }
    }

    public record PartySlotView(
        int slotIndex,
        boolean occupied,
        String speciesId,
        String formId,
        String heldItemId,
        int level
    ) {
    }

    public record ClaimEggsResult(
        boolean success,
        String message,
        int claimedEggCount
    ) {
    }

    public enum YardVisualState {
        LOCKED,
        EMPTY,
        INCOMPLETE,
        READY_TO_START,
        BREEDING,
        PAUSED_NO_SPACE,
        BLOCKED
    }

    public record DaycarePartySlotView(
        int slotIndex,
        boolean occupied,
        UUID pokemonId,
        String speciesId,
        String formId,
        String gender,
        String heldItemId,
        int level,
        int hpIv,
        int attackIv,
        int defenceIv,
        int specialAttackIv,
        int specialDefenceIv,
        int speedIv,
        int hpEv,
        int attackEv,
        int defenceEv,
        int specialAttackEv,
        int specialDefenceEv,
        int speedEv
    ) {
    }

    public record DaycareYardView(
        int yardIndex,
        boolean unlocked,
        int unlockCostPokedollars,
        YardVisualState visualState,
        ParentSnapshot parentA,
        ParentSnapshot parentB,
        BreedingItemSnapshot itemA,
        BreedingItemSnapshot itemB,
        BreedingCompatibilityResult compatibility,
        GeneratedEggData previewEgg,
        int inheritedIvCount,
        String inheritedNatureId,
        long remainingMs,
        float progress,
        int completedCycles,
        int eggCount,
        int eggCapacity
    ) {
    }

    public record EggTrayEntryView(
        UUID entryId,
        int sourceYardIndex,
        String childSpeciesId,
        String childFormId,
        String inheritedBallId,
        long createdAtEpochMs
    ) {
    }

    public record DaycareStateView(
        List<DaycarePartySlotView> partySlots,
        List<DaycareYardView> yards,
        List<EggTrayEntryView> eggTray,
        int selectedYardIndex,
        int eggTrayCapacity
    ) {
    }
}
