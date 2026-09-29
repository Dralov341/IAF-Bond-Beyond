package com.iceandfirebondbeyond.config;

import com.github.alexthe666.iceandfire.IafConfig;
import com.iceandfirebondbeyond.util.OceanIncubationProfile;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Camera preferences stay local; world balance is server-owned and Forge-synced. */
public final class BondBeyondConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.DoubleValue CAMERA_DISTANCE, CAMERA_FAR_DISTANCE, CAMERA_HEIGHT, FOV_BONUS;
    public static final ForgeConfigSpec.BooleanValue AIM_RETICLE, AREA_DAMAGE, EROSION, GROWTH_IN_OCEANS;
    public static final ForgeConfigSpec.DoubleValue AREA_RADIUS, BUBBLE_SIZE, GROWTH_SPEED, INCUBATION_SPEED, PRESSURE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue HATCH_TIME, EGG_DEPTH, SAFE_DEPTH;
    public static final ForgeConfigSpec.IntValue BUBBLES_PER_PULSE, EROSION_BLOCKS_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue BREATH_DAMAGE, BREATH_SPREAD, BREATH_PUSH, EROSION_RADIUS, WATER_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue WILD_EGG_STAGE_3, WILD_EGG_STAGE_4, WILD_EGG_STAGE_5;
    public static final OceanRates GROWTH_RATES, INCUBATION_RATES;

    static {
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("riding_camera");
        CAMERA_DISTANCE = client.comment("Extra third-person distance per serpent scale; vanilla adds 4 blocks.")
                .defineInRange("distance_per_scale", 1.8D, 0.0D, 6.0D);
        CAMERA_FAR_DISTANCE = client.comment("Extra distance for the two far F7 views, per scale.")
                .defineInRange("far_distance_per_scale", 3.0D, 0.0D, 6.0D);
        CAMERA_HEIGHT = client.comment("World-up camera lift per scale. Collision limits lift and distance.")
                .defineInRange("height_per_scale", 1.1D, 0.0D, 3.0D);
        FOV_BONUS = client.comment("Extra third-person FOV degrees; gradually reaches this value at scale 6.",
                        "Does not change your saved FOV or first-person view. 0 disables the bonus.")
                .defineInRange("fov_bonus", 12.0D, 0.0D, 30.0D);
        AIM_RETICLE = client.comment("Show a center aiming reticle in the rear riding views.")
                .define("show_aim_reticle", true);
        client.pop();
        client.push("water_effects");
        WATER_PARTICLES = client.comment("Density of cosmetic water spray/trails/impact splashes. 0 disables particles only.")
                .defineInRange("particle_density", .65D, 0.0D, 1.0D);
        client.pop();
        CLIENT_SPEC = client.build();

        ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
        server.push("breath");
        AREA_DAMAGE = server.comment("Water impacts damage nearby visible enemies, for ALL breath sources.")
                .define("area_damage", true);
        AREA_RADIUS = server.comment("Multiplier for impact radius: Stage 1 = 1.5 blocks, Stage 5 = 4.5 blocks.",
                        "Full radius belongs to the central stream; satellite droplets have smaller bursts.",
                        "Final radius is capped at 12. 0 keeps direct hits only.")
                .defineInRange("area_radius_multiplier", 1.0D, 0.0D, 3.0D);
        BUBBLE_SIZE = server.comment("Visible projectile AND collision diameter multiplier. Applies to new shots.")
                .defineInRange("bubble_size_multiplier", 1.35D, 0.5D, 2.0D);
        BUBBLES_PER_PULSE = server.comment("Maximum small bubbles per 2-tick pulse at Stage 5 / scale 11. Younger/smaller serpents emit fewer, down to 4. Does NOT multiply DPS or erosion.",
                        "New spray key replaces the old narrow-stream bubbles_per_pulse; old files receive the corrected default.")
                .defineInRange("spray_bubbles_per_pulse", 12, 4, 16);
        BREATH_SPREAD = server.comment("Maximum soft spray half-angle at Stage 5 / scale 11; smaller serpents have a narrower spray. One precisely aimed forge carrier.",
                        "New spray key replaces spread_degrees so old 3.5-degree defaults do not keep the thin stream.")
                .defineInRange("spray_half_angle_degrees", 12.0D, 3.0D, 20.0D);
        BREATH_DAMAGE = server.comment("Whole-stream damage multiplier, shared by wild/tame/mounted/forge breath.",
                        "Default sustained DPS at ages 0/25/50/75/100/125 days: 2/4/7/12/18/22, before armor/elements.",
                        "Scales with IAF dragonAttackDamageFire. One damage window per victim/source every 10 ticks.")
                .defineInRange("damage_multiplier", 1.0D, 0.0D, 10.0D);
        BREATH_PUSH = server.comment("Gentle horizontal push on a successful damage window, not per visual bubble; respects knockback resistance.")
                .defineInRange("knockback_strength", .12D, 0.0D, .4D);
        EROSION = server.comment("Allow bubble erosion, including forge breath; still respects mobGriefing and Forge vetoes.")
                .define("erosion_enabled", true);
        EROSION_RADIUS = server.comment("Erosion radius multiplier for the central stream only; spread/satellite bubbles do not widen the footprint.",
                        "Default Stage 5 reach: 2.25 blocks around the impact, plus the shallow forge bed when powering a forge.")
                .defineInRange("erosion_radius_multiplier", .75D, .25D, 1.0D);
        EROSION_BLOCKS_PER_SECOND = server.comment("Maximum successful soil changes per second per Stage-5 serpent under continuous fire.",
                        "Lower stages get proportional budgets, rounded down per half-second. Very low caps may disable young-stage erosion.",
                        "One erosion pass per 10 ticks; a block can change once per 40 ticks.",
                        "All bubbles and forge-bed erosion share this budget. 0 disables soil changes without disabling forge power.")
                .defineInRange("erosion_blocks_per_second", 20, 0, 100);
        server.pop();

        server.push("growth");
        GROWTH_SPEED = server.comment("Post-hatch aging speed for wild and owned serpents; 1 = original speed, 0 pauses aging.",
                        "Does not change random spawn ages, hunger, newborn bonding time or growth food.")
                .defineInRange("base_speed", 1.0D, 0.0D, 100.0D);
        GROWTH_IN_OCEANS = server.comment("Apply the ocean multipliers below while submerged in an eligible ocean biome.",
                        "Warm ocean defaults to 1.3x. Disable for the original uniform aging speed.")
                .define("ocean_temperature_affects_growth", true);
        GROWTH_RATES = new OceanRates(server);
        server.pop();

        server.push("eggs");
        WILD_EGG_STAGE_3 = server.comment("Chance for ONE egg on death of a wild female Stage-3 Sea Serpent (0 disables, 1 guarantees).",
                        "No separate Ancient requirement. Males, Stage 1-2, hatched/owned serpents never drop hunting eggs; breeding is separate.",
                        "Respects doMobLoot. The egg drops at death, not from corpse harvesting.")
                .defineInRange("wild_female_stage_3_drop_chance", .25D, 0.0D, 1.0D);
        WILD_EGG_STAGE_4 = server.comment("Wild female Stage-4 egg chance; same rules as Stage 3.")
                .defineInRange("wild_female_stage_4_drop_chance", .50D, 0.0D, 1.0D);
        WILD_EGG_STAGE_5 = server.comment("Wild female Stage-5 egg chance; default guarantees ONE egg, same rules as Stage 3.")
                .defineInRange("wild_female_stage_5_drop_chance", 1.0D, 0.0D, 1.0D);
        INCUBATION_SPEED = server.comment("Global egg incubation speed, multiplied by the separate egg ocean rates below.")
                .defineInRange("incubation_speed", 1.0D, 0.0D, 100.0D);
        HATCH_TIME = server.comment("Required progress ticks before biome/speed modifiers. 0 inherits IAF dragonEggTime.")
                .defineInRange("hatch_time_ticks", 0, 0, 24000000);
        EGG_DEPTH = server.comment("Water blocks required ABOVE the egg. -1 uses the edition default (Tectonic 15; Vanilla 8).")
                .defineInRange("minimum_water_depth", -1, -1, 512);
        SAFE_DEPTH = server.comment("Pressure-safe water depth. -1 uses the edition default (Tectonic 20; Vanilla 32).")
                .defineInRange("pressure_safe_depth", -1, -1, 512);
        PRESSURE_MULTIPLIER = server.comment("Multiplier for the edition's pressure break chance; checked every 100 ticks.",
                        "0 disables pressure breaking. Does not affect incubation speed.")
                .defineInRange("pressure_chance_multiplier", 1.0D, 0.0D, 100.0D);
        INCUBATION_RATES = new OceanRates(server);
        server.pop();
        SERVER_SPEC = server.build();
    }

    private BondBeyondConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static int hatchTime() {
        int configured = HATCH_TIME.get();
        return Math.max(1, configured == 0 ? IafConfig.dragonEggTime : configured);
    }

    public static int eggDepth() {
        int configured = EGG_DEPTH.get();
        return configured < 0 ? OceanIncubationProfile.current().minimumWaterDepth : configured;
    }

    public static float pressureChance(int depth) {
        OceanIncubationProfile profile = OceanIncubationProfile.current();
        int safe = SAFE_DEPTH.get() < 0 ? profile.safeWaterDepth : SAFE_DEPTH.get();
        return (float) Math.min(1.0D, profile.pressureBreakChance(depth, safe) * PRESSURE_MULTIPLIER.get());
    }

    /** Separate sections let pack makers tune incubation without changing a living serpent's age. */
    public static final class OceanRates {
        private final ForgeConfigSpec.DoubleValue frozen, deepFrozen, cold, deepCold,
                ocean, deep, lukewarm, deepLukewarm, warm;

        private OceanRates(ForgeConfigSpec.Builder builder) {
            builder.comment("Multipliers by ocean biome; modded cold/hot oceans use cold_ocean/warm_ocean.")
                    .push("ocean_multipliers");
            frozen = rate(builder, "frozen_ocean", .75D);
            deepFrozen = rate(builder, "deep_frozen_ocean", .8D);
            cold = rate(builder, "cold_ocean", .85D);
            deepCold = rate(builder, "deep_cold_ocean", .9D);
            ocean = rate(builder, "ocean", 1.0D);
            deep = rate(builder, "deep_ocean", 1.05D);
            lukewarm = rate(builder, "lukewarm_ocean", 1.15D);
            deepLukewarm = rate(builder, "deep_lukewarm_ocean", 1.2D);
            warm = rate(builder, "warm_ocean", 1.3D);
            builder.pop();
        }

        private static ForgeConfigSpec.DoubleValue rate(ForgeConfigSpec.Builder builder, String name, double value) {
            return builder.defineInRange(name, value, 0.0D, 10.0D);
        }

        public double forBiome(Holder<Biome> biome) {
            if (biome.is(Biomes.FROZEN_OCEAN)) return frozen.get();
            if (biome.is(Biomes.DEEP_FROZEN_OCEAN)) return deepFrozen.get();
            if (biome.is(Biomes.COLD_OCEAN)) return cold.get();
            if (biome.is(Biomes.DEEP_COLD_OCEAN)) return deepCold.get();
            if (biome.is(Biomes.OCEAN)) return ocean.get();
            if (biome.is(Biomes.DEEP_OCEAN)) return deep.get();
            if (biome.is(Biomes.LUKEWARM_OCEAN)) return lukewarm.get();
            if (biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) return deepLukewarm.get();
            if (biome.is(Biomes.WARM_OCEAN)) return warm.get();
            if (biome.is(Tags.Biomes.IS_COLD)) return cold.get();
            if (biome.is(Tags.Biomes.IS_HOT)) return warm.get();
            return ocean.get();
        }
    }
}
