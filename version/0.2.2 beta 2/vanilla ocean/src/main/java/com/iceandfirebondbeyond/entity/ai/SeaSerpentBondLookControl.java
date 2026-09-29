package com.iceandfirebondbeyond.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.world.entity.ai.control.LookControl;

/** Prevent a previously queued random look from turning an escorted/ridden serpent. */
public final class SeaSerpentBondLookControl extends LookControl {
    private final EntitySeaSerpent serpent;
    private final LookControl nativeControl;

    public SeaSerpentBondLookControl(EntitySeaSerpent serpent, LookControl nativeControl) {
        super(serpent);
        this.serpent = serpent;
        this.nativeControl = nativeControl;
    }

    @Override
    public void setLookAt(double x, double y, double z, float yawLimit, float pitchLimit) {
        super.setLookAt(x, y, z, yawLimit, pitchLimit);
        nativeControl.setLookAt(x, y, z, yawLimit, pitchLimit);
    }

    @Override
    public void tick() {
        if (SeaSerpentBondGoals.controlsMovement(serpent)) {
            serpent.yHeadRot = serpent.getYRot();
        } else {
            nativeControl.tick();
        }
    }
}
