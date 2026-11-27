package bat.cobblenursery.network;

import bat.cobblenursery.CobbleNursery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OpenNurseryScreenS2CPayload(
    BlockPos nurseryPos,
    int startCost,
    boolean canAfford,
    boolean canForceReady,
    List<NurseryPartySlotPayload> partySlots,
    List<NurseryProcessSlotPayload> processSlots,
    NurseryPreviewPayload preview,
    String message,
    boolean messageSuccess
) implements CustomPacketPayload {
    public static final Type<OpenNurseryScreenS2CPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CobbleNursery.MOD_ID, "open_nursery_screen_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNurseryScreenS2CPayload> STREAM_CODEC =
        StreamCodec.of(OpenNurseryScreenS2CPayload::write, OpenNurseryScreenS2CPayload::read);

    public OpenNurseryScreenS2CPayload {
        partySlots = List.copyOf(partySlots);
        processSlots = List.copyOf(processSlots);
        message = message == null ? "" : message;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenNurseryScreenS2CPayload payload) {
        buf.writeBlockPos(payload.nurseryPos());
        buf.writeVarInt(payload.startCost());
        buf.writeBoolean(payload.canAfford());
        buf.writeBoolean(payload.canForceReady());
        buf.writeVarInt(payload.partySlots().size());
        for (NurseryPartySlotPayload partySlot : payload.partySlots()) {
            NurseryPartySlotPayload.write(buf, partySlot);
        }
        buf.writeVarInt(payload.processSlots().size());
        for (NurseryProcessSlotPayload processSlot : payload.processSlots()) {
            NurseryProcessSlotPayload.write(buf, processSlot);
        }
        buf.writeBoolean(payload.preview() != null);
        if (payload.preview() != null) {
            NurseryPreviewPayload.write(buf, payload.preview());
        }
        buf.writeUtf(payload.message());
        buf.writeBoolean(payload.messageSuccess());
    }

    private static OpenNurseryScreenS2CPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int startCost = buf.readVarInt();
        boolean canAfford = buf.readBoolean();
        boolean canForceReady = buf.readBoolean();
        int partySize = buf.readVarInt();
        List<NurseryPartySlotPayload> partySlots = new ArrayList<>(partySize);
        for (int i = 0; i < partySize; i++) {
            partySlots.add(NurseryPartySlotPayload.read(buf));
        }
        int processSize = buf.readVarInt();
        List<NurseryProcessSlotPayload> processSlots = new ArrayList<>(processSize);
        for (int i = 0; i < processSize; i++) {
            processSlots.add(NurseryProcessSlotPayload.read(buf));
        }
        NurseryPreviewPayload preview = buf.readBoolean() ? NurseryPreviewPayload.read(buf) : null;
        String message = buf.readUtf();
        boolean messageSuccess = buf.readBoolean();
        return new OpenNurseryScreenS2CPayload(
            pos,
            startCost,
            canAfford,
            canForceReady,
            partySlots,
            processSlots,
            preview,
            message,
            messageSuccess
        );
    }
}
