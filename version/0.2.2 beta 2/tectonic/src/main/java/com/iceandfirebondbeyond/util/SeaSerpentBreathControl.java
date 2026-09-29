package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-only state. Values never retain their weak owner/world keys. */
public final class SeaSerpentBreathControl {
    private static final Map<EntitySeaSerpent, SeaSerpentBreathBudget> SOURCES = new WeakHashMap<>();
    private static final Map<Level, SeaSerpentBreathBudget.Cooldowns<Long>> SOIL = new WeakHashMap<>();
    private SeaSerpentBreathControl() {}

    public static SeaSerpentBreathBudget budget(EntitySeaSerpent serpent) {
        return SOURCES.computeIfAbsent(serpent, ignored -> new SeaSerpentBreathBudget());
    }

    public static boolean reserveSoil(Level level, BlockPos pos) {
        return SOIL.computeIfAbsent(level, ignored -> new SeaSerpentBreathBudget.Cooldowns<>(8192))
                .take(pos.asLong(), level.getGameTime(), SeaSerpentBreathTuning.BLOCK_COOLDOWN_TICKS);
    }

    public static void releaseSoil(Level level, BlockPos pos) {
        SeaSerpentBreathBudget.Cooldowns<Long> soil = SOIL.get(level);
        if (soil != null) soil.release(pos.asLong(), level.getGameTime() + SeaSerpentBreathTuning.BLOCK_COOLDOWN_TICKS);
    }
}
