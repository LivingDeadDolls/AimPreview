package aimpreview.sim;

import aimpreview.api.Trajectory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a shot can land once vanilla's random spread is added, laid on the surface the middle shot hits.
 *
 * <p>Each axis of the spread is bounded, so the possible offsets fill a cube; its eight corners are the extremes.
 * Each extreme is flown through empty air to where it crosses the surface the middle shot hits, extended as a flat
 * plane, and the area is the outline around those crossings. Using the real landing points instead would mix up
 * surfaces: near the edge of a wall some extremes miss it and land far behind, which stretched the area far away.
 *
 * @param center  where the shot without spread lands
 * @param normal  the surface the outline lies on (the hit face, or facing back along the flight)
 * @param outline the corners of the area, in order, on that surface; one or two points when the spread only moves
 *                the shot along a line
 */
public record SpreadArea(Vec3 center, Vec3 normal, Vec3 u, Vec3 v, List<Vec3> outline) {

    /**
     * @param scale how much of the largest possible spread to cover (1 = all of it)
     * @return null when the shot has no spread or does not land
     */
    public static @Nullable SpreadArea of(Level level, Launch launch, Trajectory center, double scale, int maxTicks) {
        Spread spread = launch.spread();
        if (spread == null || !center.landed()) return null;
        Vec3 c = center.last();
        Vec3 n = normal(center);
        Vec3 u = (Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).cross(n).normalize();
        Vec3 v = n.cross(u);
        // The extremes cross the plane at about the same time as the middle shot lands; a little longer is enough.
        int ticks = Math.min(maxTicks, 2 * center.points().size() + 20);

        double m = spread.max() * scale;
        List<double[]> points = new ArrayList<>(8);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    Trajectory t = Simulator.simulateFree(level, launch.withOffset(new Vec3(sx * m, sy * m, sz * m)), ticks);
                    Vec3 at = crossing(t.points(), c, n);
                    if (at == null) continue;
                    Vec3 d = at.subtract(c);
                    points.add(new double[] {d.dot(u), d.dot(v)});
                }
            }
        }
        if (points.isEmpty()) return null;
        List<Vec3> outline = new ArrayList<>();
        for (double[] p : hull(points)) outline.add(c.add(u.scale(p[0])).add(v.scale(p[1])));
        return new SpreadArea(c, n, u, v, outline);
    }

    /** Whether p, seen along the normal, falls inside the area (or within {@code margin} blocks of it). */
    public boolean contains(Vec3 p, double margin) {
        Vec3 d = p.subtract(center);
        double px = d.dot(u);
        double py = d.dot(v);
        if (outline.size() < 3) {
            // The spread only moves the shot along a line (e.g. a bobber cast straight ahead): near that segment.
            Vec3 a3 = outline.getFirst().subtract(center);
            Vec3 b3 = outline.getLast().subtract(center);
            double ax = a3.dot(u), ay = a3.dot(v), ex = b3.dot(u) - ax, ey = b3.dot(v) - ay;
            double len2 = ex * ex + ey * ey;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * ex + (py - ay) * ey) / len2));
            return Math.hypot(px - (ax + t * ex), py - (ay + t * ey)) <= margin;
        }
        for (int i = 0; i < outline.size(); i++) {
            Vec3 a3 = outline.get(i).subtract(center);
            Vec3 b3 = outline.get((i + 1) % outline.size()).subtract(center);
            double ax = a3.dot(u), ay = a3.dot(v), bx = b3.dot(u), by = b3.dot(v);
            double ex = bx - ax, ey = by - ay;
            double cross = ex * (py - ay) - ey * (px - ax);
            if (cross / Math.hypot(ex, ey) < -margin) return false;
        }
        return true;
    }

    /** Where the path first goes through the plane (point c, normal n) from its front side, or null if it never does. */
    private static @Nullable Vec3 crossing(List<Vec3> path, Vec3 c, Vec3 n) {
        for (int i = 1; i < path.size(); i++) {
            double before = path.get(i - 1).subtract(c).dot(n);
            double after = path.get(i).subtract(c).dot(n);
            if (before > 0 && after <= 0) {
                double t = before / (before - after);
                return path.get(i - 1).add(path.get(i).subtract(path.get(i - 1)).scale(t));
            }
        }
        return null;
    }

    private static Vec3 normal(Trajectory t) {
        if (t.end() == Trajectory.End.BLOCK && t.face() != null) return Vec3.atLowerCornerOf(t.face().getUnitVec3i());
        if (t.end() == Trajectory.End.WATER) return new Vec3(0, 1, 0);
        List<Vec3> points = t.points();
        if (points.size() >= 2) {
            Vec3 back = points.get(points.size() - 2).subtract(points.getLast());
            if (back.lengthSqr() > 1.0e-8) return back.normalize();
        }
        return new Vec3(0, 1, 0);
    }

    /** Convex hull, counter-clockwise (Andrew's monotone chain). */
    private static List<double[]> hull(List<double[]> points) {
        if (points.size() < 3) return points;
        List<double[]> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.<double[]>comparingDouble(p -> p[0]).thenComparingDouble(p -> p[1]));
        List<double[]> lower = new ArrayList<>();
        for (double[] p : sorted) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.getLast(), p) <= 0) lower.removeLast();
            lower.add(p);
        }
        List<double[]> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            double[] p = sorted.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.getLast(), p) <= 0) upper.removeLast();
            upper.add(p);
        }
        lower.removeLast();
        upper.removeLast();
        lower.addAll(upper);
        return lower;
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }
}
