package com.iceandfirebondbeyond.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Carves eye openings without repacking or stretching the original box UVs. */
final class ArmorCutoutMesh {
    record Bounds(float x0, float y0, float z0, float x1, float y1, float z1) {
        boolean contains(float x, float y, float z) {
            return x > x0 && x < x1 && y > y0 && y < y1 && z > z0 && z < z1;
        }
    }

    record Box(int u, int v, float x, float y, float z, float width, float height, float depth,
               float textureWidth, float textureHeight, float textureDepth) {
        Box(int u, int v, float x, float y, float z, float width, float height, float depth) {
            this(u, v, x, y, z, width, height, depth, width, height, depth);
        }
    }
    private record Vertex(float x, float y, float z, float u, float v) {}
    private record Quad(Vertex[] vertices, int nx, int ny, int nz) {}
    private final List<Quad> quads;

    private ArmorCutoutMesh(List<Quad> quads) {
        this.quads = List.copyOf(quads);
    }

    static ArmorCutoutMesh bake(Bounds[] openings, Box... boxes) {
        List<Quad> quads = new ArrayList<>();
        for (Box box : boxes) bakeBox(quads, box, openings, new Box[0]);
        return new ArmorCutoutMesh(quads);
    }

    /** Native dragon horns grow along +Z; the existing armor UV strip runs up -Y.
     * Each segment uses its portion of that strip, so the tip decoration stays
     * at the tip rather than repeating on every joint. No atlas changes needed. */
    static ArmorCutoutMesh horn(Box segment, Box texture, float start, float end) {
        List<Quad> result = new ArrayList<>();
        for (Quad q : bake(new Bounds[0], segment).quads) {
            int sourceSide = q.nx < 0 ? 0 : q.nx > 0 ? 1 : q.ny < 0 ? 4 : q.ny > 0 ? 5 : q.nz < 0 ? 3 : 2;
            Vertex[] vertices = new Vertex[4];
            for (int i = 0; i < 4; i++) {
                Vertex p = q.vertices[i];
                float x = (p.x * 16 - segment.x) / segment.width * texture.width;
                float y = (p.y * 16 - segment.y) / segment.height * texture.depth;
                float z = start + (end - start) * (p.z * 16 - segment.z) / segment.depth;
                float[] uv = uv(texture, sourceSide, x, (1 - z) * texture.height, y);
                vertices[i] = new Vertex(p.x, p.y, p.z, uv[0], uv[1]);
            }
            result.add(new Quad(vertices, q.nx, q.ny, q.nz));
        }
        return new ArmorCutoutMesh(result);
    }

    /** Joins overlapping helmet plates while keeping the outer faces' box UVs. */
    static ArmorCutoutMesh bakeUnion(Bounds[] openings, Box... boxes) {
        List<Quad> quads = new ArrayList<>();
        for (int i = 0; i < boxes.length; i++) {
            List<Quad> faces = new ArrayList<>();
            bakeBox(faces, boxes[i], openings, boxes);
            for (Quad face : faces) {
                if (exposed(face, i, openings, boxes)) quads.add(face);
            }
        }
        return new ArmorCutoutMesh(quads);
    }

    private static boolean exposed(Quad face, int source, Bounds[] openings, Box[] boxes) {
        float x = 0, y = 0, z = 0;
        for (Vertex v : face.vertices) {
            x += v.x * 4; y += v.y * 4; z += v.z * 4;
        }
        // Vertex coordinates are in blocks; the four-vertex mean above is now
        // in model pixels. Probe both sides of this already partitioned face.
        float dx = face.nx * 0.0001F, dy = face.ny * 0.0001F, dz = face.nz * 0.0001F;
        for (int i = 0; i < boxes.length; i++) {
            if (i == source) continue;
            if (occupied(boxes[i], openings, x + dx, y + dy, z + dz)) return false;
            // On coincident outer faces, the later plate supplies the texture.
            if (i > source && occupied(boxes[i], openings, x - dx, y - dy, z - dz)) return false;
        }
        return true;
    }

    private static boolean occupied(Box box, Bounds[] openings, float x, float y, float z) {
        if (x <= box.x || x >= box.x + box.width || y <= box.y || y >= box.y + box.height
                || z <= box.z || z >= box.z + box.depth) return false;
        for (Bounds opening : openings) if (opening.contains(x, y, z)) return false;
        return true;
    }

    private static void bakeBox(List<Quad> out, Box box, Bounds[] openings, Box[] neighbors) {
        float[] xs = split(box.x, box.x + box.width, openings, neighbors, 0);
        float[] ys = split(box.y, box.y + box.height, openings, neighbors, 1);
        float[] zs = split(box.z, box.z + box.depth, openings, neighbors, 2);
        int nx = xs.length - 1, ny = ys.length - 1, nz = zs.length - 1;
        boolean[][][] solid = new boolean[nx][ny][nz];
        for (int x = 0; x < nx; x++) for (int y = 0; y < ny; y++) for (int z = 0; z < nz; z++) {
            solid[x][y][z] = true;
            for (Bounds hole : openings) {
                if (hole.contains((xs[x] + xs[x + 1]) / 2, (ys[y] + ys[y + 1]) / 2,
                        (zs[z] + zs[z + 1]) / 2)) {
                    solid[x][y][z] = false;
                    break;
                }
            }
        }
        for (int x = 0; x < nx; x++) for (int y = 0; y < ny; y++) for (int z = 0; z < nz; z++) {
            if (!solid[x][y][z]) continue;
            float x0 = xs[x], x1 = xs[x + 1], y0 = ys[y], y1 = ys[y + 1], z0 = zs[z], z1 = zs[z + 1];
            // Emit only exposed faces, including the solid rims of the openings.
            if (x == 0 || !solid[x - 1][y][z]) face(out, box, 0,
                    x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
            if (x == nx - 1 || !solid[x + 1][y][z]) face(out, box, 1,
                    x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
            if (y == 0 || !solid[x][y - 1][z]) face(out, box, 2,
                    x1,y0,z1, x0,y0,z1, x0,y0,z0, x1,y0,z0);
            if (y == ny - 1 || !solid[x][y + 1][z]) face(out, box, 3,
                    x1,y1,z0, x0,y1,z0, x0,y1,z1, x1,y1,z1);
            if (z == 0 || !solid[x][y][z - 1]) face(out, box, 4,
                    x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
            if (z == nz - 1 || !solid[x][y][z + 1]) face(out, box, 5,
                    x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
        }
    }

    private static float[] split(float min, float max, Bounds[] cuts, Box[] neighbors, int axis) {
        float[] values = new float[2 + cuts.length * 2 + neighbors.length * 2];
        values[0] = min;
        values[1] = max;
        int count = 2;
        for (Bounds cut : cuts) {
            float a = axis == 0 ? cut.x0 : axis == 1 ? cut.y0 : cut.z0;
            float b = axis == 0 ? cut.x1 : axis == 1 ? cut.y1 : cut.z1;
            if (a > min && a < max) values[count++] = a;
            if (b > min && b < max) values[count++] = b;
        }
        for (Box box : neighbors) {
            float a = axis == 0 ? box.x : axis == 1 ? box.y : box.z;
            float b = a + (axis == 0 ? box.width : axis == 1 ? box.height : box.depth);
            if (a > min && a < max) values[count++] = a;
            if (b > min && b < max) values[count++] = b;
        }
        Arrays.sort(values, 0, count);
        int unique = 1;
        for (int i = 1; i < count; i++) {
            if (values[i] != values[unique - 1]) values[unique++] = values[i];
        }
        return Arrays.copyOf(values, unique);
    }

    private static void face(List<Quad> out, Box b, int side, float... xyz) {
        Vertex[] vertices = new Vertex[4];
        for (int i = 0; i < 4; i++) {
            float x = xyz[3 * i], y = xyz[3 * i + 1], z = xyz[3 * i + 2];
            float dx = x - b.x, dy = y - b.y, dz = z - b.z;
            if (b.textureWidth != b.width) dx *= b.textureWidth / b.width;
            if (b.textureHeight != b.height) dy *= b.textureHeight / b.height;
            if (b.textureDepth != b.depth) dz *= b.textureDepth / b.depth;
            // ModelPart.Cube's six box-UV faces, evaluated in the UNCLIPPED box.
            // A fragment therefore samples the same pixels as its original face.
            float[] uv = uv(b, side, dx, dy, dz);
            vertices[i] = new Vertex(x / 16, y / 16, z / 16, uv[0], uv[1]);
        }
        out.add(new Quad(vertices, side == 0 ? -1 : side == 1 ? 1 : 0,
                side == 2 ? -1 : side == 3 ? 1 : 0, side == 4 ? -1 : side == 5 ? 1 : 0));
    }

    private static float[] uv(Box b, int side, float dx, float dy, float dz) {
        float u = switch (side) {
            case 0 -> b.u + b.textureDepth - dz;
            case 1 -> b.u + b.textureDepth + b.textureWidth + dz;
            case 2, 4 -> b.u + b.textureDepth + dx;
            case 3 -> b.u + b.textureDepth + b.textureWidth + dx;
            default -> b.u + 2 * b.textureDepth + 2 * b.textureWidth - dx;
        };
        float v = side == 2 || side == 3 ? b.v + b.textureDepth - dz : b.v + b.textureDepth + dy;
        return new float[]{u / 128, v / 128};
    }

    void render(PoseStack stack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = stack.last();
        for (Quad quad : quads) {
            for (Vertex vertex : quad.vertices) {
                consumer.vertex(pose.pose(), vertex.x, vertex.y, vertex.z)
                        .color(red, green, blue, alpha).uv(vertex.u, vertex.v)
                        .overlayCoords(overlay).uv2(light)
                        .normal(pose.normal(), quad.nx, quad.ny, quad.nz).endVertex();
            }
        }
    }
}
