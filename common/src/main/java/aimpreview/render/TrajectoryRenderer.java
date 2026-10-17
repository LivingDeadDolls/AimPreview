package aimpreview.render;

import aimpreview.config.AimPreviewConfig;
import aimpreview.api.Trajectory;
import aimpreview.sim.SpreadArea;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws a trajectory with vanilla gizmos. Must be called while a gizmo collector is active. */
public final class TrajectoryRenderer {
    private TrajectoryRenderer() {
    }

    /**
     * @param eye       the player's eye; the path closer to it than the start distance is left out (null draws it all)
     * @param color     line color (ARGB)
     * @param marker    draw the landing marker
     * @param highlight outline the entity that would be hit
     */
    public static void draw(Trajectory trajectory, @Nullable Vec3 eye, int color, boolean marker, boolean highlight, float partialTick, AimPreviewConfig config) {
        draw(trajectory, eye, null, color, marker, highlight, partialTick, config);
    }

    /** @param bob the view bobbing to take out of the path (fully at the eye, none at the landing point), or null */
    public static void draw(Trajectory trajectory, @Nullable Vec3 eye, @Nullable ViewBob bob, int color, boolean marker, boolean highlight,
                            float partialTick, AimPreviewConfig config) {
        List<Vec3> path = eye == null ? trajectory.points() : trimStart(trajectory.points(), eye, config.startDistance);
        if (bob != null && eye != null && !path.isEmpty()) path = steady(path, eye, bob);
        if (path.size() >= 2) {
            switch (config.lineStyle) {
                case SOLID -> {
                    for (int i = 1; i < path.size(); i++) line(path.get(i - 1), path.get(i), color, config);
                }
                case DASHED -> dashed(path, color, config);
                case CUBES -> cubes(path, color, config);
            }
        }

        if (marker) drawMarker(trajectory, config);
        if (highlight && trajectory.end() == Trajectory.End.ENTITY && trajectory.target() != null) {
            Entity target = trajectory.target();
            // The box sits where the entity is drawn this frame, not where it was at the last tick.
            Vec3 shift = target.getPosition(partialTick).subtract(target.position());
            AABB box = target.getBoundingBox().move(shift).inflate(0.05);
            onTop(Gizmos.cuboid(box, GizmoStyle.stroke(config.highlightColor.getRGB(), config.lineWidth)), config);
        }
    }

    /** The area where the shot can land with vanilla's random spread: a filled outline just above the surface. */
    public static void drawSpread(SpreadArea area, AimPreviewConfig config) {
        int fill = config.spreadColor.getRGB();
        Vec3 lift = area.normal().scale(0.005);
        List<Vec3> outline = area.outline();
        Vec3 middle = Vec3.ZERO;
        for (Vec3 p : outline) middle = middle.add(p);
        middle = middle.scale(1.0 / outline.size()).add(lift);
        GizmoStyle fillStyle = GizmoStyle.fill(fill);
        float width = Math.max(1.0f, config.lineWidth * 0.6f);
        for (int i = 0; i < outline.size(); i++) {
            Vec3 a = outline.get(i).add(lift);
            Vec3 b = outline.get((i + 1) % outline.size()).add(lift);
            // A triangle of the fan, drawn as a quad with two equal corners.
            onTop(Gizmos.rect(middle, a, b, b, fillStyle), config);
            onTop(Gizmos.line(a, b, ARGB.opaque(fill), width), config);
        }
    }

    private static void drawMarker(Trajectory trajectory, AimPreviewConfig config) {
        int fill = config.markerColor.getRGB();
        GizmoStyle style = GizmoStyle.strokeAndFill(ARGB.opaque(fill), Math.max(1.0f, config.lineWidth * 0.6f), fill);
        double half = config.markerSize / 2.0;
        switch (trajectory.end()) {
            case BLOCK -> {
                Direction face = trajectory.face() == null ? Direction.UP : trajectory.face();
                square(trajectory.last(), face, half, style, config);
            }
            case WATER -> square(trajectory.last(), Direction.UP, half, style, config);
            case EXPIRED -> onTop(Gizmos.cuboid(AABB.ofSize(trajectory.last(), half, half, half), style), config);
            default -> {
            }
        }
    }

    /** A square lying on the given face, lifted a hair off it so it is not hidden inside the block. */
    private static void square(Vec3 center, Direction face, double half, GizmoStyle style, AimPreviewConfig config) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
        Vec3 c = center.add(normal.scale(0.01));
        Vec3 u = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 v = normal.cross(u);
        u = u.scale(half);
        v = v.scale(half);
        onTop(Gizmos.rect(c.add(u).add(v), c.add(u).subtract(v), c.subtract(u).subtract(v), c.subtract(u).add(v), style), config);
    }

    private static void line(Vec3 a, Vec3 b, int color, AimPreviewConfig config) {
        onTop(Gizmos.line(a, b, color, config.lineWidth), config);
    }

    private static void onTop(GizmoProperties properties, AimPreviewConfig config) {
        if (config.throughWalls) properties.setAlwaysOnTop();
    }

    private static void dashed(List<Vec3> path, int color, AimPreviewConfig config) {
        double dash = config.dashLength;
        double walked = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            double length = a.distanceTo(b);
            double t = 0.0;
            while (t < length) {
                double inDash = walked % (2.0 * dash);
                double left = (inDash < dash ? dash : 2.0 * dash) - inDash;
                double step = Math.min(left, length - t);
                if (inDash < dash) line(lerp(a, b, t / length), lerp(a, b, (t + step) / length), color, config);
                t += step;
                walked += step;
            }
        }
    }

    private static void cubes(List<Vec3> path, int color, AimPreviewConfig config) {
        double spacing = config.cubeSpacing;
        double size = config.cubeSize;
        GizmoStyle style = GizmoStyle.fill(color);
        double untilNext = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            double length = a.distanceTo(b);
            double t = untilNext;
            while (t <= length) {
                onTop(Gizmos.cuboid(AABB.ofSize(lerp(a, b, length == 0 ? 0 : t / length), size, size, size), style), config);
                t += spacing;
            }
            untilNext = t - length;
        }
    }

    private static List<Vec3> steady(List<Vec3> path, Vec3 eye, ViewBob bob) {
        double far = path.getLast().distanceTo(eye);
        if (far <= 0.0) return path;
        List<Vec3> steadied = new ArrayList<>(path.size());
        for (Vec3 p : path) {
            // Squared, so the part near the eyes is almost fully steady and only the far end follows the ground.
            double t = p.distanceTo(eye) / far;
            steadied.add(bob.steady(p, 1.0 - t * t));
        }
        return steadied;
    }

    /** Drops the part of the path closer than {@code distance} to the eye, so it does not start in the player's face. */
    private static List<Vec3> trimStart(List<Vec3> points, Vec3 eye, double distance) {
        if (distance <= 0.0) return points;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).distanceTo(eye) >= distance) {
                List<Vec3> trimmed = new ArrayList<>(points.size() - i + 1);
                if (i > 0) trimmed.add(cut(points.get(i - 1), points.get(i), eye, distance));
                trimmed.addAll(points.subList(i, points.size()));
                return trimmed;
            }
        }
        return List.of();
    }

    /** The point between a (inside the radius) and b (outside) where the path leaves the radius, found by bisection. */
    private static Vec3 cut(Vec3 a, Vec3 b, Vec3 eye, double distance) {
        double lo = 0.0;
        double hi = 1.0;
        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) / 2.0;
            if (lerp(a, b, mid).distanceTo(eye) < distance) lo = mid;
            else hi = mid;
        }
        return lerp(a, b, hi);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }
}
