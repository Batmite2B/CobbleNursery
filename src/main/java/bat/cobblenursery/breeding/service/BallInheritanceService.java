package bat.cobblenursery.breeding.service;

import bat.cobblenursery.nursery.model.ParentSnapshot;

import java.util.Random;
import java.util.Set;

public final class BallInheritanceService {
    private static final String DEFAULT_BALL = "cobblemon:poke_ball";
    private static final Set<String> NON_INHERITABLE_BALLS = Set.of(
        "cobblemon:master_ball",
        "cobblemon:cherish_ball",
        "cobblemon:strange_ball"
    );

    public String resolveInheritedBall(ParentSnapshot a, ParentSnapshot b, long seed) {
        ParentSnapshot source = resolveSourceParent(a, b, seed);
        String inheritedBall = source.ballId();
        if (inheritedBall == null || inheritedBall.isBlank() || NON_INHERITABLE_BALLS.contains(inheritedBall)) {
            return DEFAULT_BALL;
        }
        return inheritedBall;
    }

    private ParentSnapshot resolveSourceParent(ParentSnapshot a, ParentSnapshot b, long seed) {
        if (a.ditto()) {
            return b;
        }
        if (b.ditto()) {
            return a;
        }
        if (a.speciesId().equals(b.speciesId())) {
            return new Random(seed ^ 0x6A09E667F3BCC909L).nextBoolean() ? a : b;
        }
        if (a.isFemale()) {
            return a;
        }
        if (b.isFemale()) {
            return b;
        }
        return a;
    }
}
