package com.iceandfirebondbeyond.client;

import com.iceandfirebondbeyond.config.BondBeyondConfig;
import com.iceandfirebondbeyond.network.SeaSerpentSplashPacket;
import com.iceandfirebondbeyond.network.SeaSerpentSprayPacket;
import com.iceandfirebondbeyond.registry.ModParticles;
import com.iceandfirebondbeyond.util.SeaSerpentWaterSpray;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;

/** Small asymmetric water bursts, scaled by the actual bubble and local particle preference. */
public final class SeaSerpentWaterEffects {
    private SeaSerpentWaterEffects() {}
    public static void spray(SeaSerpentSprayPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) return;
        double density = BondBeyondConfig.WATER_PARTICLES.get();
        double distance = minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(packet.mouth());
        if (density <= 0 || distance > 128 * 128) return;
        // Respect the vanilla particle preference even though these are spawned as one network pulse.
        density *= switch (minecraft.options.particles().get()) {
            case ALL -> 1D;
            case DECREASED -> .5D;
            case MINIMAL -> .15D;
        };
        if (distance > 64 * 64) density *= .5D;
        var random = minecraft.level.random;
        int count = (int) Math.ceil(SeaSerpentWaterSpray.particles(packet.stage(), packet.scale()) * density);
        double maturity = SeaSerpentWaterSpray.maturity(packet.stage(), packet.scale());
        for (int i = 0; i < count; i++) {
            var direction = SeaSerpentWaterSpray.direction(packet.aim(), Math.min(22, packet.spread() * 1.12D),
                    i + 1, count + 1, random.nextDouble(), random.nextDouble());
            var speed = direction.scale(1.3D + random.nextDouble() * 1.4D);
            var particle = minecraft.particleEngine.createParticle(ModParticles.WATER_SPRAY.get(),
                    packet.mouth().x, packet.mouth().y, packet.mouth().z, speed.x, speed.y, speed.z);
            if (particle != null) particle.scale((float) (.70D + .70D * maturity));
        }
    }
    public static void impact(SeaSerpentSplashPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) return;
        double density = BondBeyondConfig.WATER_PARTICLES.get();
        if (density <= 0 || minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(packet.point()) > 128 * 128) return;
        var level = minecraft.level;
        var random = level.random;
        boolean underwater = level.getFluidState(BlockPos.containing(packet.point())).is(FluidTags.WATER);
        int count = (int) Math.ceil((6 + packet.diameter() * 3) * density);
        double radius = packet.diameter() * .35D;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double outward = .035D + random.nextDouble() * .12D;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double x = packet.point().x + dx * radius * random.nextDouble();
            double y = packet.point().y + (random.nextDouble() - .3D) * radius;
            double z = packet.point().z + dz * radius * random.nextDouble();
            level.addParticle(i % 3 == 0 ? ParticleTypes.BUBBLE_POP
                            : underwater ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH,
                    x, y, z, dx * outward, .025D + random.nextDouble() * .1D, dz * outward);
            if (!underwater && i % 4 == 0) level.addParticle(ParticleTypes.FALLING_WATER,
                    x, y + .15D, z, 0, -.03D, 0);
        }
    }
}
