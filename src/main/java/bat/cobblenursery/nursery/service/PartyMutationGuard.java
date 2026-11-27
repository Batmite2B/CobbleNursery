package bat.cobblenursery.nursery.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class PartyMutationGuard {
    enum Result { COMMITTED, REJECTED, INCONSISTENT }

    record Observation(boolean exactSlot, boolean exactIndex, boolean emptyIndex, boolean exactCoordinates,
                       boolean detached, boolean sameUuidInOtherSlot) {
        boolean present() { return exactSlot && exactIndex && exactCoordinates && !sameUuidInOtherSlot; }
        boolean absent() { return !exactSlot && emptyIndex && detached && !sameUuidInOtherSlot; }
        boolean matches(boolean placing) { return placing ? present() : absent(); }
    }

    private PartyMutationGuard() {}

    static Result apply(boolean placing, Supplier<Observation> observe, Runnable mutate, Runnable reconcile,
                        Runnable rollback, Consumer<RuntimeException> report) {
        if (!observe.get().matches(!placing)) return Result.REJECTED;
        attempt(mutate, report);
        var observed = observe.get();
        if (observed.matches(placing)) return Result.COMMITTED;
        if (observed.matches(!placing)) return Result.REJECTED;
        attempt(reconcile, report);
        if (observe.get().matches(placing)) return Result.COMMITTED;
        attempt(rollback, report);
        observed = observe.get();
        if (observed.matches(!placing)) return Result.REJECTED;
        // A recovery action may itself throw after performing the mutation.
        return observed.matches(placing) ? Result.COMMITTED : Result.INCONSISTENT;
    }

    private static void attempt(Runnable action, Consumer<RuntimeException> report) {
        try { action.run(); }
        catch (RuntimeException exception) { report.accept(exception); }
    }
}
