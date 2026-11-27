package bat.cobblenursery.nursery.service;

import bat.cobblenursery.nursery.model.NurseryEggTrayEntry;
import bat.cobblenursery.nursery.model.NurseryPlayerState;
import bat.cobblenursery.nursery.model.NurseryYardRecord;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class HopperEggOutput {
    private HopperEggOutput() {}

    static boolean transferOne(NurseryPlayerState state, int yardMask, Predicate<NurseryEggTrayEntry> deliver,
                               Consumer<NurseryPlayerState> checkpoint) {
        NurseryYardRecord source = null;
        NurseryEggTrayEntry oldest = null;
        for (var yard : state.yards()) {
            if (yard.yardIndex() < 0 || yard.yardIndex() >= state.unlockedYardSlots()
                    || yard.yardIndex() >= NurseryPlayerState.MAX_YARDS || (yardMask & (1 << yard.yardIndex())) == 0) continue;
            for (var entry : yard.eggs()) {
                if (oldest == null || entry.createdAtEpochMs() < oldest.createdAtEpochMs()) {
                    oldest = entry;
                    source = yard;
                }
            }
        }
        if (oldest == null || !deliver.test(oldest)) return false;
        var remaining = new ArrayList<>(source.eggs());
        remaining.remove(oldest);
        checkpoint.accept(state.upsertYard(source.withEggs(remaining)));
        return true;
    }
}
