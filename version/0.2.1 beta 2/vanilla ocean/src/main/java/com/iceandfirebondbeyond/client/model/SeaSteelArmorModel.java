package com.iceandfirebondbeyond.client.model;

import com.github.alexthe666.iceandfire.client.model.armor.ArmorModelBase;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Left atlas half: Dragonsteel plates. Right half: Tide Guardian fin UVs. */
public final class SeaSteelArmorModel extends ArmorModelBase {
    public SeaSteelArmorModel(boolean inner) { super(createMesh(new CubeDeformation(inner ? 0.38F : 0.45F)).getRoot().bake(128, 64)); }
    private static MeshDefinition createMesh(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.getChild("head").addOrReplaceChild("visor1", CubeListBuilder.create().texOffs(27, 50).addBox(-4.7F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(0.0F, 9.0F, 0.2F));
        root.getChild("head").addOrReplaceChild("visor2", CubeListBuilder.create().texOffs(27, 50).mirror().addBox(0.8F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(-0.1F, 9.0F, 0.2F));
        root.getChild("right_arm").addOrReplaceChild("sleeveRight", CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(0.3F, -0.3F, 0.0F, 0.0F, 0.0F, -0.12217304763960307F));
        root.getChild("left_arm").addOrReplaceChild("sleeveLeft", CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(-0.7F, -0.3F, 0.0F, 0.0F, 0.0F, 0.12217304763960307F));
        root.getChild("right_leg").addOrReplaceChild("robeLowerRight", CubeListBuilder.create().texOffs(4, 51).mirror().addBox(-2.1F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));
        root.getChild("left_leg").addOrReplaceChild("robeLowerLeft", CubeListBuilder.create().texOffs(4, 51).addBox(-1.9F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));
        root.getChild("head").addOrReplaceChild("headFin", CubeListBuilder.create().texOffs(64, 32).addBox(-0.5F, -8.4F, -7.9F, 1, 16, 14), PartPose.offsetAndRotation(-3.5F, -8.8F, 3.5F, 3.141592653589793F, -0.5235987755982988F, 0.0F));
        root.getChild("head").addOrReplaceChild("headFin2", CubeListBuilder.create().texOffs(64, 32).mirror().addBox(-0.5F, -8.4F, -7.9F, 1, 16, 14), PartPose.offsetAndRotation(3.5F, -8.8F, 3.5F, 3.141592653589793F, 0.5235987755982988F, 0.0F));
        root.getChild("right_arm").addOrReplaceChild("armFinR", CubeListBuilder.create().texOffs(94, 32).addBox(-0.5F, -5.4F, -6.0F, 1, 7, 5), PartPose.offsetAndRotation(-1.5F, 4.0F, -0.4F, 3.141592653589793F, -1.3089969389957472F, -0.003490658503988659F));
        root.getChild("left_arm").addOrReplaceChild("armFinL", CubeListBuilder.create().texOffs(94, 32).mirror().addBox(-0.5F, -5.4F, -6.0F, 1, 7, 5), PartPose.offsetAndRotation(1.5F, 4.0F, -0.4F, 3.141592653589793F, 1.3089969389957472F, 0.0F));
        root.getChild("right_leg").addOrReplaceChild("legFinR", CubeListBuilder.create().texOffs(109, 31).addBox(-0.5F, -5.4F, -6.0F, 1, 7, 6), PartPose.offsetAndRotation(-1.5F, 5.2F, 1.6F, 3.141592653589793F, -1.3089969389957472F, 0.0F));
        root.getChild("left_leg").addOrReplaceChild("legFinL", CubeListBuilder.create().texOffs(109, 31).mirror().addBox(-0.5F, -5.4F, -6.0F, 1, 7, 6), PartPose.offsetAndRotation(1.5F, 5.2F, 1.6F, 3.141592653589793F, 1.3089969389957472F, 0.0F));
        return mesh;
    }
}
