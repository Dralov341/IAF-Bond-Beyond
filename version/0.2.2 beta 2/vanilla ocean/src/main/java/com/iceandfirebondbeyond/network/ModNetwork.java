package com.iceandfirebondbeyond.network;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL = "9";
    private static int nextPacketId;
    private static boolean registered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(
                nextPacketId++,
                SeaSerpentEquipmentSyncPacket.class,
                SeaSerpentEquipmentSyncPacket::encode,
                SeaSerpentEquipmentSyncPacket::decode,
                SeaSerpentEquipmentSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SeaSerpentAbilityPacket.class,
                SeaSerpentAbilityPacket::encode,
                SeaSerpentAbilityPacket::decode,
                SeaSerpentAbilityPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SeaSerpentBreachPacket.class,
                SeaSerpentBreachPacket::encode,
                SeaSerpentBreachPacket::decode,
                SeaSerpentBreachPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(nextPacketId++, SeaSerpentRiderInputPacket.class,
                SeaSerpentRiderInputPacket::encode, SeaSerpentRiderInputPacket::decode,
                SeaSerpentRiderInputPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextPacketId++, LightningArcPacket.class,
                LightningArcPacket::encode, LightningArcPacket::decode,
                LightningArcPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextPacketId++, SeaSerpentSplashPacket.class,
                SeaSerpentSplashPacket::encode, SeaSerpentSplashPacket::decode,
                SeaSerpentSplashPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextPacketId++, SeaSerpentSprayPacket.class,
                SeaSerpentSprayPacket::encode, SeaSerpentSprayPacket::decode,
                SeaSerpentSprayPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncEquipment(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) {
            return;
        }
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> serpent),
                SeaSerpentEquipmentSyncPacket.from(serpent)
        );
    }

    public static void syncEquipmentTo(
            EntitySeaSerpent serpent,
            ServerPlayer player
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                SeaSerpentEquipmentSyncPacket.from(serpent)
        );
    }
}
