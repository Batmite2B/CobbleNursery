package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssignPartyPokemonToDaycareYardC2SPayload(BlockPos nurseryPos, int yardIndex, int partySlot) implements CustomPacketPayload {
    public static final Type<AssignPartyPokemonToDaycareYardC2SPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "assign_daycare_pokemon_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignPartyPokemonToDaycareYardC2SPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.nurseryPos());
                buf.writeVarInt(payload.yardIndex());
                buf.writeVarInt(payload.partySlot());
            },
            buf -> new AssignPartyPokemonToDaycareYardC2SPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
