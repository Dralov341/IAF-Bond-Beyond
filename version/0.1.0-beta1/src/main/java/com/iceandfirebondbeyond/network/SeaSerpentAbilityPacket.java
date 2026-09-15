package com.iceandfirebondbeyond.network;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts or stops rider-controlled bubble breath on the server. */
public record SeaSerpentAbilityPacket(boolean active) {
    public static void encode(
            SeaSerpentAbilityPacket message,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(message.active);
    }

    public static SeaSerpentAbilityPacket decode(FriendlyByteBuf buffer) {
        return new SeaSerpentAbilityPacket(buffer.readBoolean());
    }

    public static void handle(
            SeaSerpentAbilityPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null
                    && sender.getVehicle() instanceof EntitySeaSerpent serpent) {
                SeaSerpentBondData.setMountedAbilityActive(
                        serpent,
                        sender,
                        message.active
                );
            }
        });
        context.setPacketHandled(true);
    }
}
