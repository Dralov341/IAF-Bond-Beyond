package com.iceandfirebondbeyond.compat.jade;

import com.iceandfirebondbeyond.util.CreatureSex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Discovered by Jade only. No core class references this optional adapter. */
@WailaPlugin
public final class BondBeyondJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(SexProvider.INSTANCE, Entity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(SexProvider.INSTANCE, Entity.class);
    }

    public enum SexProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;
        private static final String KEY = "BondBeyondJadeMale";
        private static final ResourceLocation UID = new ResourceLocation("iceandfire_bond_beyond", "creature_sex");

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            data.remove(KEY);
            Boolean male = CreatureSex.male(accessor.getEntity());
            if (male != null) data.putBoolean(KEY, male);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY, Tag.TAG_BYTE)) return;
            tooltip.add(Component.translatable("jade.iceandfire_bond_beyond.sex",
                    Component.translatable("jade.iceandfire_bond_beyond." + (data.getBoolean(KEY) ? "male" : "female"))));
        }

        @Override
        public ResourceLocation getUid() { return UID; }
    }
}
