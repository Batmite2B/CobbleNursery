package bat.cobblenursery.terminal;

import java.util.Objects;
import java.util.UUID;

public final class HopperAutomationRules {
    private HopperAutomationRules() {}

    public static boolean mayConfigure(UUID placer, UUID actor) {
        return placer != null && placer.equals(actor);
    }

    public static boolean mayInsert(boolean occupied, UUID nestOwner, UUID eggOwner) {
        return !occupied || Objects.equals(nestOwner, eggOwner);
    }
}
