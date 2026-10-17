package aimpreview.render;

import aimpreview.AimPreviewClient;
import aimpreview.config.AimPreviewConfig;
import aimpreview.api.Trajectory;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

/** Shows how far away the landing point is, next to the crosshair. */
public final class DistanceHud {
    private DistanceHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics, float partialTick) {
        AimPreviewConfig config = AimPreviewConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (!config.enabled || !config.distanceHud || mc.player == null) return;
        Trajectory trajectory = AimPreviewClient.primary();
        if (trajectory == null || !trajectory.landed()) return;

        Vec3 eye = mc.player.getEyePosition(partialTick);
        String text = String.format(Locale.ROOT, "%." + config.hudDecimals + "fm", eye.distanceTo(trajectory.last()));
        int x = graphics.guiWidth() / 2 + config.hudOffsetX;
        int y = graphics.guiHeight() / 2 + config.hudOffsetY;
        graphics.text(mc.font, text, x, y, config.hudColor.getRGB(), config.hudShadow);
    }
}
