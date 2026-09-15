package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.client.IafKeybindRegistry;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import com.iceandfirebondbeyond.network.ModNetwork;
import com.iceandfirebondbeyond.network.SeaSerpentBreachPacket;
import com.iceandfirebondbeyond.network.SeaSerpentRiderInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reuses IAF's R/G/X bindings; Space and movement use the player's own bindings. */
@Mod.EventBusSubscriber(
        modid = IceAndFireBondBeyond.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientForgeEvents {
    private static SeaSerpentRiderInputPacket lastInput;
    private static int heartbeatTicks;
    private static boolean lastBreachState;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void setupCamera(ViewportEvent.ComputeCameraAngles event) {
        SeaSerpentCamera.setup(event);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        SeaSerpentCamera.tick(minecraft);
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent serpent)) {
            lastInput = null;
            heartbeatTicks = 0;
            lastBreachState = false;
            return;
        }
        boolean acceptingInput = minecraft.screen == null && minecraft.isWindowActive();
        long window = minecraft.getWindow().getWindow();
        int flags = 0;
        if (acceptingInput) {
            if (SeaSerpentKeys.isHeld(minecraft.options.keyJump, window)) flags |= SeaSerpentRiderInputPacket.UP;
            if (SeaSerpentKeys.isHeld(IafKeybindRegistry.dragon_down, window)) {
                flags |= SeaSerpentRiderInputPacket.DOWN;
            }
            if (SeaSerpentKeys.isHeld(IafKeybindRegistry.dragon_fireAttack, window)) {
                flags |= SeaSerpentRiderInputPacket.BREATH;
            }
            if (SeaSerpentKeys.isHeld(SeaSerpentKeys.BITE, window)) flags |= SeaSerpentRiderInputPacket.BITE;
        }
        SeaSerpentRiderInputPacket input = new SeaSerpentRiderInputPacket(serpent.getId(),
                acceptingInput ? minecraft.player.input.forwardImpulse : 0.0F,
                acceptingInput ? minecraft.player.input.leftImpulse : 0.0F, flags);
        if (!input.equals(lastInput) || ++heartbeatTicks >= 10) {
            ModNetwork.CHANNEL.sendToServer(input);
            lastInput = input;
            heartbeatTicks = 0;
        }
        boolean breach = acceptingInput
                && SeaSerpentKeys.isHeld(IafKeybindRegistry.dragon_strike, window);

        // The breach is a one-shot action, so only send the rising key edge.
        if (breach && !lastBreachState) {
            ModNetwork.CHANNEL.sendToServer(new SeaSerpentBreachPacket());
        }
        lastBreachState = breach;
    }

}
