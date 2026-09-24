package com.iceandfirebondbeyond.client;

import com.github.alexthe666.citadel.client.model.TabulaModel;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.client.model.SeaSerpentRemainsModel;
import com.iceandfirebondbeyond.client.renderer.SeaSerpentCorpseRenderer;
import com.iceandfirebondbeyond.util.SeaSerpentDeath;
import com.iceandfirebondbeyond.util.SeaSerpentRemainsPose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Keeps the last live pose, then relaxes that same mesh into the saved corpse pose. */
@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID, value = Dist.CLIENT)
public final class SeaSerpentDeathClient {
    private static final Map<EntitySeaSerpent, Map<String, float[]>> LAST_POSE = new WeakHashMap<>();
    private static final SeaSerpentRemainsModel FLESH = new SeaSerpentRemainsModel(SeaSerpentRemainsModel.createFleshLayer().bakeRoot());

    public static void rememberPose(TabulaModel model, EntitySeaSerpent serpent) {
        if (serpent.isDeadOrDying()) return;
        Map<String, float[]> pose = LAST_POSE.computeIfAbsent(serpent, ignored -> new HashMap<>());
        for (var entry : model.getCubes().entrySet()) {
            var cube = entry.getValue();
            pose.put(cube.boxName, new float[]{cube.rotationPointX, cube.rotationPointY, cube.rotationPointZ,
                    cube.rotateAngleX, cube.rotateAngleY, cube.rotateAngleZ});
        }
    }

    @SubscribeEvent
    public static void renderDeath(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof EntitySeaSerpent serpent) || !serpent.isDeadOrDying()) return;
        event.setCanceled(true);
        if (serpent.isInvisible()) return;
        float scale = serpent.getSeaSerpentScale(), partial = event.getPartialTick();
        float yaw = Mth.rotLerp(partial, serpent.yBodyRotO, serpent.yBodyRot);
        float progress = SeaSerpentDeath.progress(Math.max(0, serpent.deathTime - 1 + partial));
        SeaSerpentRemainsPose target = SeaSerpentDeath.pose(serpent, scale, yaw,
                serpent.onGround() ? .7F : 1, serpent.onGround() ? 1 : 0);
        Map<String, float[]> start = LAST_POSE.computeIfAbsent(serpent, ignored -> initialPose(serpent));
        FLESH.deathPose(target, start, progress, serpent.onGround());
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.scale(-scale, -scale, scale);
        pose.translate(0, -1.501, 0);
        FLESH.root.render(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(
                SeaSerpentCorpseRenderer.fleshTexture(serpent.getVariant()))), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static Map<String, float[]> initialPose(EntitySeaSerpent serpent) {
        // A serpent killed before its first rendered frame has no cached pose.
        Map<String, float[]> pose = new HashMap<>();
        SeaSerpentRemainsPose base = SeaSerpentRemainsPose.create(0, 0, null);
        for (int i = 0; i < SeaSerpentRemainsPose.NAMES.length; i++) {
            float[] p = base.position[i];
            pose.put(SeaSerpentRemainsPose.NAMES[i], new float[]{p[0], p[1], p[2], 0, 0, 0});
        }
        pose.put("BodyUpper", new float[]{0, 20, -10, (2 - serpent.getXRot()) * Mth.DEG_TO_RAD,
                -10.43F * Mth.DEG_TO_RAD, 0});
        return pose;
    }
    private SeaSerpentDeathClient() {}
}
