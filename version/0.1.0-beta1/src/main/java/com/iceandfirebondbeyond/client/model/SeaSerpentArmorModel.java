// Blockbench mesh/UVs bound to the live skeleton, with carved eye openings.
package com.iceandfirebondbeyond.client.model;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.iceandfire.client.model.util.EnumSeaSerpentAnimations;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SeaSerpentArmorModel<T extends EntitySeaSerpent> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("iceandfire_bond_beyond", "sea_serpent_armor"), "main");
    // One fitting transform for the whole original mesh; units are model pixels.
    private static final float FIT_Y = 0.2F;
    private static final float FIT_Z = 3.0F;
    private static final String[] PARTS = {
            "BodyUpper", "BodyLower", "Tail1", "Tail2", "Tail3", "Tail4", "Tail5", "Tail6",
            "Neck1", "Neck2", "Neck3", "Head", "HeadFront", "Jaw", "BackSaddle", "FrontSaddle"};
    private static final int[] PARENTS = {-1, 0, 1, 2, 3, 4, 5, 6, 0, 8, 9, 10, 11, 11, 1, 0};
    private static final int BONE_COUNT = 14;
    private static final int HEAD = 11;
    private static final int HEAD_FRONT = 12;
    // The upper muzzle and snout share x = +/-2 side planes. A 1% width
    // clearance separates those layers without moving the nose tip or its UVs.
    private static final float MUZZLE_WIDTH_SCALE = 1.01F;
    // Tail plates fit their own native bone instead of retaining the export's
    // cumulative rest-pose bend, which lifted the last plate off the animal.
    private static final float[][] TAIL_OFFSETS = {
            {0, 0, 0}, {0, 0.4F, 0.9F}, {-0.02F, 0.7F, 0.9F},
            {0, 0.7F, 0.9F}, {0, 1.1F, 1}, {-0.02F, 1.1F, 1}};
    private static final ArmorCutoutMesh HEAD_PLATES = createHeadPlates();
    private static final ArmorCutoutMesh SNOUT_PLATES = createSnoutPlates();
    private static final ArmorCutoutMesh MUZZLE_PLATES = createMuzzlePlates();
    private static final ArmorCutoutMesh CHIN_PLATES = createChinPlates();
    private static final String[] HORNS = {"HornR", "HornR2", "HornR3", "HornL", "HornL2", "HornL3",
            "HornR4", "HornR5", "HornL4", "HornL5"};
    private static final int[] HORN_PARENTS = {-1, 0, 1, -1, 3, 4, -1, 6, -1, 8};
    private static final float HORN_SCALE = 1.1F;
    private static final ArmorCutoutMesh[] HORN_MESHES = createHornMeshes();
    private final ModelPart[] hornParts = new ModelPart[HORNS.length];
    private final ModelPart[] pieces = new ModelPart[PARTS.length];
    private final Matrix4f[] originalPose = new Matrix4f[PARTS.length];
    private final Matrix4f[] binding = new Matrix4f[PARTS.length];
    private final Matrix4f[] liveBones = new Matrix4f[BONE_COUNT];
    private final Matrix4f transform = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private boolean bound;

    public SeaSerpentArmorModel(ModelPart root) {
        for (int i = 0; i < PARTS.length; i++) {
            ModelPart piece = pieces[i] = root.getChild(PARTS[i]);
            originalPose[i] = i == 0 ? new Matrix4f() : new Matrix4f(originalPose[PARENTS[i]])
                    .translate(piece.x / 16.0F, piece.y / 16.0F, piece.z / 16.0F)
                    .rotateZYX(piece.zRot, piece.yRot, piece.xRot);
            binding[i] = new Matrix4f();
            // Applied exactly once by the matrix, not again by ModelPart.render.
            piece.setPos(0, 0, 0);
            piece.setRotation(0, 0, 0);
        }
        for (int i = 0; i < BONE_COUNT; i++) liveBones[i] = new Matrix4f();
        for (int i = 0; i < HORNS.length; i++) {
            ModelPart parent = HORN_PARENTS[i] < 0 ? pieces[HEAD].getChild("Detail") : hornParts[HORN_PARENTS[i]];
            hornParts[i] = parent.getChild(HORNS[i]);
        }
    }

    public void bindTo(TabulaModel parent) {
        if (!bound) {
            Matrix4f[] restBones = new Matrix4f[BONE_COUNT];
            TabulaModel rest = EnumSeaSerpentAnimations.T_POSE.seaserpent_model;
            for (int i = 0; i < BONE_COUNT; i++) {
                restBones[i] = new Matrix4f();
                copyGlobalBone(restBones[i], rest.getCube(PARTS[i]),
                        i == 0 ? null : restBones[PARENTS[i]]);
            }
            Matrix4f fitting = new Matrix4f(restBones[0]).translate(0, FIT_Y / 16, FIT_Z / 16);
            for (int i = 0; i < PARTS.length; i++) {
                // Both saddle halves bind to BodyUpper as one rigid seat.
                int bone = i < BONE_COUNT ? i : 0;
                binding[i].set(restBones[bone]).invert().mul(fitting).mul(originalPose[i]);
                if (i >= 2 && i <= 7) {
                    float[] offset = TAIL_OFFSETS[i - 2];
                    binding[i].identity().translate(offset[0] / 16, offset[1] / 16, offset[2] / 16);
                }
            }
            bound = true;
        }
        for (int i = 0; i < BONE_COUNT; i++) {
            copyGlobalBone(liveBones[i], parent.getCube(PARTS[i]),
                    i == 0 ? null : liveBones[PARENTS[i]]);
        }
    }

    private static void copyGlobalBone(Matrix4f matrix, AdvancedModelBox bone, Matrix4f parent) {
        if (parent == null) matrix.identity(); else matrix.set(parent);
        matrix.translate(bone.rotationPointX / 16, bone.rotationPointY / 16, bone.rotationPointZ / 16)
                .rotateZYX(bone.rotateAngleZ, bone.rotateAngleY, bone.rotateAngleX);
    }

    @Override
    public void setupAnim(EntitySeaSerpent entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        // Read the completed native pose in bindTo; never animate a second skeleton.
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        if (!bound) return;
        for (int i = 0; i < PARTS.length; i++) {
            transform.set(liveBones[i < BONE_COUNT ? i : 0]).mul(binding[i]);
            stack.pushPose();
            stack.last().pose().mul(transform);
            stack.last().normal().mul(normal.set(transform));
            pieces[i].render(stack, consumer, light, overlay, red, green, blue, alpha);
            if (i == HEAD) {
                HEAD_PLATES.render(stack, consumer, light, overlay, red, green, blue, alpha);
                stack.pushPose();
                pieces[i].getChild("ChinGuard").translateAndRotate(stack);
                CHIN_PLATES.render(stack, consumer, light, overlay, red, green, blue, alpha);
                stack.popPose();
                for (int h = 0; h < HORNS.length; h++) {
                    if (HORN_PARENTS[h] < 0) renderHorn(h, stack, consumer, light, overlay, red, green, blue, alpha);
                }
            }
            if (i == HEAD_FRONT) {
                SNOUT_PLATES.render(stack, consumer, light, overlay, red, green, blue, alpha);
                stack.pushPose();
                pieces[i].getChild("MuzzleGuard").translateAndRotate(stack);
                stack.scale(MUZZLE_WIDTH_SCALE, 1, 1);
                MUZZLE_PLATES.render(stack, consumer, light, overlay, red, green, blue, alpha);
                stack.popPose();
            }
            stack.popPose();
        }
    }

    private void renderHorn(int index, PoseStack stack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        stack.pushPose();
        hornParts[index].translateAndRotate(stack);
        if (HORN_PARENTS[index] < 0) stack.scale(HORN_SCALE, HORN_SCALE, HORN_SCALE);
        HORN_MESHES[index].render(stack, consumer, light, overlay, red, green, blue, alpha);
        for (int child = index + 1; child < HORNS.length; child++) {
            if (HORN_PARENTS[child] == index) renderHorn(child, stack, consumer, light, overlay, red, green, blue, alpha);
        }
        stack.popPose();
    }

    private static ArmorCutoutMesh createHeadPlates() {
        // Native eyes wrap around both front corners of Head. Keep the brow,
        // temples, cheek rims and central bridge; open BOTH the side and front.
        // The crown brackets join the helmet here so their touching faces do
        // not fight for depth when the temples are narrowed.
        return ArmorCutoutMesh.bakeUnion(skullOpenings(),
                new ArmorCutoutMesh.Box(72, 110, -4, -0.5F, -4.1F, 2, 4, 4),
                new ArmorCutoutMesh.Box(104, 109, -3, -0.5F, -4.1F, 6, 3, 4),
                new ArmorCutoutMesh.Box(110, 78, 2, -0.5F, -4.1F, 2, 4, 4),
                // Original bracket UVs; moved onto the skull. The two forward
                // blocks marked X in the reference are deliberately omitted.
                new ArmorCutoutMesh.Box(112, 49, -3.2F, -1.5F, -4.1F, 2, 1, 4),
                new ArmorCutoutMesh.Box(92, 112, 1.2F, -1.5F, -4.1F, 2, 1, 4),
                new ArmorCutoutMesh.Box(116, 12, -3.1F, -0.5F, -4.1F, 1, 2, 4),
                new ArmorCutoutMesh.Box(116, 40, 2.1F, -0.5F, -4.1F, 1, 2, 4),
                // Close the circled brow/snout seam above the eye. Reuse the
                // crown's complete UV patch; the original nose cap is untouched.
                new ArmorCutoutMesh.Box(104, 109, -3, -1.5F, -6.15F, 6, 2.25F, 2.5F, 6, 3, 4));
    }

    private static ArmorCutoutMesh.Bounds[] skullOpenings() {
        return new ArmorCutoutMesh.Bounds[]{
                new ArmorCutoutMesh.Bounds(-5, 0.85F, -5, -0.75F, 2.95F, -2),
                new ArmorCutoutMesh.Bounds(0.75F, 0.85F, -5, 5, 2.95F, -2),
                // Bring the temples closer to the skull, without rescaling UVs.
                new ArmorCutoutMesh.Bounds(-10, -10, -20, -3, 10, 10),
                new ArmorCutoutMesh.Bounds(3, -10, -20, 10, 10, 10),
                // Shorten and thin the ledge below the eye instead of letting it
                // project forward as a separate shelf.
                new ArmorCutoutMesh.Bounds(-5, 2.95F, -5, -0.75F, 5, -3.25F),
                new ArmorCutoutMesh.Bounds(0.75F, 2.95F, -5, 5, 5, -3.25F),
                new ArmorCutoutMesh.Bounds(-5, 3.2F, -5, 5, 5, -2)};
    }

    private static ArmorCutoutMesh createSnoutPlates() {
        // Trim only the rear cheek straps that otherwise cover the eye openings.
        // The nose tip, forward shell and their texture coordinates are untouched.
        return ArmorCutoutMesh.bake(new ArmorCutoutMesh.Bounds[]{
                new ArmorCutoutMesh.Bounds(-5, -10, -2, -0.75F, 10, 5),
                new ArmorCutoutMesh.Bounds(0.75F, -10, -2, 5, 10, 5),
                // Keep the side guards as thin bands against the muzzle.
                new ArmorCutoutMesh.Bounds(-10, -10, -20, -2.8F, 10, 10),
                new ArmorCutoutMesh.Bounds(2.8F, -10, -20, 10, 10, 10),
                new ArmorCutoutMesh.Bounds(-10, 2, -20, 10, 10, 10)},
                new ArmorCutoutMesh.Box(0, 79, -2, -2.2F, -7, 4, 3, 8),
                new ArmorCutoutMesh.Box(104, 87, -3.5F, 0.8F, -7, 1, 3, 8),
                new ArmorCutoutMesh.Box(104, 98, 2.5F, 0.8F, -7, 1, 3, 8));
    }

    private static ArmorCutoutMesh createMuzzlePlates() {
        // Leave room for the rear cheek edge to swing with HeadFront when the
        // native breath animation lifts the snout. The nose cap is ahead of this.
        return ArmorCutoutMesh.bakeUnion(rearCheekOpenings(-2.2F),
                new ArmorCutoutMesh.Box(62, 102, -2, -3.4F, -6.6F, 4, 1, 7),
                new ArmorCutoutMesh.Box(100, 69, -2, -2.9F, -7.3F, 4, 3, 1),
                new ArmorCutoutMesh.Box(110, 69, -2.6F, -2.9F, -4.6F, 1, 2, 7),
                new ArmorCutoutMesh.Box(20, 111, 1.6F, -2.9F, -6.6F, 1, 3, 5),
                new ArmorCutoutMesh.Box(112, 32, -2.6F, -2.9F, -6.6F, 1, 3, 5));
    }

    private static ArmorCutoutMesh.Bounds[] rearCheekOpenings(float front) {
        return new ArmorCutoutMesh.Bounds[]{
                new ArmorCutoutMesh.Bounds(-5, -10, front, -0.75F, 10, 5),
                new ArmorCutoutMesh.Bounds(0.75F, -10, front, 5, 10, 5)};
    }

    private static ArmorCutoutMesh createChinPlates() {
        // A short overlapping hinge cover closes the second circled seam.
        // It stays behind the moving jaw, below the eye, and joins the existing
        // throat plate as a solid mesh instead of stacking coplanar faces.
        return ArmorCutoutMesh.bakeUnion(new ArmorCutoutMesh.Bounds[0],
                new ArmorCutoutMesh.Box(64, 35, -3, 1.1F, 0.4F, 6, 1, 7),
                // Ends at Head-local Z=-3.25, where the existing lower eye rim
                // starts: their side faces must not overlap at X=+/-3.
                new ArmorCutoutMesh.Box(64, 35, -3, 0.5F, -0.4F, 6, 1.6F, 0.95F, 6, 1, 7));
    }

    private static ArmorCutoutMesh[] createHornMeshes() {
        // Exact ten segment boxes from Ice and Fire beta-5's
        // firedragon_base_male.tbl: HornR/L, HornR/L2, HornR/L3, HornR/L4, HornR/L5.
        // Only the four roots are fitted to this helmet; child bends are native.
        ArmorCutoutMesh.Box right = new ArmorCutoutMesh.Box(84, 112, -1, -10, -1, 2, 10, 2);
        ArmorCutoutMesh.Box left = new ArmorCutoutMesh.Box(32, 111, -1, -10, -1, 2, 10, 2);
        ArmorCutoutMesh.Box smallRight = new ArmorCutoutMesh.Box(20, 90, -0.5F, -6, -0.5F, 1, 6, 1);
        ArmorCutoutMesh.Box smallLeft = new ArmorCutoutMesh.Box(60, 24, -0.5F, -6, -0.5F, 1, 6, 1);
        return new ArmorCutoutMesh[]{
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.5F, -0.5F, 0, 1, 2, 4), right, 0, 0.48F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, 0.01F, -0.8F, 0, 1, 1, 3), right, 0.4F, 0.76F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.51F, -0.8F, 0, 1, 1, 2), right, 0.76F, 1),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.5F, -0.5F, 0, 1, 2, 4), left, 0, 0.48F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.01F, -0.8F, 0, 1, 1, 3), left, 0.4F, 0.76F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.51F, -0.8F, 0, 1, 1, 2), left, 0.76F, 1),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.5F, 0, 0, 1, 1, 4), smallRight, 0, 0.72F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.47F, -0.8F, 0, 1, 1, 2), smallRight, 0.64F, 1),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.5F, 0, 0, 1, 1, 4), smallLeft, 0, 0.72F),
                ArmorCutoutMesh.horn(new ArmorCutoutMesh.Box(0, 0, -0.53F, -0.8F, 0, 1, 1, 2), smallLeft, 0.64F, 1)};
    }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition BodyUpper = partdefinition.addOrReplaceChild("BodyUpper", CubeListBuilder.create().texOffs(90, 20).addBox(3.0F, -2.0F, -3.0F, 2.0F, 6.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(0, 58).addBox(-4.0F, -3.0F, -3.0F, 8.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(92, 74).addBox(-5.0F, -2.0F, -3.0F, 2.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition BodyLower = partdefinition.addOrReplaceChild("BodyLower", CubeListBuilder.create().texOffs(24, 86).addBox(-5.0F, -2.0F, 0.0F, 2.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(88, 0).addBox(3.0F, -2.0F, 0.0F, 2.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(32, 32).addBox(-4.0F, -3.0F, 0.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0367F, 0.0F, 0.0F));

		PartDefinition Tail1 = partdefinition.addOrReplaceChild("Tail1", CubeListBuilder.create().texOffs(88, 49).addBox(-3.0F, -3.0F, 1.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(108, 0).addBox(2.0F, -2.0F, 1.0F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(108, 20).addBox(-4.0F, -2.0F, 1.0F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.0349F, 0.0F, 0.0F));

		PartDefinition Tail2 = partdefinition.addOrReplaceChild("Tail2", CubeListBuilder.create().texOffs(44, 89).addBox(-3.5F, -2.0F, 0.0F, 2.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 89).addBox(1.5F, -2.0F, 0.0F, 2.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 24).addBox(-2.5F, -3.0F, 0.0F, 5.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.0175F, 0.0F, 0.0F));

		PartDefinition Tail3 = partdefinition.addOrReplaceChild("Tail3", CubeListBuilder.create().texOffs(60, 0).addBox(-2.5F, -3.0F, 0.0F, 5.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(26, 73).addBox(-3.5F, -2.0F, 0.0F, 2.0F, 4.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(48, 76).addBox(1.5F, -2.0F, 0.0F, 2.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, -0.0175F, 0.0F, 0.0F));

		PartDefinition Tail4 = partdefinition.addOrReplaceChild("Tail4", CubeListBuilder.create().texOffs(70, 76).addBox(1.5F, -2.0F, 0.0F, 2.0F, 4.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(78, 61).addBox(-3.5F, -2.0F, 0.0F, 2.0F, 4.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(60, 12).addBox(-2.5F, -3.0F, 0.0F, 5.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, -0.0175F, 0.0F, 0.0F));

		PartDefinition Tail5 = partdefinition.addOrReplaceChild("Tail5", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -2.0F, 0.0F, 2.0F, 4.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(32, 16).addBox(1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, -0.0175F, 0.0F, 0.0F));

		PartDefinition Tail6 = partdefinition.addOrReplaceChild("Tail6", CubeListBuilder.create().texOffs(0, 43).addBox(0.5F, -2.0F, 0.0F, 2.0F, 3.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(28, 43).addBox(-2.5F, -2.0F, 0.0F, 2.0F, 3.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(0, 15).addBox(-1.5F, -3.0F, 0.0F, 3.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 11.0F, -0.0037F, 0.0F, 0.0F));

		PartDefinition BackSaddle = partdefinition.addOrReplaceChild("BackSaddle", CubeListBuilder.create().texOffs(88, 43).addBox(-6.0F, -7.0F, 11.0F, 11.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(84, 101).addBox(3.0F, -6.0F, 4.0F, 2.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 102).addBox(-6.0F, -6.0F, 4.0F, 2.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(56, 52).addBox(-4.0F, -4.0F, 3.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -4.0F));

		PartDefinition Neck1 = partdefinition.addOrReplaceChild("Neck1", CubeListBuilder.create().texOffs(30, 58).addBox(-4.0F, -2.0F, -8.9F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(-3.0F, -3.0F, -8.9F, 6.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(54, 61).addBox(2.0F, -2.0F, -8.9F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.0F));

		PartDefinition Neck2 = partdefinition.addOrReplaceChild("Neck2", CubeListBuilder.create().texOffs(0, 68).addBox(-2.5F, -3.0F, -6.2F, 5.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 90).addBox(1.5F, -2.0F, -6.2F, 2.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(84, 89).addBox(-3.5F, -2.0F, -6.2F, 2.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -9.0F));

		PartDefinition Neck3 = partdefinition.addOrReplaceChild("Neck3", CubeListBuilder.create().texOffs(40, 110).addBox(-3.5F, -2.0F, -5.61F, 2.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(56, 110).addBox(1.5F, -2.0F, -5.61F, 2.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(90, 33).addBox(-2.5F, -3.0F, -5.61F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -6.0F));

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, -3.4F));

		PartDefinition HeadFront = partdefinition.addOrReplaceChild("HeadFront", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.7F, -3.9F, -0.014F, 0.0F, 0.0F));

		// The export put the muzzle shell in Jaw and extended it seven pixels
		// beyond the native snout. Keep its boxes/UVs, but attach upper plates to
		// HeadFront. Only the lower plate should rotate with the mandible.
		PartDefinition Jaw = partdefinition.addOrReplaceChild("Jaw", CubeListBuilder.create()
		.texOffs(40, 102).addBox(-2.0F, 1.1F, -6.6F, 4.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.4F, -3.8F));

		// The wider rear plate protects the chin/throat, behind the jaw hinge.
		Head.addOrReplaceChild("ChinGuard", CubeListBuilder.create(), PartPose.offset(0.0F, 2.4F, -3.8F));

		// inverse(HeadFront author pose) * Jaw author pose; removes the snout's
		// authored -0.014 rad tilt before reusing the old Jaw-local coordinates.
		float muzzleCos = (float) Math.cos(0.014F);
		float muzzleSin = (float) Math.sin(0.014F);
		HeadFront.addOrReplaceChild("MuzzleGuard", CubeListBuilder.create(),
		PartPose.offsetAndRotation(0.0F, 1.7F * muzzleCos - 0.1F * muzzleSin,
				1.7F * muzzleSin + 0.1F * muzzleCos, 0.014F, 0.0F, 0.0F));

		// Native fire-dragon horn hierarchy, rigidly attached to Head. Root
		// placements/lean fit the crown; descendant offsets and bends match IAF.
		PartDefinition Detail = Head.addOrReplaceChild("Detail", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition HornR = Detail.addOrReplaceChild("HornR", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.2F, -1.1F, -1.8F, 0.61086524F, -0.33161256F, -0.19198622F));
		PartDefinition HornR2 = HornR.addOrReplaceChild("HornR2", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.5F, 0.3F, 3.3F, -0.06981317F, 0.0F, 0.0F));
		HornR2.addOrReplaceChild("HornR3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.5F, 0.0F, 2.6F, 0.08203047F, 0.0F, 0.0F));
		PartDefinition HornL = Detail.addOrReplaceChild("HornL", CubeListBuilder.create(), PartPose.offsetAndRotation(2.2F, -1.1F, -1.8F, 0.61086524F, 0.33161256F, 0.19198622F));
		PartDefinition HornL2 = HornL.addOrReplaceChild("HornL2", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.5F, 0.3F, 3.3F, -0.06981317F, 0.0F, 0.0F));
		HornL2.addOrReplaceChild("HornL3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.5F, 0.0F, 2.6F, 0.08203047F, 0.0F, 0.0F));
		PartDefinition HornR4 = Detail.addOrReplaceChild("HornR4", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.6F, -0.2F, -0.9F, 0.34906585F, -0.31415927F, 0.0F));
		HornR4.addOrReplaceChild("HornR5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.8F, 3.6F, 0.17453293F, 0.0F, 0.0F));
		PartDefinition HornL4 = Detail.addOrReplaceChild("HornL4", CubeListBuilder.create(), PartPose.offsetAndRotation(2.6F, -0.2F, -0.9F, 0.34906585F, 0.31415927F, 0.0F));
		HornL4.addOrReplaceChild("HornL5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.8F, 3.6F, 0.17453293F, 0.0F, 0.0F));

		PartDefinition FrontSaddle = partdefinition.addOrReplaceChild("FrontSaddle", CubeListBuilder.create().texOffs(56, 43).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(20, 100).addBox(-6.0F, -6.0F, -4.0F, 2.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(100, 58).addBox(3.0F, -6.0F, -4.0F, 2.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(88, 14).addBox(-6.0F, -7.0F, -7.0F, 11.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

}
