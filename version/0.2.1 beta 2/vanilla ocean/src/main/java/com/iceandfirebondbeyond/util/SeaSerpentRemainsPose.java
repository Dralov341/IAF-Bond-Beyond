package com.iceandfirebondbeyond.util;

/** Shared model-space joints for the renderer and harvest hitboxes. No client classes. */
public final class SeaSerpentRemainsPose {
    public static final float MODEL_ORIGIN_Y = 1.501F * 16.0F;
    public static final String[] NAMES = {"BodyUpper", "BodyLower", "Tail1", "Tail2", "Tail3", "Tail4",
            "Tail5", "Tail6", "Neck1", "Neck2", "Neck3", "Head"};
    private static final int[] PARENT = {-1, 0, 1, 2, 3, 4, 5, 6, 0, 8, 9, 10};
    private static final float[][] PIVOTS = {{0, 0, -10}, {0, -.3F, 6.7F}, {0, .4F, 5.5F},
            {0, -.1F, 5.8F}, {0, -.1F, 7.7F}, {0, -.1F, 7.9F}, {0, -.2F, 8.6F},
            {0, .7F, 11}, {0, .6F, 1.1F}, {0, -.7F, -7.3F}, {0, .3F, -7.5F}, {0, 2.4F, -2.7F}};
    private static final float[] CURL = {0, .025F, .10F, .08F, .035F, -.055F, -.08F, -.08F,
            -.12F, -.08F, .04F, .04F};
    private static final float[] RADIUS = {3.52F, 3.52F, 2.5F, 2, 2.02F, 2, 1.5F, 1.02F, 3, 2.5F, 2.5F, 2.5F};
    public final float[][] position = new float[NAMES.length][3];
    public final float[][] rotation = new float[NAMES.length][3];
    private final double[][] transforms = new double[NAMES.length][];

    /** Ground height above entity feet, in model pixels; x/z are model-space coordinates. */
    @FunctionalInterface public interface Surface { double height(double x, double z); }

    public static SeaSerpentRemainsPose create(float curl, float settling, Surface surface) {
        SeaSerpentRemainsPose pose = new SeaSerpentRemainsPose();
        for (int i = 0; i < NAMES.length; i++) {
            System.arraycopy(PIVOTS[i], 0, pose.position[i], 0, 3);
            pose.rotation[i][0] = CURL[i] * curl;
        }
        pose.position[0][1] = MODEL_ORIGIN_Y - RADIUS[0];
        pose.rotation[0][2] = (float) (Math.PI / 2);
        pose.rebuild();
        if (surface != null && settling > 0) {
            // Anchor the heavy chest, then let connected joints follow the local
            // collision surface. Water is excluded by the caller's ray casts.
            double[] chest = pose.point(0, 0, 0, 3.5);
            double ground = surface.height(chest[0], chest[2]);
            if (Double.isFinite(ground)) {
                pose.position[0][1] -= (float) clamp(ground, -12, 24) * settling;
                pose.rebuild();
            }
            for (int bone = 0; bone < NAMES.length; bone++) {
                int child = bone == 0 ? 1 : bone < 7 ? bone + 1 : bone == 7 ? -1 : bone < 11 ? bone + 1 : -1;
                double[] end = child >= 0 ? new double[]{PIVOTS[child][0], PIVOTS[child][1], PIVOTS[child][2]}
                        : new double[]{0, 0, bone == 7 ? 11 : -10};
                double radius = child >= 0 ? RADIUS[child] : bone == 7 ? .8 : 1.5;
                float fitted = 0;
                // A small numerical IK solve keeps every original pivot connected;
                // unlike moving individual bones up/down it never opens neck gaps.
                for (int iteration = 0; iteration < 4; iteration++) {
                    double[] tip = pose.point(bone, end[0], end[1], end[2]);
                    double floor = surface.height(tip[0], tip[2]);
                    if (!Double.isFinite(floor)) break;
                    double wanted = MODEL_ORIGIN_Y - floor - radius;
                    double[] start = pose.point(bone, 0, 0, 0);
                    // A segment crossing a ledge must rest on its lip. Sampling
                    // only the far joint would send the middle through the step.
                    for (double fraction : new double[]{.25, .5, .75}) {
                        double[] middle = pose.point(bone, end[0] * fraction, end[1] * fraction, end[2] * fraction);
                        double support = surface.height(middle[0], middle[2]);
                        if (Double.isFinite(support)) {
                            double clearance = RADIUS[bone];
                            double allowed = MODEL_ORIGIN_Y - support - clearance;
                            wanted = Math.min(wanted, start[1] + (allowed - start[1]) / fraction);
                        }
                    }
                    pose.rotation[bone][1] = fitted + .01F;
                    pose.rebuild();
                    double derivative = (pose.point(bone, end[0], end[1], end[2])[1] - tip[1]) / .01;
                    if (Math.abs(derivative) < .1) break;
                    fitted = (float) clamp(fitted + (wanted - tip[1]) / derivative, -.65, .65);
                    pose.rotation[bone][1] = fitted;
                    pose.rebuild();
                }
                pose.rotation[bone][1] = fitted * settling;
                pose.rebuild();
            }
        }
        return pose;
    }

    public double[] point(int bone, double x, double y, double z) {
        double[] m = transforms[bone];
        return new double[]{m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11]};
    }

    public double[] harvestPoint(int part) {
        int bone = part / 2;
        double[] lengths = {6.7, 5.5, 5.8, 7.7, 7.9, 8.6, 11, 12, -7.3, -7.5, -2.7, -11};
        return point(bone, 0, 0, lengths[bone] * (part % 2 == 0 ? .15 : .7));
    }

    public static SeaSerpentRemainsPose blend(SeaSerpentRemainsPose from, SeaSerpentRemainsPose to, float weight) {
        SeaSerpentRemainsPose pose = new SeaSerpentRemainsPose();
        for (int i = 0; i < NAMES.length; i++) for (int axis = 0; axis < 3; axis++) {
            pose.position[i][axis] = from.position[i][axis] + weight * (to.position[i][axis] - from.position[i][axis]);
            pose.rotation[i][axis] = from.rotation[i][axis] + weight * (to.rotation[i][axis] - from.rotation[i][axis]);
        }
        pose.rebuild();
        return pose;
    }

    private void rebuild() {
        for (int i = 0; i < NAMES.length; i++) {
            double[] local = matrix(position[i], rotation[i]);
            transforms[i] = PARENT[i] < 0 ? local : multiply(transforms[PARENT[i]], local);
        }
    }
    private static double[] matrix(float[] p, float[] r) {
        double cx = Math.cos(r[0]), sx = Math.sin(r[0]), cy = Math.cos(r[1]), sy = Math.sin(r[1]);
        double cz = Math.cos(r[2]), sz = Math.sin(r[2]);
        return new double[]{cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx, p[0],
                sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx, p[1],
                -sy, cy * sx, cy * cx, p[2]};
    }
    private static double[] multiply(double[] a, double[] b) {
        double[] out = new double[12];
        for (int row = 0; row < 3; row++) for (int column = 0; column < 4; column++) {
            for (int k = 0; k < 3; k++) out[row * 4 + column] += a[row * 4 + k] * b[k * 4 + column];
            if (column == 3) out[row * 4 + column] += a[row * 4 + 3];
        }
        return out;
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private SeaSerpentRemainsPose() {}
}
