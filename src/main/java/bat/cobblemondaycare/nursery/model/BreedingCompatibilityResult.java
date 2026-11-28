package bat.cobblemondaycare.nursery.model;

public record BreedingCompatibilityResult(
    boolean compatible,
    String reason
) {
    public static BreedingCompatibilityResult success() {
        return new BreedingCompatibilityResult(true, "compatible");
    }

    public static BreedingCompatibilityResult failure(String reason) {
        return new BreedingCompatibilityResult(false, reason);
    }
}
