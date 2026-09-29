package com.iceandfirebondbeyond.util;

/** Per-entity AI cadence; stopping/restarting a goal never resets its cooldowns. */
public final class SeaSerpentAttackCycle {
    private long nextBite;
    private long nextShot;
    private boolean pendingBite;
    private boolean breathing;

    public boolean startBite(long now) {
        if (pendingBite || now < nextBite) return false;
        nextBite = now + 20L;
        pendingBite = true;
        breathing = false;
        return true;
    }

    public boolean consumeBite(int animationTick) {
        if (!pendingBite || animationTick < 6) return false;
        pendingBite = false;
        return true;
    }

    public void cancelBite() { pendingBite = false; }

    public boolean updateBreath(boolean permitted, double distance, double biteReach) {
        // Different enter/exit thresholds avoid switching modes every tick.
        breathing = permitted && !pendingBite
                && distance > biteReach + (breathing ? 3.0D : 6.0D);
        return breathing;
    }

    public boolean takeShot(long now, int interval) {
        if (!breathing || now < nextShot) return false;
        nextShot = now + Math.max(1, interval);
        return true;
    }

    public boolean isBreathing() { return breathing; }
    public void stop() { breathing = false; pendingBite = false; }
}
