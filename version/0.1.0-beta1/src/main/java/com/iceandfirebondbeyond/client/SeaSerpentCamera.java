package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.client.IafKeybindRegistry;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.mixin.CameraAccess;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ViewportEvent;

/** Reuses IAF's remappable F7 binding, with one change per key press. */
public final class SeaSerpentCamera {
    private static boolean keyWasDown;
    private static int mountId = -1;
    private static int view;
    private static boolean changedView;
    private static CameraType previousView;

    private SeaSerpentCamera() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent serpent)) {
            restore(minecraft);
            return;
        }
        if (mountId != serpent.getId()) {
            restore(minecraft);
            mountId = serpent.getId();
            previousView = minecraft.options.getCameraType();
            view = previousView == CameraType.FIRST_PERSON ? 0
                    : previousView == CameraType.THIRD_PERSON_FRONT ? 3 : 1;
        }
        boolean down = minecraft.screen == null && IafKeybindRegistry.dragon_change_view != null
                && IafKeybindRegistry.dragon_change_view.isDown();
        if (down && !keyWasDown) {
            view = (view + 1) % 5;
            changedView = true;
            minecraft.options.setCameraType(view == 0 ? CameraType.FIRST_PERSON
                    : view >= 3 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            minecraft.player.displayClientMessage(Component.translatable(
                    "message.iceandfire_bond_beyond.sea_serpent.camera_" + view), true);
        }
        keyWasDown = down;
    }

    private static void restore(Minecraft minecraft) {
        if (changedView && previousView != null) minecraft.options.setCameraType(previousView);
        previousView = null;
        mountId = -1;
        keyWasDown = false;
        changedView = false;
    }

    public static void setup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getCamera().getEntity() != minecraft.player
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent serpent)
                || minecraft.options.getCameraType().isFirstPerson()) return;
        CameraAccess camera = (CameraAccess) event.getCamera();
        // Both far modes scale with the animal, so a scale-11 head fits in the
        // front view as well. Keep the native block-collision zoom limit.
        double extraDistance = serpent.getSeaSerpentScale() * (view == 2 || view == 4 ? 3.0D : 1.2D);
        // Same collision-limited zoom used by IAF's dragon camera.
        camera.bondBeyond$move(-camera.bondBeyond$getMaxZoom(extraDistance), 0, 0);
    }
}
