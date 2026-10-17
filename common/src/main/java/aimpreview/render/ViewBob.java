package aimpreview.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Takes vanilla's view bobbing back out of the aimed path. With "View Bobbing" on, walking sways the whole world
 * against the crosshair (GameRenderer.bobView), and the path right in front of the eyes sways the most.
 * Points are moved so that they show where they would without the sway, fully at the eye and less and less
 * towards the landing point, which stays on the ground it lands on.
 */
public final class ViewBob {
    private final Vec3 camera;
    private final Matrix4f view;
    private final Matrix4f viewInverse;
    private final Matrix4f bobInverse;

    private ViewBob(Vec3 camera, Matrix4f view, Matrix4f bobInverse) {
        this.camera = camera;
        this.view = view;
        this.viewInverse = new Matrix4f(view).invert();
        this.bobInverse = bobInverse;
    }

    /** The sway of the frame being drawn, or null when there is none. */
    public static @Nullable ViewBob current(Minecraft mc) {
        if (!mc.options.bobView().get()) return null;
        CameraRenderState camera = camera(mc);
        if (camera == null || !camera.entityRenderState.isPlayer) return null;
        float walked = camera.entityRenderState.backwardsInterpolatedWalkDistance;
        float bob = camera.entityRenderState.bob;
        if (bob == 0.0f) return null;
        // The same steps as GameRenderer.bobView, applied to the projection.
        Matrix4f sway = new Matrix4f()
                .translate(Mth.sin(walked * Mth.PI) * bob * 0.5f, -Math.abs(Mth.cos(walked * Mth.PI) * bob), 0.0f)
                .rotateZ(Mth.sin(walked * Mth.PI) * bob * 3.0f * Mth.DEG_TO_RAD)
                .rotateX(Math.abs(Mth.cos(walked * Mth.PI - 0.2f) * bob) * 5.0f * Mth.DEG_TO_RAD);
        return new ViewBob(camera.pos, new Matrix4f(camera.viewRotationMatrix), sway.invert());
    }

    private static @Nullable CameraRenderState camera(Minecraft mc) {
        return mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
    }

    /**
     * @param weight 1 to take the sway out completely, 0 to leave the point where it is
     */
    public Vec3 steady(Vec3 point, double weight) {
        if (weight <= 0.0) return point;
        Vector4f v = new Vector4f((float) (point.x - camera.x), (float) (point.y - camera.y), (float) (point.z - camera.z), 1.0f);
        view.transform(v);
        bobInverse.transform(v);
        viewInverse.transform(v);
        Vec3 steadied = new Vec3(camera.x + v.x, camera.y + v.y, camera.z + v.z);
        return point.add(steadied.subtract(point).scale(Math.min(1.0, weight)));
    }
}
