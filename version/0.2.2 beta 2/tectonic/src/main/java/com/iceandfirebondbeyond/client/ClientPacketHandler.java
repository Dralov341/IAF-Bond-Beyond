package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.network.SeaSerpentEquipmentSyncPacket;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleEquipmentSync(SeaSerpentEquipmentSyncPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(message.entityId());
        if (entity instanceof EntitySeaSerpent serpent) {
            SeaSerpentBondData.setClientEquipmentState(
                    serpent,
                    message.armorTier(),
                    message.bondState()
            );
        }
    }
}
