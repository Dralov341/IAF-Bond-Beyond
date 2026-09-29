package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.config.BondBeyondConfig;
import net.minecraft.world.level.GameRules;

/** One roll on the normal death-loot path; corpse harvesting never rolls eggs. */
public final class SeaSerpentEggLoot {
    public static double chance(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide
                || !serpent.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || SeaSerpentBondData.wasHatched(serpent) || SeaSerpentBondData.isTamed(serpent)
                || !SeaSerpentBondData.isFemale(serpent)) return 0;
        return switch (SeaSerpentBondData.getKnockbackStage(serpent)) {
            case 3 -> BondBeyondConfig.WILD_EGG_STAGE_3.get();
            case 4 -> BondBeyondConfig.WILD_EGG_STAGE_4.get();
            case 5 -> BondBeyondConfig.WILD_EGG_STAGE_5.get();
            default -> 0;
        };
    }

    public static boolean shouldDrop(EntitySeaSerpent serpent) {
        double chance = chance(serpent);
        return chance >= 1 || chance > 0 && serpent.getRandom().nextDouble() < chance;
    }

    private SeaSerpentEggLoot() {}
}
