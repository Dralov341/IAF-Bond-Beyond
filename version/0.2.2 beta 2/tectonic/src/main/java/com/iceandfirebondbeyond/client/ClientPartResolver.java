package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import javax.annotation.Nullable;
import java.util.UUID;

/** beta-5 getParent() only resolves ServerLevel UUIDs; client clicks need this lookup. */
public final class ClientPartResolver {
    private ClientPartResolver() {}

    @Nullable
    public static Entity findParent(EntityMutlipartPart part) {
        UUID id = part.getParentId();
        if (id == null || !(part.level() instanceof ClientLevel level)) return null;
        // Click-time lookup only, not an entity/world scan every tick.
        for (Entity candidate : level.entitiesForRendering()) {
            if (id.equals(candidate.getUUID())) return candidate;
        }
        return null;
    }
}
