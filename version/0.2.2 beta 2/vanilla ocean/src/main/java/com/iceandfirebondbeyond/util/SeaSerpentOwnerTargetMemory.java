package com.iceandfirebondbeyond.util;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;

/** Consume each owner combat event once; keep fighting after vanilla's short hurt memory expires. */
public final class SeaSerpentOwnerTargetMemory<T> {
    private int attackSeen = Integer.MIN_VALUE, defenseSeen = Integer.MIN_VALUE;
    private WeakReference<T> selected = new WeakReference<>(null);
    private WeakReference<T> attackTargetSeen = new WeakReference<>(null), defenseTargetSeen = new WeakReference<>(null);
    public T update(int now, int attackTime, T attacked, int defenseTime, T attacker, Predicate<T> valid) {
        boolean attack = (attackTime != attackSeen || attacked != attackTargetSeen.get()) && fresh(now, attackTime) && attacked != null && valid.test(attacked);
        boolean defense = (defenseTime != defenseSeen || attacker != defenseTargetSeen.get()) && fresh(now, defenseTime) && attacker != null && valid.test(attacker);
        attackSeen = attackTime; defenseSeen = defenseTime;
        if (attacked != attackTargetSeen.get()) attackTargetSeen = new WeakReference<>(attacked);
        if (attacker != defenseTargetSeen.get()) defenseTargetSeen = new WeakReference<>(attacker);
        if (attack || defense) selected = new WeakReference<>(defense && (!attack || defenseTime >= attackTime) ? attacker : attacked);
        T target = selected.get();
        if (target != null && !valid.test(target)) { selected.clear(); return null; }
        return target;
    }
    private static boolean fresh(int now, int event) { return event >= 0 && event <= now && (long) now - event <= 200; }
}
