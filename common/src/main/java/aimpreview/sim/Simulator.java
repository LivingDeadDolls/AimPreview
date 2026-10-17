package aimpreview.sim;

import aimpreview.api.Trajectory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/** Steps a projectile tick by tick the way the vanilla entity would, until it hits something. */
public final class Simulator {
    /** Below this many blocks under the world the path is given up on. */
    private static final int BELOW_WORLD = 64;

    private Simulator() {
    }

    public static Trajectory simulate(Level level, Launch launch, int maxTicks) {
        return simulate(level, launch, maxTicks, true);
    }

    /** With {@code hitEntities} false only blocks stop it. */
    public static Trajectory simulate(Level level, Launch launch, int maxTicks, boolean hitEntities) {
        return simulate(level, launch, maxTicks, hitEntities, true);
    }

    /**
     * The flight through empty air: nothing stops it (only water still slows it down), so the path keeps going for
     * {@code maxTicks}. Used to find where a shot would cross a surface that is not really there.
     */
    public static Trajectory simulateFree(Level level, Launch launch, int maxTicks) {
        return simulate(level, launch, maxTicks, false, false);
    }

    private static Trajectory simulate(Level level, Launch launch, int maxTicks, boolean hitEntities, boolean hitBlocks) {
        Physics physics = launch.physics();
        Entity owner = launch.owner();
        List<Vec3> points = new ArrayList<>();
        Vec3 pos = launch.position();
        Vec3 vel = launch.velocity();
        points.add(pos);

        boolean expires = launch.lifetime() >= 0;
        int ticks = expires ? Math.min(maxTicks, launch.lifetime() - launch.age()) : maxTicks;
        double minY = level.getMinY() - BELOW_WORLD;
        boolean leftOwner = owner == null;

        for (int i = 0; i < ticks; i++) {
            boolean inWater = isWater(level, pos);
            switch (physics) {
                case ARROW, TRIDENT -> {
                    if (inWater) vel = vel.scale(physics.waterDrag);
                }
                case THROWABLE, POTION, EXPERIENCE_BOTTLE ->
                        vel = vel.add(0.0, -physics.gravity, 0.0).scale(inWater ? physics.waterDrag : physics.airDrag);
                case FISHING -> {
                    if (inWater) return new Trajectory(points, Trajectory.End.WATER, null, null);
                    vel = vel.add(0.0, -physics.gravity, 0.0);
                }
                case STRAIGHT, FIREWORK -> {
                }
            }

            if (!leftOwner) leftOwner = hasLeftOwner(owner, pos, vel);
            Vec3 to = pos.add(vel);
            if (hitBlocks || hitEntities) {
                Trajectory hit = hit(level, launch, points, pos, to, launch.age() + i, leftOwner, hitEntities);
                if (hit != null) return hit;
            }
            pos = to;
            points.add(pos);

            switch (physics) {
                case ARROW, TRIDENT -> {
                    if (!inWater) vel = vel.scale(physics.airDrag);
                    vel = vel.add(0.0, -physics.gravity, 0.0);
                }
                case FISHING -> vel = vel.scale(physics.airDrag);
                default -> {
                }
            }
            if (pos.y < minY) break;
        }
        boolean expired = expires && ticks == launch.lifetime() - launch.age();
        return new Trajectory(points, expired ? Trajectory.End.EXPIRED : Trajectory.End.NONE, null, null);
    }

    /** The first block or entity between from and to, as the finished trajectory; null if the way is clear. */
    private static @Nullable Trajectory hit(Level level, Launch launch, List<Vec3> points, Vec3 from, Vec3 to, int age, boolean leftOwner,
                                           boolean hitEntities) {
        BlockHitResult block = level.clipIncludingBorder(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        boolean blockHit = block.getType() != HitResult.Type.MISS;
        Vec3 end = blockHit ? block.getLocation() : to;

        // Vanilla ProjectileUtil.computeMargin: entities get easier to hit as the projectile ages.
        float margin = Math.max(0.0f, Math.min(0.3f, (age - 2) / 20.0f));
        Entity owner = launch.owner();
        Entity target = null;
        Vec3 targetAt = null;
        double best = Double.MAX_VALUE;
        AABB area = new AABB(from, end).inflate(1.0 + margin);
        List<Entity> candidates = hitEntities ? level.getEntities(launch.self(), area, e -> canHit(e, owner, leftOwner)) : List.of();
        for (Entity entity : candidates) {
            Optional<Vec3> at = entity.getBoundingBox().inflate(margin).clip(from, end);
            if (at.isPresent()) {
                double distance = from.distanceToSqr(at.get());
                if (distance < best) {
                    best = distance;
                    target = entity;
                    targetAt = at.get();
                }
            }
        }

        if (target != null) {
            points.add(targetAt);
            return new Trajectory(points, Trajectory.End.ENTITY, null, target);
        }
        if (blockHit) {
            points.add(end);
            Direction face = block.getDirection();
            return new Trajectory(points, Trajectory.End.BLOCK, face, null);
        }
        return null;
    }

    private static boolean canHit(Entity entity, @Nullable Entity owner, boolean leftOwner) {
        if (!entity.canBeHitByProjectile()) return false;
        if (owner == null || leftOwner) return true;
        return entity != owner && !owner.isPassengerOfSameVehicle(entity);
    }

    /** Vanilla Projectile.isOutsideOwnerCollisionRange: the shooter can be hit once the projectile is clear of it. */
    private static boolean hasLeftOwner(Entity owner, Vec3 pos, Vec3 vel) {
        AABB box = AABB.ofSize(pos, 0.25, 0.25, 0.25).expandTowards(vel).inflate(1.0);
        return owner.getRootVehicle().getSelfAndPassengers()
                .filter(EntitySelector.CAN_BE_PICKED)
                .noneMatch(e -> box.intersects(e.getBoundingBox()));
    }

    private static boolean isWater(Level level, Vec3 pos) {
        return level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
    }
}
