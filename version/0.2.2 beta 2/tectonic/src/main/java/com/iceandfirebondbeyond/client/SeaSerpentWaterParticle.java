package com.iceandfirebondbeyond.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/** Independent, client-only droplets with their own swept block-collision boxes. No combat callbacks. */
public final class SeaSerpentWaterParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private SeaSerpentWaterParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        xd = vx; yd = vy; zd = vz;
        lifetime = 16 + random.nextInt(13);
        quadSize = .055F + random.nextFloat() * .095F;
        setSize(.06F, .06F);
        hasPhysics = true;
        setColor(.60F + random.nextFloat() * .15F, .84F, 1F);
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        // Particle.move sweeps this particle's own box against block shapes, independently of bubbles.
        double beforeX = x, beforeY = y, beforeZ = z;
        double vx = xd, vy = yd, vz = zd;
        move(vx, vy, vz);
        boolean contact = Math.abs(x - beforeX - vx) > 1E-5
                || Math.abs(y - beforeY - vy) > 1E-5 || Math.abs(z - beforeZ - vz) > 1E-5;
        if (contact) {
            level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, .025D, 0);
            if (random.nextBoolean()) level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0, 0);
            remove();
            return;
        }
        xd *= .96D; zd *= .96D; yd = yd * .96D - .008D;
        alpha = Math.min(1F, (lifetime - age) / 6F);
        setSpriteFromAge(sprites);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                  double x, double y, double z, double vx, double vy, double vz) {
            return new SeaSerpentWaterParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
