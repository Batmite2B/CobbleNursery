package bat.cobblenursery.nursery.service;

import bat.cobblenursery.CobbleNursery;
import com.cobblemon.mod.common.api.storage.StoreCoordinates;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

final class NurseryPartyMutation {
    private final ServerPlayer owner;
    private final PlayerPartyStore party;
    private final int slot;
    private final Pokemon pokemon;
    private final PartyPosition position;
    private boolean storageNotifiedException;

    NurseryPartyMutation(ServerPlayer owner, PlayerPartyStore party, int slot, Pokemon pokemon) {
        this.owner = owner;
        this.party = party;
        this.slot = slot;
        this.pokemon = pokemon;
        this.position = new PartyPosition(slot);
    }

    PartyMutationGuard.Result apply(boolean placing) {
        if (placing && party.get(slot) != null) return PartyMutationGuard.Result.REJECTED;
        var result = PartyMutationGuard.apply(placing, this::observe,
                () -> {
                    if (placing) party.set(slot, pokemon);
                    else party.remove(pokemon);
                },
                () -> reconcile(placing),
                () -> rollback(placing),
                this::report);
        if (storageNotifiedException && result != PartyMutationGuard.Result.INCONSISTENT) {
            try { party.sendTo(owner); }
            catch (RuntimeException e) { report(e); }
        }
        return result;
    }

    private PartyMutationGuard.Observation observe() {
        Pokemon indexed = party.get(pokemon.getUuid());
        var coords = pokemon.getStoreCoordinates().get();
        boolean exact = party.get(slot) == pokemon;
        boolean duplicate = false;
        for (int i = 0; i < 6; i++) {
            Pokemon candidate = party.get(i);
            if (candidate != null && pokemon.getUuid().equals(candidate.getUuid()) && (i != slot || !exact)) {
                duplicate = true;
            }
        }
        return new PartyMutationGuard.Observation(exact, indexed == pokemon, indexed == null,
                matchesCoordinates(coords, slot), coords == null, duplicate);
    }

    private boolean matchesCoordinates(StoreCoordinates<?> coords, int matchingSlot) {
        return coords != null && coords.getStore() == party && coords.getPosition() instanceof PartyPosition p
                && p.getSlot() == matchingSlot;
    }

    private boolean recoverable(PartyMutationGuard.Observation now) {
        var coords = pokemon.getStoreCoordinates().get();
        return !now.sameUuidInOtherSlot() && (now.exactIndex() || now.emptyIndex())
                && (coords == null || matchesCoordinates(coords, slot));
    }

    private void reconcile(boolean placing) {
        var now = observe();
        if (!recoverable(now) || now.exactSlot() != placing) return;
        if (placing && !now.exactCoordinates()) {
            // The setter assigns before emitting; a thrown observer need not have prevented the change.
            try { pokemon.getStoreCoordinates().set(new StoreCoordinates<>(party, position)); }
            catch (RuntimeException e) { report(e); }
            if (!observe().exactCoordinates()) return;
        } else if (!placing && !now.detached()) {
            try { pokemon.getStoreCoordinates().set(null); }
            catch (RuntimeException e) { report(e); }
            if (!observe().detached()) return;
        }
        var checked = observe();
        if (!recoverable(checked) || checked.exactSlot() != placing) return;
        if (!checked.matches(placing)) rebuildIndexIfSafe();
    }

    private void rollback(boolean placing) {
        var now = observe();
        if (!recoverable(now)) return;
        if (placing && now.exactSlot()) {
            if (!now.exactCoordinates()) {
                try { pokemon.getStoreCoordinates().set(new StoreCoordinates<>(party, position)); }
                catch (RuntimeException e) { report(e); }
            }
            if (observe().exactSlot() && observe().exactCoordinates()) {
                try { party.remove(pokemon); }
                catch (RuntimeException e) { report(e); }
            }
        } else if (!placing && party.get(slot) == null) {
            // Put back the same in-memory object in the original slot, without a PC overflow path.
            try { party.set(slot, pokemon); }
            catch (RuntimeException e) { report(e); }
        }
        reconcile(!placing);
    }

    private void rebuildIndexIfSafe() {
        // PartyStore.initialize reindexes current slots. No other party member may be repaired implicitly.
        for (int i = 0; i < 6; i++) {
            Pokemon candidate = party.get(i);
            if (candidate == null || candidate == pokemon) continue;
            if (!matchesCoordinates(candidate.getStoreCoordinates().get(), i) || party.get(candidate.getUuid()) != candidate) {
                return;
            }
        }
        var observers = party.getObserverUUIDs();
        List<java.util.UUID> previous = List.copyOf(observers);
        try {
            party.initialize();
        } finally {
            // PlayerPartyStore's override appends the owner even when re-initializing.
            // Remove only that exact append, keeping the original observers/list unchanged.
            if (party.getObserverUUIDs() == observers && observers.size() == previous.size() + 1
                    && observers.subList(0, previous.size()).equals(previous)
                    && owner.getUUID().equals(observers.getLast())) {
                observers.removeLast();
            }
        }
    }

    private void report(RuntimeException e) {
        storageNotifiedException = true;
        CobbleNursery.LOGGER.warn("Cobblemon raised a nursery party storage/observer exception.", e);
    }
}
