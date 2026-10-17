package aimpreview.sim;

import aimpreview.config.AimPreviewConfig;
import aimpreview.mixin.AbstractArrowAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import org.jspecify.annotations.Nullable;

/** Projectiles already in the air, picked up from the client world. */
public final class Flying {
    /** A firework's fuse is not sent to the client, so its path is cut off after this many ticks. */
    private static final int FIREWORK_TICKS = 40;

    private Flying() {
    }

    public static List<Launch> launches(ClientLevel level, Player player, AimPreviewConfig config) {
        if (config.flyingMode == AimPreviewConfig.FlyingMode.OFF) return List.of();
        double range = config.flyingRange;
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile)) continue;
            if (config.flyingMode == AimPreviewConfig.FlyingMode.OWN && projectile.getOwner() != player) continue;
            if (entity.distanceToSqr(player) > range * range) continue;
            if (physicsOf(entity) == null) continue;
            found.add(entity);
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        List<Launch> launches = new ArrayList<>();
        for (Entity entity : found) {
            if (launches.size() >= config.flyingMaxCount) break;
            if (entity.getDeltaMovement().lengthSqr() < 1.0e-4) continue;
            Physics physics = physicsOf(entity);
            int lifetime = physics == Physics.FIREWORK ? entity.tickCount + FIREWORK_TICKS : -1;
            launches.add(new Launch(physics, entity.position(), entity.getDeltaMovement(), ((Projectile) entity).getOwner(), entity, entity.tickCount, lifetime));
        }
        return launches;
    }

    /** How the entity moves, or null when it is not flying freely (stuck, hooked, riding a player...). */
    private static @Nullable Physics physicsOf(Entity entity) {
        if (entity.isRemoved() || entity.noPhysics) return null;
        if (entity instanceof AbstractArrow arrow) {
            if (((AbstractArrowAccessor) arrow).aimpreview$isInGround()) return null;
            return arrow instanceof ThrownTrident ? Physics.TRIDENT : Physics.ARROW;
        }
        if (entity instanceof AbstractThrownPotion) return Physics.POTION;
        if (entity instanceof ThrownExperienceBottle) return Physics.EXPERIENCE_BOTTLE;
        if (entity instanceof ThrowableProjectile) return Physics.THROWABLE;
        if (entity instanceof AbstractWindCharge) return Physics.STRAIGHT;
        if (entity instanceof FireworkRocketEntity firework) return firework.isShotAtAngle() ? Physics.FIREWORK : null;
        if (entity instanceof FishingHook hook) {
            if (hook.onGround() || hook.getHookedIn() != null || hook.level().getFluidState(hook.blockPosition()).is(FluidTags.WATER)) return null;
            return Physics.FISHING;
        }
        return null;
    }
}
