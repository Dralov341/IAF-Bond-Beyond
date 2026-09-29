package com.iceandfirebondbeyond.review;

import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.iceandfirebondbeyond.compat.jade.BondBeyondJadePlugin;
import com.iceandfirebondbeyond.util.SeaSerpentBondData;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.gametest.*;
import snownee.jade.api.*;
import java.lang.reflect.Proxy;
import java.util.*;

@GameTestHolder("iceandfire_bond_beyond")
@PrefixGameTestTemplate(false)
public final class JadeSexGameTests {
    @GameTest(batch="egg_jade",template="combat_empty",timeoutTicks=30)
    public static void jade_registration_payload_and_tooltip(GameTestHelper h) {
        List<Object[]> registrations=new ArrayList<>();
        var registration=(IWailaCommonRegistration)Proxy.newProxyInstance(IWailaCommonRegistration.class.getClassLoader(),new Class[]{IWailaCommonRegistration.class},
                (proxy,method,args)->{if(method.getName().equals("registerEntityDataProvider"))registrations.add(args);return null;});
        new BondBeyondJadePlugin().register(registration);
        h.assertTrue(registrations.size()==1 && registrations.get(0)[1]==Entity.class,"provider includes native multipart entities");
        Entity[] target={IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel())};
        SeaSerpentBondData.setMale((com.github.alexthe666.iceandfire.entity.EntitySeaSerpent)target[0],false);
        CompoundTag data=new CompoundTag();
        var accessor=(EntityAccessor)Proxy.newProxyInstance(EntityAccessor.class.getClassLoader(),new Class[]{EntityAccessor.class},
                (proxy,method,args)->switch(method.getName()){case "getEntity"->target[0];case "getServerData"->data;default->null;});
        var provider=BondBeyondJadePlugin.SexProvider.INSTANCE;
        provider.appendServerData(data,accessor);
        h.assertTrue(data.getAllKeys().equals(Set.of("BondBeyondJadeMale")) && !data.getBoolean("BondBeyondJadeMale"),"only one sex boolean sent, no private/full NBT");
        List<Component> lines=new ArrayList<>();
        var tooltip=(ITooltip)Proxy.newProxyInstance(ITooltip.class.getClassLoader(),new Class[]{ITooltip.class},
                (proxy,method,args)->{if(method.getName().equals("add") && args[0] instanceof Component c)lines.add(c);return null;});
        provider.appendTooltip(tooltip,accessor,null);
        h.assertTrue(lines.size()==1 && Component.Serializer.toJson(lines.get(0)).contains("jade.iceandfire_bond_beyond.female"),"female text is appended");
        target[0]=IafEntityRegistry.HYDRA.get().create(h.getLevel());
        provider.appendServerData(data,accessor);lines.clear();provider.appendTooltip(tooltip,accessor,null);
        h.assertTrue(!data.contains("BondBeyondJadeMale") && lines.isEmpty(),"genderless target cannot retain stale previous tooltip");
        h.succeed();
    }
}
