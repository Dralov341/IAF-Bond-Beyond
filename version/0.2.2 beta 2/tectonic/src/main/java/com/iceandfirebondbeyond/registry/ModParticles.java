package com.iceandfirebondbeyond.registry;

import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, IceAndFireBondBeyond.MOD_ID);
    public static final RegistryObject<SimpleParticleType> WATER_SPRAY = TYPES.register(
            "water_spray", () -> new SimpleParticleType(false));
    private ModParticles() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
