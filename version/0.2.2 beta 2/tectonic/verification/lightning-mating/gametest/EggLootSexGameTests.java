package com.iceandfirebondbeyond.review;

import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.enums.EnumSeaSerpent;
import com.iceandfirebondbeyond.config.BondBeyondConfig;
import com.iceandfirebondbeyond.entity.SeaSerpentCorpseEntity;
import com.iceandfirebondbeyond.registry.ModItems;
import com.iceandfirebondbeyond.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("iceandfire_bond_beyond")
@PrefixGameTestTemplate(false)
public final class EggLootSexGameTests {
    private static EntitySeaSerpent wild(GameTestHelper h, int stage, boolean male, int variant, int x) {
        EntitySeaSerpent s = IafEntityRegistry.SEA_SERPENT.get().create(h.getLevel());
        int ticks = (SeaSerpentGrowth.firstDay(stage) + 3) * 24000;
        CompoundTag nativeTag = new CompoundTag();
        s.addAdditionalSaveData(nativeTag);
        nativeTag.putFloat("Scale", SeaSerpentGrowth.scaleForTicks(ticks));
        nativeTag.putBoolean("Ancient", false); // Age/Stage eligibility must not depend on this flag.
        s.readAdditionalSaveData(nativeTag);
        s.getPersistentData().putInt("BondBeyondGrowthTicks", ticks);
        SeaSerpentBondData.setMale(s, male);
        s.setVariant(variant);s.setNoAi(true);s.setNoGravity(true);
        Vec3 pos = h.absoluteVec(new Vec3(x, 4, 25));s.moveTo(pos.x,pos.y,pos.z,0,0);
        return s;
    }
    private static int eggs(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new net.minecraft.core.BlockPos(0,0,0)),h.absolutePos(new net.minecraft.core.BlockPos(64,16,64))), e -> Arrays.stream(EnumSeaSerpent.values()).anyMatch(v -> e.getItem().is(ModItems.getSeaSerpentEgg(v).getItem())))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }
    @GameTest(batch="egg_jade",template="combat_empty",timeoutTicks=30)
    public static void egg_stage_gender_and_config_matrix(GameTestHelper h) {
        for (int stage=1;stage<=5;stage++) for (boolean male : new boolean[]{true,false}) {
            EntitySeaSerpent s = wild(h,stage,male,0,20);
            double expected = male || stage<3 ? 0 : stage==3 ? .25 : stage==4 ? .5 : 1;
            h.assertTrue(SeaSerpentEggLoot.chance(s)==expected,"default chance for stage "+stage+" male="+male);
            h.assertTrue(!s.isAncient(),"fixture has no Ancient flag");
        }
        EntitySeaSerpent s=wild(h,3,false,0,20);
        double old=BondBeyondConfig.WILD_EGG_STAGE_3.get();
        try {
            BondBeyondConfig.WILD_EGG_STAGE_3.set(0D);h.assertTrue(!SeaSerpentEggLoot.shouldDrop(s),"zero disables");
            BondBeyondConfig.WILD_EGG_STAGE_3.set(1D);h.assertTrue(SeaSerpentEggLoot.shouldDrop(s),"one guarantees without Ancient");
        } finally {BondBeyondConfig.WILD_EGG_STAGE_3.set(old);}
        SeaSerpentBondData.tame(s,UUID.randomUUID());
        h.assertTrue(SeaSerpentEggLoot.chance(s)==0,"owned excluded");
        s=wild(h,5,false,0,20);s.getPersistentData().putBoolean("BondBeyondHatched",true);
        h.assertTrue(SeaSerpentEggLoot.chance(s)==0,"hatched excluded even if unowned");
        h.succeed();
    }
    @GameTest(batch="egg_jade",template="combat_empty",timeoutTicks=180)
    public static void actual_death_drops_one_variant_egg_and_corpse_never_duplicates(GameTestHelper h) {
        for(int x=0;x<64;x++)for(int z=0;z<64;z++)h.setBlock(x,1,z,Blocks.STONE);
        int before=eggs(h);
        double old3=BondBeyondConfig.WILD_EGG_STAGE_3.get(),old4=BondBeyondConfig.WILD_EGG_STAGE_4.get();
        try {
            BondBeyondConfig.WILD_EGG_STAGE_3.set(1D);BondBeyondConfig.WILD_EGG_STAGE_4.set(1D);
            for(int v=0;v<7;v++) {
                EntitySeaSerpent s=wild(h,3+v%3,false,v,5+v*8);h.getLevel().addFreshEntity(s);
                h.assertTrue(s.hurt(h.getLevel().damageSources().generic(),100000),"real lethal damage accepted");
                ItemStack egg=ModItems.getSeaSerpentEgg(s.getEnum());
                h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,s.getBoundingBox().inflate(2),e->e.getItem().is(egg.getItem())).stream()
                        .mapToInt(e->e.getItem().getCount()).sum()==1,"exactly one matching variant on actual death "+v);
            }
        } finally {BondBeyondConfig.WILD_EGG_STAGE_3.set(old3);BondBeyondConfig.WILD_EGG_STAGE_4.set(old4);}
        h.assertTrue(eggs(h)==before+7,"seven deaths produce seven eggs");
        ServerPlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"egg-harvest"));
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        SeaSerpentCorpseEntity[] body={null};
        h.runAfterDelay(60,()->{
            var list=h.getLevel().getEntitiesOfClass(SeaSerpentCorpseEntity.class,new AABB(h.absolutePos(new net.minecraft.core.BlockPos(0,0,0)),h.absolutePos(new net.minecraft.core.BlockPos(64,16,64))),e->e.getStage()==5);
            h.assertTrue(!list.isEmpty(),"actual dead serpent becomes harvestable corpse");
            CompoundTag saved=new CompoundTag();list.get(0).save(saved);
            body[0]=new SeaSerpentCorpseEntity(com.iceandfirebondbeyond.registry.SeaSteelContent.CORPSE.get(),h.getLevel());
            body[0].load(saved);list.get(0).discard();
        });
        for(int i=0;i<25;i++)h.runAfterDelay(64+i*4,()->body[0].interact(p,InteractionHand.MAIN_HAND));
        h.runAfterDelay(168,()->{h.assertTrue(body[0].isRemoved(),"all flesh/bone/skull harvest steps completed after reload");
            h.assertTrue(eggs(h)==before+7,"corpse reload/harvesting never rerolls eggs");h.succeed();});
    }
    @GameTest(batch="egg_jade",template="combat_empty",timeoutTicks=30)
    public static void actual_no_egg_for_male_young_or_disabled_mob_loot(GameTestHelper h) {
        int before=eggs(h);
        for(int stage=1;stage<=5;stage++) {
            EntitySeaSerpent s=wild(h,stage,true,0,5+stage*8);h.getLevel().addFreshEntity(s);
            s.hurt(h.getLevel().damageSources().generic(),100000);
        }
        for(int stage=1;stage<=2;stage++) {
            EntitySeaSerpent s=wild(h,stage,false,0,5+stage*8);h.getLevel().addFreshEntity(s);
            s.hurt(h.getLevel().damageSources().generic(),100000);
        }
        boolean rule=h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        try {
            h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
            EntitySeaSerpent s=wild(h,5,false,0,40);h.getLevel().addFreshEntity(s);
            s.hurt(h.getLevel().damageSources().generic(),100000);
        } finally {h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(rule,h.getLevel().getServer());}
        h.assertTrue(eggs(h)==before,"male/young/doMobLoot=false cannot drop hunting eggs");h.succeed();
    }
    @GameTest(batch="egg_jade",template="combat_empty",timeoutTicks=30)
    public static void sex_reader_uses_native_and_saved_fields_without_assigning(GameTestHelper h) {
        h.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("jade") != Boolean.getBoolean("bondbeyond.review.without_jade"),
                "optional dependency presence matches actual test runtime");
        for(var type:List.of(IafEntityRegistry.FIRE_DRAGON.get(),IafEntityRegistry.ICE_DRAGON.get(),IafEntityRegistry.LIGHTNING_DRAGON.get())) {
            EntityDragonBase d=(EntityDragonBase)type.create(h.getLevel());
            for(boolean male:new boolean[]{true,false}){d.setGender(male);h.assertTrue(CreatureSex.male(d)==male,"dragon native Gender");}
        }
        EntityCockatrice c=IafEntityRegistry.COCKATRICE.get().create(h.getLevel());
        for(boolean hen:new boolean[]{true,false}){c.setHen(hen);h.assertTrue(CreatureSex.male(c)==!hen,"Hen true is FEMALE");}
        EntitySeaSerpent s=wild(h,3,false,0,20);
        h.assertTrue(Boolean.FALSE.equals(CreatureSex.male(s)),"explicit false is female, not missing");
        SeaSerpentBondData.setMale(s,true);h.assertTrue(Boolean.TRUE.equals(CreatureSex.male(s)),"serpent male persistent data");
        s.getPersistentData().remove("BondBeyondMale");s.getPersistentData().remove("BondBeyondGenderSet");
        CompoundTag before=s.getPersistentData().copy();
        h.assertTrue(CreatureSex.male(s)==null && before.equals(s.getPersistentData()),"missing sex remains unknown without random assignment");
        h.assertTrue(CreatureSex.male(IafEntityRegistry.HYDRA.get().create(h.getLevel()))==null,"genderless IAF mob omitted");
        var cow=EntityType.COW.create(h.getLevel());cow.getPersistentData().putBoolean("Male",true);
        h.assertTrue(CreatureSex.male(cow)==null,"unrelated namespace unaffected");
        CompoundTag malformed=new CompoundTag();malformed.putString("Gender","female");
        h.assertTrue(CreatureSex.fromTag(malformed)==null,"unknown schema not guessed");
        malformed.putByte("Gender",(byte)2);h.assertTrue(CreatureSex.fromTag(malformed)==null,"enum value not treated as boolean");
        h.succeed();
    }
}
