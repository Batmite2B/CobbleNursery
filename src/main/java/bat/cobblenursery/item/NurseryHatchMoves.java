package bat.cobblenursery.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/** Stable four-slot hatch order: saved inheritance first, initialized defaults second. */
final class NurseryHatchMoves {
    private static final int CAPACITY = 4;

    static <T> List<T> equipped(List<String> inherited, List<T> defaults,
                                Function<String, T> resolveInherited, Function<T, String> name) {
        var ordered = new LinkedHashMap<String, T>();
        for (var id : inherited) {
            if (ordered.size() == CAPACITY) break;
            if (id == null || id.isBlank()) continue;
            T move = resolveInherited.apply(id);
            if (move != null) ordered.putIfAbsent(name.apply(move), move);
        }
        for (var move : defaults) {
            if (ordered.size() == CAPACITY) break;
            ordered.putIfAbsent(name.apply(move), move);
        }
        return List.copyOf(ordered.values());
    }

    private NurseryHatchMoves() {}
}