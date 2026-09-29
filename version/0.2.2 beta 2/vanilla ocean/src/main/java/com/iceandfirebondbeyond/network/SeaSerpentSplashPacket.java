package com.iceandfirebondbeyond.network;

import com.iceandfirebondbeyond.client.SeaSerpentWaterEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Actual server contact point: cosmetic water only, never client-authorized damage. */
public record SeaSerpentSplashPacket(ResourceLocation dimension, Vec3 point, float diameter) {
    public SeaSerpentSplashPacket {
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)
                || !Float.isFinite(diameter) || diameter < .12F || diameter > 6.2F)
            throw new IllegalArgumentException("Invalid water impact");
    }
    public static void encode(SeaSerpentSplashPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.point.x); buffer.writeDouble(packet.point.y); buffer.writeDouble(packet.point.z);
        buffer.writeFloat(packet.diameter);
    }
    public static SeaSerpentSplashPacket decode(FriendlyByteBuf buffer) {
        return new SeaSerpentSplashPacket(buffer.readResourceLocation(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
    }
    public static void handle(SeaSerpentSplashPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SeaSerpentWaterEffects.impact(packet)));
        context.setPacketHandled(true);
    }
}
