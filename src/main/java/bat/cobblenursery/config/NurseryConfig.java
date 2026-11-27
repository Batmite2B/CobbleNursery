package bat.cobblenursery.config;

public record NurseryConfig(
    int baseDurationMinutes,
    int baseProcessSlots,
    int startCostPokedollars,
    boolean allowDitto,
    int yardEggTrayCapacity,
    int shinyOddsDenominator
) {
    public static final NurseryConfig DEFAULT = new NurseryConfig(
        5,
        1,
        500,
        true,
        8,
        4096
    );
}
