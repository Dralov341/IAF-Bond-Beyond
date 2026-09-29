package com.iceandfirebondbeyond.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Access only the armor layer being replaced; leave all other render layers. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccess {
    @Accessor("layers")
    List<RenderLayer<?, ?>> bondBeyond$getLayers();
}
