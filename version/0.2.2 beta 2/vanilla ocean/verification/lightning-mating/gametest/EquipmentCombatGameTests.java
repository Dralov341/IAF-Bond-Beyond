package com.iceandfirebondbeyond.review;

import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.misc.IafDamageRegistry;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.iceandfirebondbeyond.event.CommonEvents;
import com.iceandfirebondbeyond.inventory.SeaSerpentMenu;
import com.iceandfirebondbeyond.registry.*;
import com.iceandfirebondbeyond.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("iceandfire_bond_beyond")
@PrefixGameTestTemplate(false)
public final class EquipmentCombatGameTests {
    private static ServerPlayer owner(GameTestHelper h) {
        // GameTest batches reuse arena coordinates. Remove earlier fixture mobs whose
        // centers are inside this arena before checking giant-serpent egg placement.
        var bounds=new net.minecraft.world.phys.AABB(h.absolutePos(new net.minecraft.core.BlockPos(0,0,0)),h.absolutePos(new net.minecraft.core.BlockPos(64,16,64)));
        for(var old:h.getLevel().getEntitiesOfClass(LivingEntity.class,bounds,e->bounds.contains(e.position())))old.discard();
        for(int x=0;x<64;x++)for(int z=0;z<64;z++){h.setBlock(x,1,z,Blocks.STONE);for(int y=2;y<10;y++)h.setBlock(x,y,z,Blocks.AIR);}
        ServerPlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"equipment-audit")) { @Override public void displayClientMessage(net.minecraft.network.chat.Component text, boolean actionBar) { System.out.println("BREEDING_REVIEW_MESSAGE: "+text.getString()); } };
        Vec3 at=h.absoluteVec(new Vec3(20,2,20));p.moveTo(at.x,at.y,at.z,0,0);h.getLevel().addNewPlayer(p);return p;
    }
    private static EntitySeaSerpent serpent(GameTestHelper h,ServerPlayer p,int days,boolean male,int x) {
        EntitySeaSerpent s=IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel());
        SeaSerpentBondData.markAsHatchedBaby(s,male);SeaSerpentBondData.addGrowthDays(s,days);SeaSerpentBondData.tame(s,p.getUUID());
        SeaSerpentBondData.setCommand(s,1);s.setNoAi(true);Vec3 at=h.absoluteVec(new Vec3(x,2,24));s.moveTo(at.x,at.y,at.z,0,0);h.getLevel().addFreshEntity(s);return s;
    }
    @GameTest(template="combat_empty",timeoutTicks=30)
    public static void armor_inventory_shift_click_and_owner_guards(GameTestHelper h) {
        ServerPlayer p=owner(h);EntitySeaSerpent s=serpent(h,p,55,false,22);
        ItemStack armor=new ItemStack(ModItems.SEA_SERPENT_ARMOR_SEA_SERPENT_STEEL.get());armor.getOrCreateTag().putInt("CustomArmorData",42);
        p.getInventory().setItem(9,armor);SeaSerpentMenu menu=new SeaSerpentMenu(1,p.getInventory(),s,"owner");
        h.assertTrue(menu.stillValid(p),"owner may open nearby inventory");menu.quickMoveStack(p,1);
        h.assertTrue(p.getInventory().getItem(9).isEmpty()&&SeaSerpentBondData.getArmorStack(s).getTag().getInt("CustomArmorData")==42,"shift-click equips exact armor NBT");
        menu.quickMoveStack(p,0);h.assertTrue(SeaSerpentBondData.getArmorStack(s).isEmpty(),"shift-click removes persistent armor");
        int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ModItems.SEA_SERPENT_ARMOR_SEA_SERPENT_STEEL.get()))count+=p.getInventory().getItem(i).getCount();
        h.assertTrue(count==1,"armor returns once, no duplication");
        SeaSerpentBondData.tame(s,UUID.randomUUID());h.assertTrue(!menu.stillValid(p),"ownership change closes access");h.succeed();
    }
    @GameTest(template="combat_empty",timeoutTicks=30)
    public static void elemental_damage_and_matching_armor(GameTestHelper h) {
        ServerPlayer p=owner(h);EntitySeaSerpent s=serpent(h,p,75,false,22);
        var ice=IafDamageRegistry.causeDragonIceDamage(p);var lightning=IafDamageRegistry.causeDragonLightningDamage(p);
        h.assertTrue(ElementalCombat.incomingMultiplier(s,ice)==1.3F&&ElementalCombat.incomingMultiplier(s,lightning)==1.6F,"unarmored serpent weaknesses");
        LivingHurtEvent hit=new LivingHurtEvent(s,ice,10);MinecraftForge.EVENT_BUS.post(hit);h.assertTrue(Math.abs(hit.getAmount()-13)<.001,"actual Forge damage hook applies ice multiplier");
        SeaSerpentBondData.setArmorStack(s,new ItemStack(ModItems.SEA_SERPENT_ARMOR_DRAGONSTEEL_ICE.get()));
        h.assertTrue(ElementalCombat.incomingMultiplier(s,ice)==1&&ElementalCombat.incomingMultiplier(s,lightning)==1.6F,"ice armor cancels only ice weakness");
        SeaSerpentBondData.setArmorStack(s,new ItemStack(ModItems.SEA_SERPENT_ARMOR_DRAGONSTEEL_LIGHTNING.get()));h.assertTrue(ElementalCombat.incomingMultiplier(s,lightning)==1,"lightning armor cancels lightning weakness");
        var fire=IafEntityRegistry.FIRE_DRAGON.get().create(h.getLevel());var cold=IafEntityRegistry.ICE_DRAGON.get().create(h.getLevel());var bolt=IafEntityRegistry.LIGHTNING_DRAGON.get().create(h.getLevel());
        h.assertTrue(ElementalCombat.breathMultiplier(fire)==1.6F&&ElementalCombat.breathMultiplier(cold)==1.5F&&ElementalCombat.breathMultiplier(bolt)==1,"water outgoing multipliers include normal lightning damage");
        for(var dragon:new EntityDragonBase[]{fire,cold,bolt}) {
            dragon.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DRAGONARMOR_SEASTEEL_HEAD.get()));
            dragon.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.DRAGONARMOR_SEASTEEL_NECK.get()));
            dragon.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.DRAGONARMOR_SEASTEEL_BODY.get()));
            dragon.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.DRAGONARMOR_SEASTEEL_TAIL.get()));
            h.assertTrue(ElementalCombat.breathMultiplier(dragon)==1,"complete sea steel set cancels water weakness");
        }
        fire.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);h.assertTrue(ElementalCombat.breathMultiplier(fire)==1.6F,"partial set does not cancel water weakness");h.succeed();
    }
    @GameTest(template="combat_empty",timeoutTicks=55)
    public static void shields_and_drowning_tick(GameTestHelper h) {
        ServerPlayer p=owner(h);var target=h.spawnWithNoFreeWill(EntityType.IRON_GOLEM,20,2,23);
        Item[] shields={SeaSteelContent.SHIELD.get(),SeaSteelContent.FIRE_SHIELD.get(),SeaSteelContent.ICE_SHIELD.get(),SeaSteelContent.LIGHTNING_SHIELD.get()};
        for(int i=0;i<shields.length;i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shields[i]));p.startUsingItem(InteractionHand.MAIN_HAND);
            p.getPersistentData().remove("BondBeyondNextShieldRetaliation");target.invulnerableTime=0;
            MinecraftForge.EVENT_BUS.post(new ShieldBlockEvent(p,h.getLevel().damageSources().mobAttack(target),5));
            if(i==0)h.assertTrue(target.hasEffect(SeaSteelContent.DROWNING.get()),"water shield gives Drowning");
            if(i==1)h.assertTrue(target.isOnFire(),"fire shield ignites attacker");
            if(i==2)h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"ice shield applies frozen slowdown");
            if(i==3)h.assertTrue(target.getHealth()<target.getMaxHealth(),"lightning shield causes real damage");
            p.stopUsingItem();
        }
        target.clearFire();float health=target.getHealth();
        h.runAfterDelay(45,()->{h.assertTrue(target.getHealth()<health,"Drowning damages a dry target over real server ticks");h.succeed();});
    }
    @GameTest(batch="breeding_integration",template="combat_empty",timeoutTicks=100)
    public static void breeding_egg_and_cooldown(GameTestHelper h) {
        // This batch is beyond the initial spawn tickets. Make its fixture chunks
        // entity-ticking before spawning the parents; a FakePlayer has no chunk tickets.
        var origin=h.absolutePos(new net.minecraft.core.BlockPos(0,0,0));
        for(int dx=-1;dx<=5;dx++)for(int dz=-1;dz<=5;dz++)
            h.getLevel().setChunkForced((origin.getX()>>4)+dx,(origin.getZ()>>4)+dz,true);
        h.runAfterDelay(20, () -> {
        ServerPlayer p=owner(h);var female=serpent(h,p,125,false,28);var male=serpent(h,p,125,true,42);
        // Newly loaded arena chunks publish their entities on the next server tick.
        h.runAfterDelay(3, () -> {
        for(var s:new EntitySeaSerpent[]{female,male}) {
            SeaSerpentBondData.setCommand(s,SeaSerpentBondData.COMMAND_STAND);
            for(int i=0;i<100&&!SeaSerpentBondData.hasReachedHeartHealthCap(s);i++)SeaSerpentBondData.increaseMaxHealthWithHeart(s);
            SeaSerpentBondData.markBreedingTutorialShown(s);s.setAge(0);
            h.assertTrue(SeaSerpentBondData.isReadyForBreeding(s),"opposite-sex stage-five owned pair ready");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.HEART_OF_THE_SEA,2));
        for(int first=0;first<3;first++)for(int second=0;second<3;second++) {
            if(first==0&&second==0)continue;
            SeaSerpentBondData.setCommand(female,first);SeaSerpentBondData.setCommand(male,second);
            CommonEvents.interact(female,p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getMainHandItem().getCount()==2&&female.getAge()==0&&male.getAge()==0,"Stay/Follow on either parent consumes neither Heart nor cooldown");
            h.assertTrue(h.getLevel().getEntitiesOfClass(SeaSerpentEggEntity.class,female.getBoundingBox().inflate(35)).isEmpty(),"both parents must be Wander to create an egg");
        }
        SeaSerpentBondData.setCommand(female,0);SeaSerpentBondData.setCommand(male,0);
        h.assertTrue(SeaSerpentBondData.isReadyForBreeding(female)&&SeaSerpentBondData.isReadyForBreeding(male),"both still ready after command switching");
        h.assertTrue(h.getLevel().getEntitiesOfClass(EntitySeaSerpent.class,female.getBoundingBox().inflate(32)).contains(male),"partner is visible to world entity search after chunk loading");
        CommonEvents.interact(female,p,InteractionHand.MAIN_HAND);
        var eggs=h.getLevel().getEntitiesOfClass(SeaSerpentEggEntity.class,female.getBoundingBox().inflate(35));
        h.assertTrue(eggs.size()==1&&p.getMainHandItem().getCount()==1,"one breeding egg and one heart consumed; eggs="+eggs.size()+" hearts="+p.getMainHandItem().getCount());
        h.assertTrue(female.getAge()==SeaSerpentBondData.BREEDING_COOLDOWN_TICKS&&male.getAge()==female.getAge(),"both parents receive breeding cooldown");
        CommonEvents.interact(female,p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1&&eggs.get(0).getOwnerId().equals(p.getUUID()),"repeat click does not consume heart during cooldown");h.succeed();
        });
        });
    }
    @GameTest(template="combat_empty",timeoutTicks=50)
    public static void mounted_input_owner_finite_and_timeout(GameTestHelper h) {
        ServerPlayer p=owner(h);var s=serpent(h,p,55,false,22);
        SeaSerpentBondData.setArmorStack(s,new ItemStack(ModItems.SEA_SERPENT_ARMOR_IRON.get()));p.startRiding(s,true);
        SeaSerpentBondData.acceptRiderInput(s,p,2,-2,true,false,true,false,10000,10000);
        var input=SeaSerpentBondData.getRiderInput(s);h.assertTrue(input.forward()==1&&input.strafe()==-1,"server clamps input axes");
        h.assertTrue(input.cameraDistance()<=SeaSerpentAim.maxDistance(s.getSeaSerpentScale()),"camera offset is bounded");
        SeaSerpentBondData.acceptRiderInput(s,p,Float.NaN,0,false,false,false,false,0,0);h.assertTrue(SeaSerpentBondData.getRiderInput(s).forward()==1,"nonfinite input rejected");
        h.runAfterDelay(35,()->{h.assertTrue(SeaSerpentBondData.getRiderInput(s).forward()==0&&!SeaSerpentBondData.getRiderInput(s).breath(),"stale input expires and stops attack");h.succeed();});
    }
}
