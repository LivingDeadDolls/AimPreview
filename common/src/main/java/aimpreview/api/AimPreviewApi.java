package aimpreview.api;

import aimpreview.Constants;
import aimpreview.config.AimPreviewConfig;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * For add-ons that draw more on top of AimPreview's paths. Kept compatible across versions.
 *
 * <p>Listeners are called on the render thread every frame, right after AimPreview has drawn its own paths and while
 * the frame's gizmo collector is still open, so they can add {@code net.minecraft.gizmos.Gizmos} directly.
 * They are not called while AimPreview is turned off or hidden.
 */
public final class AimPreviewApi {
    private static final List<AimedListener> AIMED = new CopyOnWriteArrayList<>();
    private static final List<FlyingListener> FLYING = new CopyOnWriteArrayList<>();

    private AimPreviewApi() {
    }

    /** What the player is aiming with right now. */
    @FunctionalInterface
    public interface AimedListener {
        /**
         * @param stack        the item in the hand that would shoot or throw
         * @param trajectories its paths (more than one for a Multishot crossbow, the middle one first); never empty
         */
        void onAimed(ItemStack stack, List<Trajectory> trajectories, float partialTick);
    }

    /** A projectile already in the air, among those AimPreview shows (see its "Projectiles in flight" settings). */
    @FunctionalInterface
    public interface FlyingListener {
        void onFlying(Entity projectile, Trajectory trajectory, float partialTick);
    }

    public static void registerAimedListener(AimedListener listener) {
        AIMED.add(listener);
    }

    public static void registerFlyingListener(FlyingListener listener) {
        FLYING.add(listener);
    }

    /** Whether paths are drawn through blocks; add-ons should draw theirs the same way. */
    public static boolean throughWalls() {
        return AimPreviewConfig.get().throughWalls;
    }

    // Called by AimPreview.

    public static void fireAimed(ItemStack stack, List<Trajectory> trajectories, float partialTick) {
        for (AimedListener listener : AIMED) {
            try {
                listener.onAimed(stack, trajectories, partialTick);
            } catch (RuntimeException e) {
                Constants.LOG.error("An AimPreview add-on failed while drawing", e);
            }
        }
    }

    public static void fireFlying(Entity projectile, Trajectory trajectory, float partialTick) {
        for (FlyingListener listener : FLYING) {
            try {
                listener.onFlying(projectile, trajectory, partialTick);
            } catch (RuntimeException e) {
                Constants.LOG.error("An AimPreview add-on failed while drawing", e);
            }
        }
    }
}
