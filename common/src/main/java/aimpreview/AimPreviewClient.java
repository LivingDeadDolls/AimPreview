package aimpreview;

import aimpreview.api.AimPreviewApi;
import aimpreview.api.Trajectory;
import aimpreview.config.AimPreviewConfig;
import aimpreview.render.TrajectoryRenderer;
import aimpreview.render.ViewBob;
import aimpreview.sim.Aim;
import aimpreview.sim.Flying;
import aimpreview.sim.Launch;
import aimpreview.sim.Simulator;
import aimpreview.sim.SpreadArea;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Keeps the current predictions and hands them to the renderer, the HUD and the hit sound. */
public final class AimPreviewClient {
    /** Paths of what the player would shoot, recomputed every frame so they follow the camera smoothly. */
    private static List<Trajectory> aimed = List.of();
    /** Paths of projectiles in the air, recomputed every tick. */
    private static List<Trajectory> flying = List.of();
    /** The projectile of each path in {@link #flying}, in the same order. */
    private static List<Entity> flyingEntities = List.of();
    private static @Nullable Entity lastTarget;

    private AimPreviewClient() {
    }

    /** Called by the level renderer each frame, while a gizmo collector is active. */
    public static void emitGizmos() {
        Minecraft mc = Minecraft.getInstance();
        AimPreviewConfig config = AimPreviewConfig.get();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !visible(mc, config)) {
            aimed = List.of();
            return;
        }
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Aim.Aimed aim = Aim.aim(player, pt, config);
        aimed = simulate(mc, aim.launches(), config);

        Vec3 eye = player.getEyePosition(pt);
        ViewBob bob = config.steadyLine ? ViewBob.current(mc) : null;
        int lineColor = config.lineColor.getRGB();
        int hitColor = config.hitColor.getRGB();
        for (Trajectory trajectory : aimed) {
            boolean onTarget = trajectory.end() == Trajectory.End.ENTITY;
            int color = config.colorOnHit && onTarget ? hitColor : lineColor;
            TrajectoryRenderer.draw(trajectory, eye, bob, color, config.marker, config.highlight, pt, config);
        }
        if (config.spread) {
            for (int i = 0; i < aimed.size(); i++) {
                SpreadArea area = SpreadArea.of(mc.level, aim.launches().get(i), aimed.get(i), config.spreadScale, config.maxTicks);
                if (area != null) TrajectoryRenderer.drawSpread(area, config);
            }
        }
        int flyingColor = config.flyingColor.getRGB();
        for (Trajectory trajectory : flying) {
            // Not trimmed: these paths start at the projectile, not at the player's eye.
            TrajectoryRenderer.draw(trajectory, null, flyingColor, config.flyingMarker, config.flyingHighlight, pt, config);
        }

        if (!aimed.isEmpty()) AimPreviewApi.fireAimed(aim.stack(), aimed, pt);
        for (int i = 0; i < flying.size(); i++) AimPreviewApi.fireFlying(flyingEntities.get(i), flying.get(i), pt);
    }

    public static void tick(Minecraft mc) {
        AimPreviewConfig config = AimPreviewConfig.get();
        if (mc.player == null || mc.level == null || !visible(mc, config)) {
            flying = List.of();
            flyingEntities = List.of();
            lastTarget = null;
            return;
        }
        List<Launch> launches = Flying.launches(mc.level, mc.player, config);
        flying = simulate(mc, launches, config);
        flyingEntities = launches.stream().map(Launch::self).toList();

        Entity target = target();
        if (config.sound && target != null && target != lastTarget) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound(config.soundType), config.soundPitch, config.soundVolume));
        }
        lastTarget = target;
    }

    /** The toggle key: turns the whole mod on or off and remembers it. */
    public static void toggle(Minecraft mc) {
        AimPreviewConfig config = AimPreviewConfig.get();
        config.enabled = !config.enabled;
        AimPreviewConfig.HANDLER.save();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.translatable(config.enabled ? "aimpreview.message.on" : "aimpreview.message.off"));
        }
    }

    /** What the player is aiming at would hit first, or null. */
    public static @Nullable Entity target() {
        for (Trajectory trajectory : aimed) {
            if (trajectory.end() == Trajectory.End.ENTITY) return trajectory.target();
        }
        return null;
    }

    /** The path of the main projectile the player is aiming with (the middle one for multishot), or null. */
    public static @Nullable Trajectory primary() {
        return aimed.isEmpty() ? null : aimed.getFirst();
    }

    public static List<Trajectory> aimed() {
        return aimed;
    }

    public static List<Trajectory> flying() {
        return flying;
    }

    private static boolean visible(Minecraft mc, AimPreviewConfig config) {
        return config.enabled && !(config.hideWithHud && mc.gui.hud.isHidden());
    }

    private static List<Trajectory> simulate(Minecraft mc, List<Launch> launches, AimPreviewConfig config) {
        if (launches.isEmpty()) return List.of();
        List<Trajectory> result = new ArrayList<>(launches.size());
        for (Launch launch : launches) result.add(Simulator.simulate(mc.level, launch, config.maxTicks));
        return result;
    }

    private static SoundEvent sound(AimPreviewConfig.HitSound sound) {
        return switch (sound) {
            case DING -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case BELL -> SoundEvents.NOTE_BLOCK_BELL.value();
            case CHIME -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case PLING -> SoundEvents.NOTE_BLOCK_PLING.value();
            case CLICK -> SoundEvents.UI_BUTTON_CLICK.value();
        };
    }
}
