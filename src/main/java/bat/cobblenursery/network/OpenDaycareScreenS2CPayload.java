package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import bat.cobblenursery.config.ServerNurserySettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OpenDaycareScreenS2CPayload(
    BlockPos nurseryPos,
    boolean canForceReady,
    List<DaycarePartyPokemonPayload> partySlots,
    List<DaycareYardPayload> yards,
    List<DaycareEggTrayPayload> eggTray,
    int selectedYardIndex,
    int eggTrayCapacity,
    String habitatUnlockItem,
    int shinyOddsDenominator,
    String message,
    boolean messageSuccess
) implements CustomPacketPayload {
    public static final Type<OpenDaycareScreenS2CPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "open_daycare_screen_s2c_v4"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDaycareScreenS2CPayload> STREAM_CODEC =
        StreamCodec.of(OpenDaycareScreenS2CPayload::write, OpenDaycareScreenS2CPayload::read);

    public OpenDaycareScreenS2CPayload {
        partySlots = List.copyOf(partySlots);
        yards = List.copyOf(yards);
        eggTray = List.copyOf(eggTray);
        message = message == null ? "" : message;
    }

    public OpenDaycareScreenS2CPayload(BlockPos nurseryPos, boolean canForceReady,
            List<DaycarePartyPokemonPayload> partySlots, List<DaycareYardPayload> yards,
            List<DaycareEggTrayPayload> eggTray, int selectedYardIndex, int eggTrayCapacity,
            String message, boolean messageSuccess) {
        this(nurseryPos, canForceReady, partySlots, yards, eggTray, selectedYardIndex, eggTrayCapacity,
                ServerNurserySettings.DEFAULT_ITEM, ServerNurserySettings.DEFAULT.breeding().shinyOddsDenominator(),
                message, messageSuccess);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenDaycareScreenS2CPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeBoolean(payload.canForceReady());
        buf.writeVarInt(payload.partySlots().size());
        for (DaycarePartyPokemonPayload partySlot : payload.partySlots()) {
            DaycarePartyPokemonPayload.write(buf, partySlot);
        }
        buf.writeVarInt(payload.yards().size());
        for (DaycareYardPayload yard : payload.yards()) {
            DaycareYardPayload.write(buf, yard);
        }
        buf.writeVarInt(payload.eggTray().size());
        for (DaycareEggTrayPayload egg : payload.eggTray()) {
            DaycareEggTrayPayload.write(buf, egg);
        }
        buf.writeVarInt(payload.selectedYardIndex());
        buf.writeVarInt(payload.eggTrayCapacity());
        buf.writeUtf(payload.habitatUnlockItem(), 256);
        buf.writeVarInt(payload.shinyOddsDenominator());
        buf.writeUtf(payload.message());
        buf.writeBoolean(payload.messageSuccess());
    }

    private static OpenDaycareScreenS2CPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos nurseryPos = buf.readBlockPos();
        boolean canForceReady = buf.readBoolean();

        int partyCount = buf.readVarInt();
        List<DaycarePartyPokemonPayload> partySlots = new ArrayList<>(partyCount);
        for (int i = 0; i < partyCount; i++) {
            partySlots.add(DaycarePartyPokemonPayload.read(buf));
        }

        int yardCount = buf.readVarInt();
        List<DaycareYardPayload> yards = new ArrayList<>(yardCount);
        for (int i = 0; i < yardCount; i++) {
            yards.add(DaycareYardPayload.read(buf));
        }

        int eggCount = buf.readVarInt();
        List<DaycareEggTrayPayload> eggTray = new ArrayList<>(eggCount);
        for (int i = 0; i < eggCount; i++) {
            eggTray.add(DaycareEggTrayPayload.read(buf));
        }

        return new OpenDaycareScreenS2CPayload(
            nurseryPos,
            canForceReady,
            partySlots,
            yards,
            eggTray,
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readUtf(256),
            buf.readVarInt(),
            buf.readUtf(),
            buf.readBoolean()
        );
    }
}
