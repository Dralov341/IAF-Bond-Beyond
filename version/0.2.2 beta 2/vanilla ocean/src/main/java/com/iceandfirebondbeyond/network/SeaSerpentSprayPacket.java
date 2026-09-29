package com.iceandfirebondbeyond.network;

import com.iceandfirebondbeyond.client.SeaSerpentWaterEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** One visual pulse per source, independent of projectile tracking and impact packets. */
public record SeaSerpentSprayPacket(ResourceLocation dimension, Vec3 mouth, Vec3 aim,
                                    int stage, float scale, float spread) {
    public SeaSerpentSprayPacket {
        if (!Double.isFinite(mouth.lengthSqr()) || !Double.isFinite(aim.lengthSqr())
                || aim.lengthSqr() < 1E-8 || stage < 1 || stage > 5
                || !Float.isFinite(scale) || scale < .8F || scale > 11F
                || !Float.isFinite(spread) || spread < 1F || spread > 20F)
            throw new IllegalArgumentException("Invalid water spray");
    }
    public static void encode(SeaSerpentSprayPacket p, FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension);
        b.writeDouble(p.mouth.x); b.writeDouble(p.mouth.y); b.writeDouble(p.mouth.z);
        b.writeDouble(p.aim.x); b.writeDouble(p.aim.y); b.writeDouble(p.aim.z);
        b.writeByte(p.stage); b.writeFloat(p.scale); b.writeFloat(p.spread);
    }
    public static SeaSerpentSprayPacket decode(FriendlyByteBuf b) {
        return new SeaSerpentSprayPacket(b.readResourceLocation(),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                b.readUnsignedByte(), b.readFloat(), b.readFloat());
    }
    public static void handle(SeaSerpentSprayPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SeaSerpentWaterEffects.spray(p)));
        context.setPacketHandled(true);
    }
}
