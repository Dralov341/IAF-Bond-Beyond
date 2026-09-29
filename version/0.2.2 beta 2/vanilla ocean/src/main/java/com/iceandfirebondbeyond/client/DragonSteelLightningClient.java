package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.client.particle.LightningBoltData;
import com.github.alexthe666.iceandfire.client.particle.LightningRender;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.network.LightningArcPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** CE's electrical arc style, using the renderer already supplied by original IAF. */
@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID, value = Dist.CLIENT)
public final class DragonSteelLightningClient {
    private static ClientLevel world;
    private static LightningRender renderer;

    public static void accept(LightningArcPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        resetWorld(minecraft.level);
        if (world == null || !world.dimension().location().equals(packet.dimension())) return;
        if (renderer == null) renderer = new LightningRender();
        for (LightningArcPacket.Arc arc : packet.arcs()) {
            LightningBoltData bolt = new LightningBoltData(LightningBoltData.BoltRenderInfo.ELECTRICITY,
                    arc.start(), arc.end(), 4).size(.05F).lifespan(10)
                    .fade(LightningBoltData.FadeFunction.fade(.1F))
                    .spawn(LightningBoltData.SpawnFunction.NO_DELAY);
            renderer.update(new Object(), bolt, minecraft.getFrameTime());
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        resetWorld(minecraft.level);
        if (world == null || renderer == null) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        renderer.render(event.getPartialTick(), pose, buffers);
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) resetWorld(Minecraft.getInstance().level);
    }

    private static void resetWorld(ClientLevel current) {
        if (world != current) { world = current; renderer = null; }
    }
    private DragonSteelLightningClient() {}
}
