package bat.cobblenursery.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public record DaycarePartyPokemonPayload(
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
    public static void write(RegistryFriendlyByteBuf buf, DaycarePartyPokemonPayload payload) {
        buf.writeVarInt(payload.slotIndex());
        buf.writeBoolean(payload.occupied());
        buf.writeBoolean(payload.pokemonId() != null);
        if (payload.pokemonId() != null) {
            buf.writeUUID(payload.pokemonId());
        }
        buf.writeUtf(payload.speciesId());
        buf.writeUtf(payload.formId());
        buf.writeUtf(payload.gender());
        buf.writeUtf(payload.heldItemId());
        buf.writeVarInt(payload.level());
        buf.writeVarInt(payload.hpIv());
        buf.writeVarInt(payload.attackIv());
        buf.writeVarInt(payload.defenceIv());
        buf.writeVarInt(payload.specialAttackIv());
        buf.writeVarInt(payload.specialDefenceIv());
        buf.writeVarInt(payload.speedIv());
        buf.writeVarInt(payload.hpEv());
        buf.writeVarInt(payload.attackEv());
        buf.writeVarInt(payload.defenceEv());
        buf.writeVarInt(payload.specialAttackEv());
        buf.writeVarInt(payload.specialDefenceEv());
        buf.writeVarInt(payload.speedEv());
    }

    public static DaycarePartyPokemonPayload read(RegistryFriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        boolean occupied = buf.readBoolean();
        UUID pokemonId = buf.readBoolean() ? buf.readUUID() : null;
        return new DaycarePartyPokemonPayload(
            slotIndex,
            occupied,
            pokemonId,
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }
}
