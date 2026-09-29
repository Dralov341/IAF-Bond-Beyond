package com.iceandfirebondbeyond.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class SeaSerpentKeys {
    public static final KeyMapping BITE = new KeyMapping(
            "key.iceandfire_bond_beyond.sea_serpent_bite", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.iceandfire_bond_beyond");

    private SeaSerpentKeys() {}

    /**
     * Read the configured action independently of other held movement keys.
     * IAF's bindings use the default UNIVERSAL context; Forge can filter an
     * unmodified R/G/X out while Ctrl/Alt/Shift is held. Poll the bound device
     * as well, so sprint/swim chords still work without changing IAF's bindings.
     * An explicitly configured modifier (e.g. Ctrl+R) is still required.
     * Call only while the game window is focused and no screen is open.
     */
    public static boolean isHeld(KeyMapping mapping, long window) {
        if (mapping == null || !mapping.getKeyConflictContext().isActive()) {
            return false;
        }
        KeyModifier modifier = mapping.getKeyModifier();
        if (modifier != KeyModifier.NONE
                && !modifier.isActive(mapping.getKeyConflictContext())) {
            return false;
        }
        InputConstants.Key key = mapping.getKey();
        if (key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (mapping.isDown()) {
            return true; // Also supports input supplied by controller mods.
        }
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }
}
