package com.iceandfirebondbeyond.network;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Input only: the server computes movement, stage speed and damage. */
public record SeaSerpentRiderInputPacket(int entityId, float forward, float strafe, int flags,
        float cameraDistance, float cameraHeight) {
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int BREATH = 4;
    public static final int BITE = 8;

    public static void encode(SeaSerpentRiderInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
        buffer.writeByte(packet.flags);
        buffer.writeFloat(packet.cameraDistance);
        buffer.writeFloat(packet.cameraHeight);
    }

    public static SeaSerpentRiderInputPacket decode(FriendlyByteBuf buffer) {
        return new SeaSerpentRiderInputPacket(buffer.readVarInt(), buffer.readFloat(),
                buffer.readFloat(), buffer.readUnsignedByte(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(SeaSerpentRiderInputPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null && sender.getVehicle() instanceof EntitySeaSerpent serpent
                    && serpent.getId() == packet.entityId && (packet.flags & ~15) == 0) {
                SeaSerpentBondData.acceptRiderInput(serpent, sender, packet.forward, packet.strafe,
                        (packet.flags & UP) != 0, (packet.flags & DOWN) != 0,
                        (packet.flags & BREATH) != 0, (packet.flags & BITE) != 0,
                        packet.cameraDistance, packet.cameraHeight);
            }
        });
        context.setPacketHandled(true);
    }
}
