package aimpreview.sim;

import net.minecraft.world.phys.Vec3;

/**
 * How vanilla randomizes a shot. Each of the three axes gets its own random offset in [-max, max]
 * (a triangle distribution, so offsets near 0 are the most likely).
 *
 * <p>Most shots (Projectile.getMovementToShoot): {@code direction.normalize() + offset}, times the power, plus the
 * shooter's movement; max is 0.0172275 × uncertainty, and players always shoot with uncertainty 1.
 * <br>Fishing bobber: each axis of the unnormalized direction is multiplied by {@code factor + offset}; max 0.0103365.
 */
public record Spread(Vec3 direction, double power, Vec3 movement, double max, boolean perAxisFactor) {
    /** Projectile.getMovementToShoot with uncertainty 1. */
    public static Spread shot(Vec3 direction, double power, Vec3 movement) {
        return new Spread(direction.normalize(), power, movement, 0.0172275, false);
    }

    /** FishingHook(Player, ...): the direction is scaled per axis by factor + a random offset. */
    public static Spread fishing(Vec3 direction, double factor) {
        return new Spread(direction, factor, Vec3.ZERO, 0.0103365, true);
    }

    /** The velocity for the given random offset (each axis within ±max; zero is the shot without spread). */
    public Vec3 velocity(Vec3 offset) {
        if (perAxisFactor) {
            return new Vec3(direction.x * (power + offset.x), direction.y * (power + offset.y), direction.z * (power + offset.z));
        }
        return direction.add(offset).scale(power).add(movement);
    }
}
