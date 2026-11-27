package bat.cobblenursery.nursery.model;

import java.util.UUID;

public record NurseryTransfer(boolean toHabitat, int yardIndex, int partySlot, boolean parentB, UUID expectedPokemonId) {
    public boolean hasValidIndices() {
        return yardIndex >= 0 && yardIndex < 4 && partySlot >= 0 && partySlot < 6;
    }

    public boolean matches(UUID actualPokemonId, boolean destinationOccupied) {
        return hasValidIndices() && expectedPokemonId != null && expectedPokemonId.equals(actualPokemonId) && !destinationOccupied;
    }
}
