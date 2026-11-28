package bat.cobblemondaycare.network;

import bat.cobblemondaycare.CobblemonDaycare;
import bat.cobblemondaycare.nursery.model.NurseryTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MoveNurseryPokemonC2SPayload(BlockPos nurseryPos, NurseryTransfer move) implements CustomPacketPayload {
    public static final Type<MoveNurseryPokemonC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonDaycare.MOD_ID, "move_nursery_pokemon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveNurseryPokemonC2SPayload> STREAM_CODEC =
            StreamCodec.of(MoveNurseryPokemonC2SPayload::write, MoveNurseryPokemonC2SPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, MoveNurseryPokemonC2SPayload payload) {
        NurseryTransfer m = payload.move();
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeBoolean(m.toHabitat());
        buf.writeVarInt(m.yardIndex());
        buf.writeVarInt(m.partySlot());
        buf.writeBoolean(m.parentB());
        buf.writeUUID(m.expectedPokemonId());
    }

    private static MoveNurseryPokemonC2SPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new MoveNurseryPokemonC2SPayload(pos,
                new NurseryTransfer(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readUUID()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
