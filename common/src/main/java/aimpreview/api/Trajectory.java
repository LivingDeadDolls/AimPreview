package aimpreview.api;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A predicted path. Part of the public API: kept compatible for add-ons.
 *
 * <p>{@code points} has one point per tick: the first is where the projectile is now (or leaves the hand), each next
 * one is where it is after one more tick, and when it hits something the last point is the exact hit location.
 * So for a hit the point before the last is where the projectile was at the start of its final tick
 * (vanilla {@code oldPosition()}, e.g. where an ender pearl teleports its owner).
 *
 * @param end    what stops it
 * @param face   the block face that was hit (only for {@link End#BLOCK})
 * @param target the entity that was hit (only for {@link End#ENTITY})
 */
public record Trajectory(List<Vec3> points, End end, @Nullable Direction face, @Nullable Entity target) {
    public enum End {
        BLOCK,
        ENTITY,
        /** A fishing bobber landing on water. */
        WATER,
        /** A firework exploding in the air. */
        EXPIRED,
        /** Nothing hit within the simulated ticks. */
        NONE
    }

    public Vec3 last() {
        return points.getLast();
    }

    /** True when it stops somewhere (hits a block or an entity, lands on water or explodes). */
    public boolean landed() {
        return end != End.NONE;
    }
}
