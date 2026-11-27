package bat.cobblenursery.nursery.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

record EggClaimBatch<T>(int claimed, List<T> remaining) {
    static <T> EggClaimBatch<T> take(List<T> entries, Predicate<T> successfullyInserted,
                                     Consumer<List<T>> saveRemaining) {
        List<T> remaining = new ArrayList<>(entries);
        int claimed = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (!successfullyInserted.test(entries.get(i))) continue;
            remaining.remove(i - claimed);
            claimed++;
            // Checkpoint the exact residual tray before any later insertion or unrelated refresh.
            saveRemaining.accept(List.copyOf(remaining));
        }
        return new EggClaimBatch<>(claimed, List.copyOf(remaining));
    }
}