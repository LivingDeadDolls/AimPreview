package aimpreview.mixin;

import aimpreview.AimPreviewClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the paths as gizmos right before the renderer takes the frame's gizmos. Done here rather than during
 * extraction so that every rendered frame gets them, including frames rendered outside the normal loop.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private SimpleGizmoCollector renderThreadGizmos;

    @Inject(method = "finalizeGizmoCollection", at = @At("HEAD"))
    private void aimpreview$emitGizmos(CallbackInfo ci) {
        try (Gizmos.TemporaryCollection ignored = Gizmos.withCollector(this.renderThreadGizmos)) {
            AimPreviewClient.emitGizmos();
        }
    }
}
