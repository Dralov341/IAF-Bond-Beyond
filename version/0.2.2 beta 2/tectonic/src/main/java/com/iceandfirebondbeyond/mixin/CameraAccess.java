package com.iceandfirebondbeyond.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Keep private camera access mapped for both the development run and the released jar. */
@Mixin(Camera.class)
public interface CameraAccess {
    @Invoker("move")
    void bondBeyond$move(double distance, double vertical, double horizontal);

    @Invoker("getMaxZoom")
    double bondBeyond$getMaxZoom(double distance);

    @Invoker("setPosition")
    void bondBeyond$setPosition(Vec3 position);
}
