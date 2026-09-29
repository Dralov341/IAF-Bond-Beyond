package com.iceandfirebondbeyond.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Edition-specific egg balance, shared by otherwise identical source builds. */
public enum OceanIncubationProfile {
    TECTONIC(15, 20, 0.001F, 0.15F),
    VANILLA_OCEAN(8, 32, 0.0005F, 0.05F);

    public static final int PRESSURE_CHECK_INTERVAL = 100;
    public final int minimumWaterDepth;
    public final int safeWaterDepth;
    private final float chancePerExtraBlock;
    private final float maximumChance;

    OceanIncubationProfile(int minimumWaterDepth, int safeWaterDepth,
                           float chancePerExtraBlock, float maximumChance) {
        this.minimumWaterDepth = minimumWaterDepth;
        this.safeWaterDepth = safeWaterDepth;
        this.chancePerExtraBlock = chancePerExtraBlock;
        this.maximumChance = maximumChance;
    }

    public float pressureBreakChance(int waterDepth) {
        return pressureBreakChance(waterDepth, safeWaterDepth);
    }

    public float pressureBreakChance(int waterDepth, int safeDepth) {
        if (waterDepth <= safeDepth) return 0.0F;
        return Math.min(maximumChance, (waterDepth - safeDepth) * chancePerExtraBlock);
    }

    public static OceanIncubationProfile current() {
        return Active.PROFILE;
    }

    private static final class Active {
        private static final OceanIncubationProfile PROFILE = load();

        private static OceanIncubationProfile load() {
            try (InputStream stream = OceanIncubationProfile.class.getResourceAsStream("/bond_beyond_ocean.properties")) {
                if (stream == null) throw new IllegalStateException("Missing Bond Beyond ocean profile");
                Properties properties = new Properties();
                properties.load(stream);
                return switch (properties.getProperty("profile", "")) {
                    case "tectonic" -> TECTONIC;
                    case "vanilla-ocean" -> VANILLA_OCEAN;
                    default -> throw new IllegalStateException("Unknown Bond Beyond ocean profile");
                };
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read Bond Beyond ocean profile", exception);
            }
        }
    }
}
