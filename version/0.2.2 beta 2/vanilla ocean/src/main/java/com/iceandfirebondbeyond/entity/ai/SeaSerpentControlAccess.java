package com.iceandfirebondbeyond.entity.ai;

/** Implemented by the narrow hook on IAF's original entity. */
public interface SeaSerpentControlAccess {
    void bondBeyond$updateNavigation();
    net.minecraft.world.phys.Vec3 bondBeyond$getMouthPosition();
    float bondBeyond$getSurfacePose(float partialTick);
    SeaSerpentCombatGoal bondBeyond$getCombatGoal();
}
