package com.iceandfirebondbeyond.review;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.iceandfirebondbeyond.entity.SeaSerpentEggEntity;
import com.iceandfirebondbeyond.event.CommonEvents;
import com.iceandfirebondbeyond.network.LightningArcPacket;
import com.iceandfirebondbeyond.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("iceandfire_bond_beyond")
@PrefixGameTestTemplate(false)
public final class LightningMatingGameTests {
    private static final ResourceLocation CHANNEL=new ResourceLocation("iceandfire_bond_beyond","main");
    private static final class Observer extends FakePlayer {
        final List<LightningArcPacket> packets=new ArrayList<>();
        Observer(ServerLevel level) {
            super(level,new GameProfile(UUID.randomUUID(),"lightning-check"));
            connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND) {
                @Override public void send(Packet<?> packet) { capture(packet); }
                @Override public void send(Packet<?> packet,PacketSendListener callback) { capture(packet); }
                private void capture(Packet<?> packet) {
                    if(packet instanceof ClientboundCustomPayloadPacket custom && custom.getIdentifier().equals(CHANNEL)) {
                        FriendlyByteBuf data=new FriendlyByteBuf(custom.getData().copy());
                        try {if(data.readVarInt()==4)packets.add(LightningArcPacket.decode(data));} finally {data.release();}
                    }
                }
            },this);
        }
    }
    private static Observer player(GameTestHelper h) {
        var bounds=new AABB(h.absolutePos(new net.minecraft.core.BlockPos(0,0,0)),h.absolutePos(new net.minecraft.core.BlockPos(64,16,64)));
        for(var old:h.getLevel().getEntitiesOfClass(LivingEntity.class,bounds,e->bounds.contains(e.position())))old.discard();
        for(int x=0;x<64;x++)for(int z=0;z<64;z++){h.setBlock(x,1,z,Blocks.STONE);for(int y=2;y<10;y++)h.setBlock(x,y,z,Blocks.AIR);}
        Observer p=new Observer(h.getLevel());Vec3 pos=h.absoluteVec(new Vec3(10,2,20));
        p.moveTo(pos.x,pos.y,pos.z,0,0);p.setNoGravity(true);h.getLevel().addNewPlayer(p);return p;
    }
    private static LivingEntity target(GameTestHelper h,int x,int z) {
        var t=h.spawnWithNoFreeWill(EntityType.IRON_GOLEM,x,2,z);t.setNoGravity(true);return t;
    }
    private static ItemStack item(String id) {return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
    private static LightningArcPacket one(GameTestHelper h,Observer p) {
        h.assertTrue(p.packets.size()==1,"exactly one arc packet for this hit; actual="+p.packets.size());
        return p.packets.get(0);
    }
    private static void cleanup(Observer p) {p.getServer().getPlayerList().remove(p);p.discard();}

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void native_and_external_registry_and_tag_detection(GameTestHelper h) {
        String[] nativeIds={"dragonsteel_lightning_sword","dragonsteel_lightning_axe","dragonsteel_lightning_pickaxe","dragonsteel_lightning_hoe","dragonbone_sword_lightning"};
        for(String id:nativeIds)h.assertTrue(LightningWeapons.isLightning(item("iceandfire:"+id)),"native "+id);
        h.assertTrue(LightningWeapons.isLightning(item("bond_test:lightning_hammer")),"another namespace, no IAF class");
        h.assertTrue(LightningWeapons.isLightning(new ItemStack(Items.IRON_AXE)),"explicit datapack tag enables arbitrary item");
        h.assertTrue(!LightningWeapons.isLightning(item("bond_test:lightning_excluded_sword")),"exclude tag takes precedence");
        h.assertTrue(!LightningWeapons.isLightning(item("iceandfire:dragonsteel_lightning_shovel")),"exclude overrides native lightning tier");
        for(String id:new String[]{"minecraft:lightning_rod","minecraft:diamond_sword","iceandfire:dragonsteel_lightning_ingot","iceandfire:dragonsteel_fire_sword","iceandfire:dragonbone_sword_ice"})
            h.assertTrue(!LightningWeapons.isLightning(item(id)),"not a lightning weapon: "+id);
        ItemStack renamed=new ItemStack(Items.DIAMOND_SWORD);renamed.setHoverName(net.minecraft.network.chat.Component.literal("Lightning Sword"));
        h.assertTrue(!LightningWeapons.isLightning(renamed),"anvil name cannot grant lightning");
        if(net.minecraftforge.fml.ModList.get().isLoaded("spartanfire")) {
            int count=0;
            for(var entry:ForgeRegistries.ITEMS.getEntries())if(entry.getKey().location().getNamespace().equals("spartanfire")&&entry.getKey().location().getPath().contains("lightning")) {
                ItemStack s=new ItemStack(entry.getValue());
                h.assertTrue(LightningWeapons.isLightning(s),"real SpartanFire item "+entry.getKey().location());count++;
            }
            h.assertTrue(count>=20,"real addon lightning arsenal tested, count="+count);
        }
        h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void lethal_hit_still_sends_first_arc_and_chains(GameTestHelper h) {
        Observer p=player(h);var first=target(h,14,20);var next=target(h,18,20);
        p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));
        Vec3 firstPos=first.getBoundingBox().getCenter();
        h.assertTrue(first.hurt(h.getLevel().damageSources().playerAttack(p),10000),"actual lethal melee");
        h.assertTrue(!first.isAlive(),"first enemy died before arc processing");
        DragonSteelLightning.flush(h.getLevel());var packet=one(h,p);
        h.assertTrue(packet.arcs().size()==2&&packet.arcs().get(0).end().equals(firstPos),"lethal primary always visible and chain continues");
        h.assertTrue(next.getHealth()<next.getMaxHealth(),"next enemy receives lightning damage");
        cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void native_callback_and_event_do_not_duplicate_chain(GameTestHelper h) {
        Observer p=player(h);var first=target(h,14,20);var next=target(h,18,20);
        ItemStack sword=item("iceandfire:dragonsteel_lightning_sword");p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        h.assertTrue(first.hurt(h.getLevel().damageSources().playerAttack(p),5),"actual damage event");
        sword.getItem().hurtEnemy(sword,first,p); // native mixin reports same successful attack
        DragonSteelLightning.flush(h.getLevel());
        h.assertTrue(one(h,p).arcs().size()==2,"one native+generic chain");
        DragonSteelLightning.flush(h.getLevel());h.assertTrue(p.packets.size()==1,"draining twice cannot repeat hit");
        cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void chains_use_impact_range_cap_and_allied_los_guards(GameTestHelper h) {
        Observer p=player(h);var first=target(h,30,20);var near=target(h,40,20);var far=target(h,46,20);
        var pet=h.spawnWithNoFreeWill(EntityType.WOLF,31,2,21);pet.setOwnerUUID(p.getUUID());pet.setTame(true);
        var hidden=target(h,30,26);for(int x=26;x<35;x++)for(int y=2;y<8;y++)h.setBlock(x,y,24,Blocks.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));
        first.hurt(h.getLevel().damageSources().playerAttack(p),1);DragonSteelLightning.flush(h.getLevel());
        var packet=one(h,p);h.assertTrue(packet.arcs().size()==2,"impact range includes near target but excludes pet, wall, far target");
        h.assertTrue(near.getHealth()<near.getMaxHealth()&&far.getHealth()==far.getMaxHealth()&&hidden.getHealth()==hidden.getMaxHealth()&&pet.getHealth()==pet.getMaxHealth(),"chain protection and range");
        cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void projectile_keeps_launch_weapon_and_far_shooter_gets_packet(GameTestHelper h) {
        Observer p=player(h);var victim=target(h,40,20);
        p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_bow"));
        Arrow arrow=new Arrow(h.getLevel(),p);h.getLevel().addFreshEntity(arrow);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
        p.setPos(p.getX()-120,p.getY(),p.getZ());
        h.assertTrue(victim.hurt(h.getLevel().damageSources().arrow(arrow,p),5),"real projectile damage source");
        DragonSteelLightning.flush(h.getLevel());
        h.assertTrue(one(h,p).arcs().size()==1,"projectile retained lightning weapon after switching and sends to distant shooter");
        arrow.discard();cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void cancelled_and_non_lightning_hits_emit_nothing(GameTestHelper h) {
        Observer p=player(h);var victim=target(h,14,20);
        p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));
        Consumer<LivingDamageEvent> veto=e->{if(e.getEntity()==victim)e.setCanceled(true);};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH,false,LivingDamageEvent.class,veto);
        try {victim.hurt(h.getLevel().damageSources().playerAttack(p),5);} finally {MinecraftForge.EVENT_BUS.unregister(veto);}
        DragonSteelLightning.flush(h.getLevel());h.assertTrue(p.packets.isEmpty(),"canceled damage does not produce arcs");
        victim.invulnerableTime=0;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
        victim.hurt(h.getLevel().damageSources().playerAttack(p),5);DragonSteelLightning.flush(h.getLevel());
        h.assertTrue(p.packets.isEmpty(),"ordinary melee does not proc");
        ItemStack excluded=item("iceandfire:dragonsteel_lightning_shovel");p.setItemInHand(InteractionHand.MAIN_HAND,excluded);
        victim.invulnerableTime=0;victim.hurt(h.getLevel().damageSources().playerAttack(p),5);excluded.getItem().hurtEnemy(excluded,victim,p);
        DragonSteelLightning.flush(h.getLevel());h.assertTrue(p.packets.isEmpty(),"native callback respects exclusion tag too");
        boolean enabled=IafConfig.dragonWeaponLightningAbility;IafConfig.dragonWeaponLightningAbility=false;
        try {victim.invulnerableTime=0;p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));victim.hurt(h.getLevel().damageSources().playerAttack(p),5);DragonSteelLightning.flush(h.getLevel());}
        finally {IafConfig.dragonWeaponLightningAbility=enabled;}
        h.assertTrue(p.packets.isEmpty(),"IAF ability config remains respected");cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void attributed_weapon_bolt_replaced_but_natural_lightning_survives(GameTestHelper h) {
        Observer p=player(h);var victim=target(h,14,20);p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));
        victim.hurt(h.getLevel().damageSources().playerAttack(p),2);
        LightningBolt weapon=EntityType.LIGHTNING_BOLT.create(h.getLevel());weapon.moveTo(victim.position());weapon.getTags().add(p.getStringUUID());weapon.getTags().add(com.github.alexthe666.iceandfire.event.ServerEvents.BOLT_DONT_DESTROY_LOOT);
        h.assertTrue(!h.getLevel().addFreshEntity(weapon),"addon weapon sky bolt suppressed for pending chain");
        LightningBolt natural=EntityType.LIGHTNING_BOLT.create(h.getLevel());natural.moveTo(victim.position());
        h.assertTrue(h.getLevel().addFreshEntity(natural),"unattributed natural lightning untouched");natural.discard();
        DragonSteelLightning.flush(h.getLevel());one(h,p);cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void real_spartan_weapon_uses_arc_without_sky_bolt(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("spartanfire")){h.succeed();return;}
        Observer p=player(h);var first=target(h,14,20);var next=target(h,18,20);
        ItemStack sword=item("spartanfire:lightning_dragonsteel_longsword");
        h.assertTrue(!sword.isEmpty(),"real Spartan longsword fixture");p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        first.hurt(h.getLevel().damageSources().playerAttack(p),5);sword.getItem().hurtEnemy(sword,first,p);
        DragonSteelLightning.flush(h.getLevel());h.assertTrue(one(h,p).arcs().size()==2,"Spartan hit produces connected arcs");
        h.assertTrue(h.getLevel().getEntitiesOfClass(LightningBolt.class,first.getBoundingBox().inflate(4)).isEmpty(),"no duplicate sky lightning");
        cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void chain_caps_at_ten_and_never_randomly_drops_primary(GameTestHelper h) {
        Observer p=player(h);List<LivingEntity> mobs=new ArrayList<>();
        for(int i=0;i<14;i++)mobs.add(target(h,24+i%5*2,18+i/5*2));
        p.setItemInHand(InteractionHand.MAIN_HAND,item("bond_test:lightning_hammer"));
        for(int i=0;i<40;i++) {
            for(var mob:mobs){mob.setHealth(mob.getMaxHealth());mob.invulnerableTime=0;}
            p.packets.clear();mobs.get(0).hurt(h.getLevel().damageSources().playerAttack(p),1);
            DragonSteelLightning.flush(h.getLevel());
            var arcs=one(h,p).arcs();h.assertTrue(arcs.size()==10,"every eligible hit chains to the ten-target cap");
            h.assertTrue(arcs.stream().map(LightningArcPacket.Arc::end).distinct().count()==10,"no repeat targets");
        }
        cleanup(p);h.succeed();
    }

    @GameTest(batch="lightning",template="combat_empty",timeoutTicks=30)
    public static void real_spartan_thrown_weapon_retains_item_after_throw(GameTestHelper h) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("spartanfire")){h.succeed();return;}
        Observer p=player(h);var victim=target(h,20,20);
        ItemStack stack=item("spartanfire:lightning_dragonsteel_javelin");h.assertTrue(!stack.isEmpty(),"real javelin fixture");
        Class<?> type=Class.forName("com.oblivioussp.spartanweaponry.entity.projectile.JavelinEntity");
        var thrown=(net.minecraft.world.entity.projectile.AbstractArrow)type.getConstructor(net.minecraft.world.level.Level.class,LivingEntity.class).newInstance(h.getLevel(),p);
        type.getMethod("setWeapon",ItemStack.class).invoke(thrown,stack);
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.getLevel().addFreshEntity(thrown);
        h.assertTrue(LightningWeapons.carriedWeapon(thrown).is(stack.getItem()),"optional public API resolves exact thrown weapon");
        victim.hurt(h.getLevel().damageSources().arrow(thrown,p),5);DragonSteelLightning.flush(h.getLevel());
        h.assertTrue(one(h,p).arcs().size()==1,"thrown lightning javelin still emits first arc with empty hand");
        thrown.discard();cleanup(p);h.succeed();
    }

    @GameTest(batch="mating_colors",template="combat_empty",timeoutTicks=60)
    public static void breeding_all_parent_colors_and_order_preserves_one_parent(GameTestHelper h) throws Exception {
        Observer p=player(h);
        EntitySeaSerpent a=IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel()),b=IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel());
        SeaSerpentBondData.markAsHatchedBaby(a,false);SeaSerpentBondData.markAsHatchedBaby(b,true);
        Vec3 pos=h.absoluteVec(new Vec3(32,4,32));a.moveTo(pos);b.moveTo(pos.add(10,0,0));
        var lay=CommonEvents.class.getDeclaredMethod("layBreedingEgg",ServerLevel.class,EntitySeaSerpent.class,EntitySeaSerpent.class,net.minecraft.world.entity.player.Player.class);lay.setAccessible(true);
        a.getRandom().setSeed(87654321L);
        for(int mother=0;mother<7;mother++)for(int father=0;father<7;father++)for(int order=0;order<2;order++) {
            a.setVariant(mother);b.setVariant(father);Set<EnumSeaSerpent> seen=new HashSet<>();
            for(int trial=0;trial<32;trial++) {
                SeaSerpentEggEntity egg=(SeaSerpentEggEntity)lay.invoke(null,h.getLevel(),order==0?a:b,order==0?b:a,p);
                h.assertTrue(egg!=null,"actual breeding egg created");
                h.assertTrue(egg.getVariant()==a.getEnum()||egg.getVariant()==b.getEnum(),"offspring must inherit either parent only");
                seen.add(egg.getVariant());h.assertTrue(p.getUUID().equals(egg.getOwnerId()),"owner preserved");egg.discard();
            }
            h.assertTrue(seen.size()==(mother==father?1:2),"both parents selectable in either interaction order");
        }
        cleanup(p);h.succeed();
    }
}
