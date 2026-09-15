package com.iceandfirebondbeyond.network;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.client.ClientPacketHandler;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SeaSerpentEquipmentSyncPacket(
        int entityId,
        int armorTier,
        CompoundTag bondState
) {
    public static SeaSerpentEquipmentSyncPacket from(EntitySeaSerpent serpent) {
        return new SeaSerpentEquipmentSyncPacket(
                serpent.getId(),
                SeaSerpentBondData.getArmorTierNetworkId(serpent),
                SeaSerpentBondData.createClientState(serpent)
        );
    }

    public static void encode(
            SeaSerpentEquipmentSyncPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.armorTier);
        buffer.writeNbt(message.bondState);
    }

    public static SeaSerpentEquipmentSyncPacket decode(FriendlyByteBuf buffer) {
        return new SeaSerpentEquipmentSyncPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readNbt()
        );
    }

    public static void handle(
            SeaSerpentEquipmentSyncPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleEquipmentSync(message)
        ));
        context.setPacketHandled(true);
    }
}
