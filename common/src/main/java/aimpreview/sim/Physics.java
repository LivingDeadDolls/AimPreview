package aimpreview.sim;

/**
 * How a projectile moves each tick, following the vanilla entity's tick().
 * Numbers are the vanilla gravity / drag values; the order of the steps differs between families.
 */
public enum Physics {
    /** AbstractArrow: water drag, move, air drag, then gravity. */
    ARROW(0.05, 0.99, 0.6),
    /** ThrownTrident: an arrow that barely slows down in water. */
    TRIDENT(0.05, 0.99, 0.99),
    /** Snowball, egg, ender pearl: gravity, drag, then move. */
    THROWABLE(0.03, 0.99, 0.8),
    /** Splash and lingering potions. */
    POTION(0.05, 0.99, 0.8),
    /** Bottle o' enchanting. */
    EXPERIENCE_BOTTLE(0.07, 0.99, 0.8),
    /** Wind charge: a straight line at constant speed. */
    STRAIGHT(0.0, 1.0, 1.0),
    /** Firework shot from a crossbow: a straight line until it explodes. */
    FIREWORK(0.0, 1.0, 1.0),
    /** Fishing bobber: gravity, move, then drag; stops on water. */
    FISHING(0.03, 0.92, 0.92);

    public final double gravity;
    public final double airDrag;
    public final double waterDrag;

    Physics(double gravity, double airDrag, double waterDrag) {
        this.gravity = gravity;
        this.airDrag = airDrag;
        this.waterDrag = waterDrag;
    }
}
