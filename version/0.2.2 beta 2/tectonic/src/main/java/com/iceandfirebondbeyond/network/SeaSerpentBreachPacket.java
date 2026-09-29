package com.iceandfirebondbeyond.network;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests one server-authoritative Sea Serpent breach/slam. */
public record SeaSerpentBreachPacket() {
    public static void encode(
            SeaSerpentBreachPacket message,
            FriendlyByteBuf buffer
    ) {
        // No payload: sender and ridden entity come from the network context.
    }

    public static SeaSerpentBreachPacket decode(FriendlyByteBuf buffer) {
        return new SeaSerpentBreachPacket();
    }

    public static void handle(
            SeaSerpentBreachPacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null
                    && sender.getVehicle() instanceof EntitySeaSerpent serpent) {
                SeaSerpentBondData.tryStartMountedBreach(serpent, sender);
            }
        });
        context.setPacketHandled(true);
    }
}
