package bat.cobblenursery.network;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record DaycareParentPreview(String nickname, int level, String cosmetic, List<String> aspects,
                                  List<String> moves, List<Integer> ivs, List<Integer> evs) {
    private static final int ASPECT_LIMIT = 48;
    private static final int TOKEN_LIMIT = 160;
    public DaycareParentPreview {
        aspects = List.copyOf(aspects);
        moves = List.copyOf(moves);
        ivs = List.copyOf(ivs);
        evs = List.copyOf(evs);
        if (nickname.length() > 80 || cosmetic.length() > TOKEN_LIMIT || level < 1 || level > 100
                || aspects.size() > ASPECT_LIMIT || moves.size() > 4
                || ivs.size() != 6 || evs.size() != 6
                || aspects.stream().anyMatch(x -> x.length() > TOKEN_LIMIT)
                || moves.stream().anyMatch(x -> x.length() > TOKEN_LIMIT)
                || ivs.stream().anyMatch(x -> x < 0 || x > 31)
                || evs.stream().anyMatch(x -> x < 0 || x > 252)) {
            throw new IllegalArgumentException("Invalid nursery parent preview");
        }
    }

    public static DaycareParentPreview fromPokemon(Pokemon pokemon, String cosmetic) {
        var stats = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);
        String nickname = pokemon.getNickname() == null ? "" : pokemon.getNickname().getString();
        return new DaycareParentPreview(nickname.length() > 80 ? nickname.substring(0, 80) : nickname,
                Math.clamp(pokemon.getLevel(), 1, 100), cosmetic.length() <= TOKEN_LIMIT ? cosmetic : "",
                pokemon.getAspects().stream().filter(x -> x.length() <= TOKEN_LIMIT).sorted().limit(ASPECT_LIMIT).toList(),
                pokemon.getMoveSet().getMoves().stream().map(move -> move.getName())
                    .filter(x -> x.length() <= TOKEN_LIMIT).limit(4).toList(),
                stats.stream().map(stat -> Math.clamp(pokemon.getIvs().getOrDefault(stat), 0, 31)).toList(),
                stats.stream().map(stat -> Math.clamp(pokemon.getEvs().getOrDefault(stat), 0, 252)).toList());
    }

    void write(FriendlyByteBuf buf) {
        buf.writeUtf(nickname, 80);
        buf.writeVarInt(level);
        buf.writeUtf(cosmetic, TOKEN_LIMIT);
        writeStrings(buf, aspects);
        writeStrings(buf, moves);
        ivs.forEach(buf::writeVarInt);
        evs.forEach(buf::writeVarInt);
    }

    static DaycareParentPreview read(FriendlyByteBuf buf) {
        String nickname = buf.readUtf(80);
        int level = buf.readVarInt();
        String cosmetic = buf.readUtf(TOKEN_LIMIT);
        var aspects = readStrings(buf, ASPECT_LIMIT);
        var moves = readStrings(buf, 4);
        var ivs = new ArrayList<Integer>(6);
        var evs = new ArrayList<Integer>(6);
        for (int i = 0; i < 6; i++) ivs.add(buf.readVarInt());
        for (int i = 0; i < 6; i++) evs.add(buf.readVarInt());
        return new DaycareParentPreview(nickname, level, cosmetic, aspects, moves, ivs, evs);
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        values.forEach(value -> buf.writeUtf(value, TOKEN_LIMIT));
    }

    private static List<String> readStrings(FriendlyByteBuf buf, int max) {
        int size = buf.readVarInt();
        if (size < 0 || size > max) throw new IllegalArgumentException("Invalid nursery list length");
        var values = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) values.add(buf.readUtf(TOKEN_LIMIT));
        return values;
    }
}