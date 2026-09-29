package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.config.BiomeConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/** Same eligible oceans for egg incubation and living-serpent temperature bonuses. */
public final class SeaSerpentOcean {
    private SeaSerpentOcean() {}
    public static boolean isOcean(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_OCEAN)) return true;
        try { return BiomeConfig.test(BiomeConfig.seaSerpentBiomes, biome); }
        catch (Exception ignored) { return false; }
    }
}
