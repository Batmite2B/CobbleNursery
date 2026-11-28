package bat.cobblemondaycare.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DaycareParentPayload(
    UUID pokemonId,
    String speciesId,
    String formId,
    String gender,
    String abilityId,
    String natureId,
    String ballId,
    String heldItemId,
    List<String> eggGroups, DaycareParentPreview preview
) {
    public DaycareParentPayload {
        eggGroups = List.copyOf(eggGroups);
    }

    public static void write(FriendlyByteBuf buf, DaycareParentPayload payload) {
        buf.writeUUID(payload.pokemonId());
        buf.writeUtf(payload.speciesId());
        buf.writeUtf(payload.formId());
        buf.writeUtf(payload.gender());
        buf.writeUtf(payload.abilityId());
        buf.writeUtf(payload.natureId());
        buf.writeUtf(payload.ballId());
        buf.writeUtf(payload.heldItemId());
        buf.writeVarInt(payload.eggGroups().size());
        for (String eggGroup : payload.eggGroups()) {
            buf.writeUtf(eggGroup);
        }
        buf.writeBoolean(payload.preview() != null);
        if (payload.preview() != null) payload.preview().write(buf);
    }

    public static DaycareParentPayload read(FriendlyByteBuf buf) {
        UUID pokemonId = buf.readUUID();
        String speciesId = buf.readUtf();
        String formId = buf.readUtf();
        String gender = buf.readUtf();
        String abilityId = buf.readUtf();
        String natureId = buf.readUtf();
        String ballId = buf.readUtf();
        String heldItemId = buf.readUtf();
        int eggGroupCount = buf.readVarInt();
        if (eggGroupCount < 0 || eggGroupCount > 32) throw new IllegalArgumentException("Invalid egg group list");
        List<String> eggGroups = new ArrayList<>(Math.min(eggGroupCount, 32));
        for (int i = 0; i < eggGroupCount; i++) {
            eggGroups.add(buf.readUtf());
        }
        return new DaycareParentPayload(pokemonId, speciesId, formId, gender, abilityId, natureId, ballId, heldItemId, eggGroups,
            buf.readBoolean() ? DaycareParentPreview.read(buf) : null);
    }
}
