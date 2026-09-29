package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.item.DragonSteelTier;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.Optional;

/** No optional-mod classes: native tiers, datapack tags, then registry-name fallback. */
public final class LightningWeapons {
    private static final ClassValue<Optional<Method>> SPARTAN_THROWN_ITEM = new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            for (Class<?> base = type; base != null; base = base.getSuperclass()) {
                if (base.getName().equals("com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity")) {
                    try { return Optional.of(base.getMethod("getWeaponItem")); }
                    catch (NoSuchMethodException ignored) { return Optional.empty(); }
                }
            }
            return Optional.empty();
        }
    };
    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM,
            new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "lightning_weapons"));
    public static final TagKey<Item> EXCLUDED = TagKey.create(Registries.ITEM,
            new ResourceLocation(IceAndFireBondBeyond.MOD_ID, "lightning_weapons_excluded"));

    public static boolean isLightning(ItemStack stack) {
        if (stack.isEmpty() || stack.is(EXCLUDED)) return false;
        if (stack.is(WEAPONS)) return true;
        Item item = stack.getItem();
        if (item == IafItemRegistry.DRAGONBONE_SWORD_LIGHTNING.get()) return true;
        if (item instanceof TieredItem tool
                && (tool.getTier() == DragonSteelTier.DRAGONSTEEL_TIER_LIGHTNING
                || tool.getTier() == IafItemRegistry.LIGHTNING_DRAGONBONE_TOOL_MATERIAL)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !hasLightningToken(id.getPath())) return false;
        // Do not turn ingots, armor, eggs, shields or renamed mundane items into weapons.
        if (item instanceof ArmorItem || item instanceof ShieldItem) return false;
        return item instanceof TieredItem || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem
                || !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    /** Optional public Spartan API, resolved only on an already-loaded projectile class. */
    public static ItemStack carriedWeapon(Projectile projectile) {
        if (projectile instanceof ThrowableItemProjectile thrown) return thrown.getItem();
        Optional<Method> accessor = SPARTAN_THROWN_ITEM.get(projectile.getClass());
        if (accessor.isPresent()) {
            try {
                Object value = accessor.get().invoke(projectile);
                if (value instanceof ItemStack stack) return stack;
            } catch (ReflectiveOperationException ignored) { /* Unsupported API revision: no false attribution. */ }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasLightningToken(String path) {
        for (String token : path.split("[_/.-]")) {
            if (token.equals("lightning") || token.equals("thunder") || token.equals("electric")) return true;
        }
        return false;
    }

    private LightningWeapons() {}
}
