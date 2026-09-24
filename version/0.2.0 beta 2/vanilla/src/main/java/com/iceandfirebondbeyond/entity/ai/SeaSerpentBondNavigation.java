package com.iceandfirebondbeyond.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.ai.SeaSerpentPathNavigator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

/** IAF's 3D navigator, with shore-capable nodes while an owner controls it. */
public final class SeaSerpentBondNavigation extends SeaSerpentPathNavigator {
    public SeaSerpentBondNavigation(EntitySeaSerpent serpent, Level level) {
        super(serpent, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new AmphibiousNodeEvaluator(false);
        nodeEvaluator.setCanFloat(true);
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected void followThePath() {
        if (path == null || path.isDone()) {
            return;
        }
        Vec3 next = path.getNextEntityPos(mob);
        double tolerance = Mth.clamp(mob.getBbWidth() * 0.25D, 0.45D, 1.2D);
        // IAF multiplies this tolerance by width * speed * 6. On a scale-11
        // serpent that skips corners many blocks away and makes it circle.
        if (Math.abs(mob.getX() - next.x) < tolerance
                && Math.abs(mob.getZ() - next.z) < tolerance
                && Math.abs(mob.getY() - next.y) < 1.0D) {
            path.advance();
        }
        doStuckDetection(getTempMobPos());
    }
}
