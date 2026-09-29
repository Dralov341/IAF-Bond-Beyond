package com.iceandfirebondbeyond.network;

import com.iceandfirebondbeyond.client.DragonSteelLightningClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Only the server chooses damage/targets; clients receive bounded visual endpoints. */
public record LightningArcPacket(ResourceLocation dimension, List<Arc> arcs) {
    public LightningArcPacket {
        arcs = List.copyOf(arcs);
        if (arcs.size() > 10) throw new IllegalArgumentException("Too many lightning arcs");
    }

    public record Arc(Vec3 start, Vec3 end) {
        public Arc {
            if (!finite(start) || !finite(end)) throw new IllegalArgumentException("Invalid lightning endpoint");
        }
        private static boolean finite(Vec3 point) {
            return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
        }
    }

    public static void encode(LightningArcPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.arcs.size());
        for (Arc arc : packet.arcs) {
            buffer.writeDouble(arc.start.x); buffer.writeDouble(arc.start.y); buffer.writeDouble(arc.start.z);
            buffer.writeDouble(arc.end.x); buffer.writeDouble(arc.end.y); buffer.writeDouble(arc.end.z);
        }
    }

    public static LightningArcPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int count = buffer.readVarInt();
        if (count < 0 || count > 10) throw new IllegalArgumentException("Invalid lightning arc count");
        List<Arc> arcs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) arcs.add(new Arc(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())));
        return new LightningArcPacket(dimension, arcs);
    }

    public static void handle(LightningArcPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DragonSteelLightningClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
