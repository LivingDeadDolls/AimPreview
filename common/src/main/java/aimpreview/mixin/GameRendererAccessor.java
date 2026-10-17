package aimpreview.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.1 has no public getter for the render state (26.2 adds GameRenderer.gameRenderState()). */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("gameRenderState")
    GameRenderState aimpreview$gameRenderState();
}
