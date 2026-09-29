package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import com.github.alexthe666.iceandfire.entity.EntityCockatrice;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/** Read-only sex lookup. Missing tags mean unknown, never an implicit female. */
public final class CreatureSex {
    @Nullable
    public static Boolean male(Entity target) {
        for (int i = 0; i < 10; i++) {
            Entity parent = target instanceof EntityMutlipartPart part ? part.getParent()
                    : target instanceof PartEntity<?> part ? part.getParent() : null;
            if (parent == null || parent == target) break;
            target = parent;
        }
        if (!(target instanceof LivingEntity)) return null;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace();
        if (!namespace.equals("iceandfire") && !namespace.equals("iceandfire_bond_beyond")) return null;
        // These getters are backed by the native Gender / Hen saved fields.
        if (target instanceof EntityDragonBase dragon) return dragon.isMale();
        if (target instanceof EntityCockatrice cockatrice) return !cockatrice.isHen();
        Boolean stored = fromTag(target.getPersistentData());
        if (stored != null) return stored;
        // Support other IAF/addon mobs that actually save a recognised field.
        // Only the boolean result is sent to Jade, never the entity's full NBT.
        return fromTag(target.saveWithoutId(new CompoundTag()));
    }

    @Nullable
    public static Boolean fromTag(CompoundTag tag) {
        for (String key : new String[]{"BondBeyondMale", "Gender", "Male", "IsMale"}) {
            if (tag.contains(key, Tag.TAG_BYTE) && (tag.getByte(key) == 0 || tag.getByte(key) == 1))
                return tag.getBoolean(key);
        }
        for (String key : new String[]{"Hen", "Female", "IsFemale"}) {
            if (tag.contains(key, Tag.TAG_BYTE) && (tag.getByte(key) == 0 || tag.getByte(key) == 1))
                return !tag.getBoolean(key);
        }
        return null;
    }

    private CreatureSex() {}
}
