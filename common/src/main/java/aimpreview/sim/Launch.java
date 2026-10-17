package aimpreview.sim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A projectile at one moment: where it is, how fast it moves and how it moves from here on.
 *
 * @param owner     the shooter; not hit until the projectile has left it, like vanilla
 * @param self      the projectile entity when it already exists (never hits itself)
 * @param age       ticks the projectile has already lived (vanilla widens the entity hit margin with age)
 * @param lifetime  ticks until it disappears by itself (fireworks), or -1
 * @param spread    the random spread vanilla adds when it is shot, or null when it is already flying
 */
public record Launch(Physics physics, Vec3 position, Vec3 velocity, @Nullable Entity owner, @Nullable Entity self, int age, int lifetime,
                     @Nullable Spread spread) {
    public Launch(Physics physics, Vec3 position, Vec3 velocity, @Nullable Entity owner, @Nullable Entity self, int age, int lifetime) {
        this(physics, position, velocity, owner, self, age, lifetime, null);
    }

    /** About to be shot: the velocity is the one without random spread. */
    public Launch(Physics physics, Vec3 position, Spread spread, @Nullable Entity owner) {
        this(physics, position, spread.velocity(Vec3.ZERO), owner, null, 0, -1, spread);
    }

    public Launch withLifetime(int lifetime) {
        return new Launch(physics, position, velocity, owner, self, age, lifetime, spread);
    }

    /** The same shot with the given random offset (see {@link Spread#velocity}). */
    public Launch withOffset(Vec3 offset) {
        return new Launch(physics, position, spread == null ? velocity : spread.velocity(offset), owner, self, age, lifetime, spread);
    }
}
