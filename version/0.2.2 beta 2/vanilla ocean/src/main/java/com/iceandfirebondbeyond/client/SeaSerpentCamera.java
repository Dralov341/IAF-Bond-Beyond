package com.iceandfirebondbeyond.client;

import com.github.alexthe666.iceandfire.client.IafKeybindRegistry;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.iceandfirebondbeyond.mixin.CameraAccess;
import com.iceandfirebondbeyond.config.BondBeyondConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/** Reuses IAF's remappable F7 binding, with one change per key press. */
public final class SeaSerpentCamera {
    private static boolean keyWasDown;
    private static int mountId = -1;
    private static int view;
    private static boolean changedView;
    private static CameraType previousView;
    private static int frameMountId = -1;
    private static float aimDistance, aimHeight;

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
        CameraType expected = view == 0 ? CameraType.FIRST_PERSON
                : view >= 3 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
        if (minecraft.options.getCameraType() != expected) {
            // Respect an ordinary F5 change; do not overwrite it on dismount.
            previousView = minecraft.options.getCameraType();
            changedView = false;
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
        frameMountId = -1;
        aimDistance = aimHeight = 0;
    }

    public static void setup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        frameMountId = -1;
        aimDistance = aimHeight = 0;
        if (minecraft.player == null || event.getCamera().getEntity() != minecraft.player
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent serpent)
                || minecraft.options.getCameraType().isFirstPerson()) return;
        CameraAccess camera = (CameraAccess) event.getCamera();
        float partial = (float) event.getPartialTick();
        Vec3 eye = minecraft.player.getEyePosition(partial);
        double scale = Mth.clamp(serpent.getSeaSerpentScale(), 1, 11);
        double lift = safeLift(minecraft, eye, scale * BondBeyondConfig.CAMERA_HEIGHT.get());
        // Raise the pivot in WORLD up, then apply native collision-limited zoom.
        // Moving on the camera's local up axis instead slides sideways when looking down.
        camera.bondBeyond$setPosition(eye.add(0, lift, 0));
        double factor = view == 2 || view == 4 ? BondBeyondConfig.CAMERA_FAR_DISTANCE.get()
                : BondBeyondConfig.CAMERA_DISTANCE.get();
        double distance = camera.bondBeyond$getMaxZoom(4.0D + scale * factor);
        camera.bondBeyond$move(-distance, 0, 0);
        if (minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            frameMountId = serpent.getId();
            aimDistance = (float) distance;
            aimHeight = (float) lift;
        }
    }

    /** Eight corner rays keep the raised camera out of low ceilings and cliff faces. */
    private static double safeLift(Minecraft minecraft, Vec3 eye, double wanted) {
        double lift = wanted;
        for (int corner = 0; corner < 8 && lift > 0; corner++) {
            Vec3 start = eye.add((corner & 1) == 0 ? -.1D : .1D,
                    (corner & 2) == 0 ? -.1D : .1D, (corner & 4) == 0 ? -.1D : .1D);
            HitResult hit = minecraft.level.clip(new ClipContext(start, start.add(0, wanted, 0),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
            if (hit.getType() != HitResult.Type.MISS) {
                lift = Math.min(lift, Math.max(0, hit.getLocation().y - start.y - .1D));
            }
        }
        return lift;
    }

    public static float aimDistance(Minecraft minecraft, EntitySeaSerpent serpent) {
        return frameMountId == serpent.getId() && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                ? aimDistance : 0;
    }

    public static float aimHeight(Minecraft minecraft, EntitySeaSerpent serpent) {
        return frameMountId == serpent.getId() && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                ? aimHeight : 0;
    }

    public static void fov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.usedConfiguredFov() || minecraft.player == null
                || event.getCamera().getEntity() != minecraft.player
                || minecraft.options.getCameraType().isFirstPerson()
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent serpent)) return;
        double bonus = BondBeyondConfig.FOV_BONUS.get()
                * Mth.clamp((serpent.getSeaSerpentScale() - 1.0D) / 5.0D, 0, 1);
        event.setFOV(Math.min(event.getFOV() + bonus, Math.max(110.0D, event.getFOV())));
    }

    public static void reticle(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())
                || !BondBeyondConfig.AIM_RETICLE.get() || minecraft.options.hideGui
                || minecraft.screen != null || minecraft.player == null || minecraft.player.isSpectator()
                || minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK
                || !(minecraft.player.getVehicle() instanceof EntitySeaSerpent)) return;
        int x = event.getWindow().getGuiScaledWidth() / 2;
        int y = event.getWindow().getGuiScaledHeight() / 2;
        var graphics = event.getGuiGraphics();
        graphics.fill(x - 5, y - 1, x + 6, y + 2, 0xAA000000);
        graphics.fill(x - 1, y - 5, x + 2, y + 6, 0xAA000000);
        graphics.fill(x - 4, y, x + 5, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y - 4, x + 1, y + 5, 0xFFFFFFFF);
    }
}
