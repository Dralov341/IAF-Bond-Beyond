package com.iceandfirebondbeyond.util;

import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/** Server-only owner attack/defense selection, shared by the target guard and movement arbitration. */
public final class SeaSerpentOwnerAssist {
    private record State(UUID owner, SeaSerpentOwnerTargetMemory<LivingEntity> memory) {}
    private static final Map<EntitySeaSerpent, State> STATES = new WeakHashMap<>();
    private static final class Signals {
        int attackAt = Integer.MIN_VALUE, defenseAt = Integer.MIN_VALUE;
        WeakReference<LivingEntity> attacked = new WeakReference<>(null), attacker = new WeakReference<>(null);
    }
    private static final Map<Player, Signals> SIGNALS = new WeakHashMap<>();
    private SeaSerpentOwnerAssist() {}
    /** Forge attack events include arrows, modded damage and clicks on IAF multipart entities. */
    public static void record(Player owner, Entity other, boolean defense) {
        if (owner.level().isClientSide || !owner.isAlive() || owner.isSpectator()) return;
        Entity root = SeaSerpentCombat.damageTarget(other);
        if (!(root instanceof LivingEntity target) || root == owner || !root.isAlive()) return;
        Signals signals = SIGNALS.computeIfAbsent(owner, key -> new Signals());
        if (defense) { signals.defenseAt = owner.tickCount; signals.attacker = new WeakReference<>(target); }
        else { signals.attackAt = owner.tickCount; signals.attacked = new WeakReference<>(target); }
    }
    @Nullable public static LivingEntity requested(EntitySeaSerpent serpent) {
        if (serpent.level().isClientSide) return null;
        Player owner = SeaSerpentBondData.getOwner(serpent);
        if (owner == null || !owner.isAlive() || owner.isSpectator() || serpent.isVehicle() || serpent.isPassenger()) {
            STATES.remove(serpent); return null;
        }
        State state = STATES.get(serpent);
        if (state == null || !state.owner.equals(owner.getUUID())) {
            state = new State(owner.getUUID(), new SeaSerpentOwnerTargetMemory<>()); STATES.put(serpent, state);
        }
        Signals signals = SIGNALS.get(owner);
        int attackAt = owner.getLastHurtMobTimestamp(), defenseAt = owner.getLastHurtByMobTimestamp();
        LivingEntity attacked = owner.getLastHurtMob(), attacker = owner.getLastHurtByMob();
        if (signals != null) {
            if (signals.attackAt >= attackAt) { attackAt = signals.attackAt; attacked = signals.attacked.get(); }
            if (signals.defenseAt >= defenseAt) { defenseAt = signals.defenseAt; attacker = signals.attacker.get(); }
        }
        return state.memory.update(owner.tickCount, attackAt, attacked, defenseAt, attacker, target ->
                        target.level() == serpent.level() && target.distanceToSqr(serpent) <= 128 * 128
                        && owner.distanceToSqr(serpent) <= 128 * 128
                        && !(target instanceof Player player && player.isCreative())
                        && (!(target instanceof Player player) || (owner.canHarmPlayer(player)
                        && serpent.getServer() != null && serpent.getServer().isPvpAllowed()))
                        && SeaSerpentCombat.canAttack(serpent, target, true));
    }
}
