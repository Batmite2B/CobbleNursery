package bat.cobblemondaycare.network;

/** Server-side authorization gate for all daycare packets. */
final class NurseryAccessPolicy {
    private NurseryAccessPolicy() {}

    static boolean allows(boolean active, boolean sameDimension, boolean samePosition,
                          double distanceSquared, boolean liveEndpoint) {
        return active && sameDimension && samePosition && distanceSquared <= 64.0D && liveEndpoint;
    }
}
