package com.iceandfirebondbeyond.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** An aimed mouth box, optionally joined to the complete posed BodyUpper/neck chain. */
public final class SeaSerpentBiteVolume {
    private final Vec3 mouth;
    private final Vec3 right;
    private final Vec3 up;
    private final Vec3 forward;
    private final AABB localBox;
    private AABB bounds;
    private final Vec3[] worldCorners;
    private List<SeaSerpentBiteVolume> neckVolumes = List.of();

    public SeaSerpentBiteVolume(Vec3[] anchors, Vec3 look, float yaw, double reach,
            double behindMouth, double width, double height) {
        this(anchors[anchors.length - 1], look, yaw, reach, behindMouth, width, height);
        List<SeaSerpentBiteVolume> neck = new ArrayList<>(anchors.length - 1);
        for (int i = 1; i < anchors.length; i++) {
            Vec3 delta = anchors[i].subtract(anchors[i - 1]);
            double length = delta.length();
            if (length < 1.0E-6D) continue;
            // Each piece follows its own posed bone direction, including a
            // bent/upward neck. Extending a single mouth ray backwards misses it.
            float boneYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
            SeaSerpentBiteVolume segment = new SeaSerpentBiteVolume(anchors[i - 1],
                    delta, boneYaw, length, 0.0D, width, height);
            neck.add(segment);
            AABB box = segment.bounds;
            bounds = new AABB(Math.min(bounds.minX, box.minX), Math.min(bounds.minY, box.minY),
                    Math.min(bounds.minZ, box.minZ), Math.max(bounds.maxX, box.maxX),
                    Math.max(bounds.maxY, box.maxY), Math.max(bounds.maxZ, box.maxZ));
        }
        neckVolumes = List.copyOf(neck);
    }

    public SeaSerpentBiteVolume(Vec3 mouth, Vec3 look, float yaw, double reach,
            double behindMouth, double width, double height) {
        this.mouth = mouth;
        this.forward = look.normalize();
        // Yaw keeps the cross-section stable even when aiming straight up/down.
        double radians = Math.toRadians(yaw);
        Vec3 lateral = new Vec3(Math.cos(radians), 0.0D, Math.sin(radians));
        // Minecraft's float/trig-table look vector may differ slightly from
        // Math.sin/cos. Keep the local/world transforms truly orthonormal.
        this.right = lateral.subtract(forward.scale(lateral.dot(forward))).normalize();
        this.up = forward.cross(right).normalize();
        this.localBox = new AABB(-width * 0.5D, -height * 0.5D, -behindMouth,
                width * 0.5D, height * 0.5D, reach);
        this.worldCorners = corners(localBox);
        for (int i = 0; i < worldCorners.length; i++) {
            worldCorners[i] = toWorld(worldCorners[i]);
        }
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (Vec3 point : worldCorners) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB bounds() {
        return bounds;
    }

    public boolean contains(Vec3 point) {
        if (localBox.contains(toLocal(point))) return true;
        for (SeaSerpentBiteVolume segment : neckVolumes) {
            if (segment.contains(point)) return true;
        }
        return false;
    }

    @Nullable
    public Vec3 contact(AABB target) {
        Vec3 nearest = contactSingle(target);
        double distance = nearest == null ? Double.POSITIVE_INFINITY : mouth.distanceToSqr(nearest);
        for (SeaSerpentBiteVolume segment : neckVolumes) {
            if (!segment.bounds.intersects(target)) continue;
            Vec3 candidate = segment.contactSingle(target);
            if (candidate != null && mouth.distanceToSqr(candidate) < distance) {
                nearest = candidate;
                distance = mouth.distanceToSqr(candidate);
            }
        }
        return nearest;
    }

    /**
     * Return a point in BOTH the real target box and the bite volume. Testing
     * only the enclosing world AABB would bite far outside diagonal/upward aim.
     * The centre line handles the common case; clipping the 12 edges of each
     * box also handles a wide/tall side hit or either box enclosing the other.
     */
    @Nullable
    private Vec3 contactSingle(AABB target) {
        Vec3 direct = clip(target, mouth, toWorld(new Vec3(0.0D, 0.0D, localBox.maxZ)));
        if (direct != null) return direct;

        Vec3 nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        Vec3[] targetCorners = corners(target);
        for (int i = 0; i < targetCorners.length; i++) {
            targetCorners[i] = toLocal(targetCorners[i]);
        }
        for (int i = 0; i < 8; i++) {
            for (int axis = 1; axis <= 4; axis <<= 1) {
                if ((i & axis) != 0) continue;
                int end = i | axis;
                Vec3 local = clip(localBox, targetCorners[i], targetCorners[end]);
                if (local != null) {
                    Vec3 point = toWorld(local);
                    double candidate = mouth.distanceToSqr(point);
                    if (candidate < distance) {
                        nearest = point;
                        distance = candidate;
                    }
                }
                Vec3 point = clip(target, worldCorners[i], worldCorners[end]);
                if (point != null) {
                    double candidate = mouth.distanceToSqr(point);
                    if (candidate < distance) {
                        nearest = point;
                        distance = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    private Vec3 toLocal(Vec3 point) {
        Vec3 offset = point.subtract(mouth);
        return new Vec3(offset.dot(right), offset.dot(up), offset.dot(forward));
    }

    private Vec3 toWorld(Vec3 point) {
        return mouth.add(right.scale(point.x)).add(up.scale(point.y)).add(forward.scale(point.z));
    }

    @Nullable
    private static Vec3 clip(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) ? from : box.clip(from, to).orElse(null);
    }

    private static Vec3[] corners(AABB box) {
        Vec3[] result = new Vec3[8];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Vec3((i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY,
                    (i & 4) == 0 ? box.minZ : box.maxZ);
        }
        return result;
    }
}
