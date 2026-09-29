package com.iceandfirebondbeyond.inventory;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.network.ModNetwork;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.world.SimpleContainer;

/**
 * One-slot armor view backed directly by the serpent's persistent data.
 */
public final class SeaSerpentEquipmentContainer extends SimpleContainer {
    private final EntitySeaSerpent serpent;
    private boolean loading = true;

    public SeaSerpentEquipmentContainer(EntitySeaSerpent serpent) {
        super(1);
        this.serpent = serpent;
        super.setItem(
                SeaSerpentBondData.ARMOR_SLOT,
                SeaSerpentBondData.getArmorStack(serpent)
        );
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading || serpent.level().isClientSide) {
            return;
        }
        SeaSerpentBondData.setArmorStack(
                serpent,
                getItem(SeaSerpentBondData.ARMOR_SLOT)
        );
        SeaSerpentBondData.tickArmorModifier(serpent);
        ModNetwork.syncEquipment(serpent);
    }

    public EntitySeaSerpent getSerpent() {
        return serpent;
    }
}
