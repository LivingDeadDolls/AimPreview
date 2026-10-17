package aimpreview;

import aimpreview.render.DistanceHud;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class AimPreviewFabric implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    @Override
    public void onInitializeClient() {
        AimPreviewCommon.init();

        // Unbound by default; set it in Controls.
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.aimpreview.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (toggle.consumeClick()) {
                AimPreviewClient.toggle(minecraft);
            }
            AimPreviewClient.tick(minecraft);
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "distance"),
                (graphics, deltaTracker) -> DistanceHud.extract(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }
}
