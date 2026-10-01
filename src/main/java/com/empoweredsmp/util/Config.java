package com.empoweredsmp.util;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed access to config.yml. Per-tier values live under "low" / "high" keys
 * (and "level0" where a Level 0 value exists), e.g. strength.main.duration-seconds.low.
 */
public class Config {

    private final FileConfiguration c;

    public Config(FileConfiguration c) {
        this.c = c;
    }

    private static String tierKey(int level) {
        return level >= 2 ? "high" : level == 1 ? "low" : "level0";
    }

    private int tierInt(String path, int level, int def) {
        return c.getInt(path + "." + tierKey(level), def);
    }

    private double tierDouble(String path, int level, double def) {
        return c.getDouble(path + "." + tierKey(level), def);
    }

    public int effectTickInterval() { return c.getInt("effect-tick-interval-ticks", 20); }

    // ---- enemies (players who recently attacked you) ----
    public int enemyMemorySeconds() { return c.getInt("enemies.memory-seconds", 30); }
    public int enemyRadius() { return c.getInt("enemies.radius", 20); }

    // ---- item caps ----
    public int capGoldenApplesNormal() { return c.getInt("caps.golden-apples-normal", 64); }
    public int capGoldenApplesVitality() { return c.getInt("caps.golden-apples-vitality", 96); }
    public int capEnchantedGoldenApples(int level) {
        return tierInt("caps.enchanted-golden-apples", level, level >= 2 ? 3 : 2);
    }
    public int capXpBottlesNormal() { return c.getInt("caps.xp-bottles-normal", 128); }
    public int capXpBottlesProsperityStacks() { return c.getInt("caps.xp-bottles-prosperity-stacks", 3); }
    public int capTotemsNormal() { return c.getInt("caps.totems-normal", 1); }
    public int capTotemsProsperity() { return c.getInt("caps.totems-prosperity", 2); }
    public int capCobwebsNormal() { return c.getInt("caps.cobwebs-normal", 64); }
    public int capCobwebsMobility() { return c.getInt("caps.cobwebs-mobility", 128); }
    public int capWindCharges() { return c.getInt("caps.wind-charges", 128); }
    public int capEnderPearlsMax() { return c.getInt("caps.ender-pearls-max", 6); }
    public int enderPearlCooldownSeconds() { return c.getInt("caps.ender-pearl-cooldown-seconds", 30); }

    // ---- enchant caps ----
    public int protectionNormal() { return c.getInt("enchant-caps.protection-normal", 3); }
    public int protectionVitality() { return c.getInt("enchant-caps.protection-vitality", 4); }
    public int sharpnessNormal() { return c.getInt("enchant-caps.sharpness-normal", 4); }
    public int sharpnessStrength() { return c.getInt("enchant-caps.sharpness-strength", 5); }
    public int powerNormal() { return c.getInt("enchant-caps.power-normal", 4); }
    public int powerRanger() { return c.getInt("enchant-caps.power-ranger", 5); }

    // ---- strength ----
    public double strengthHitChance(int level) { return tierDouble("strength.hit-multiplier-chance", level, 0.25); }
    public double strengthHitMultiplier() { return c.getDouble("strength.hit-multiplier", 1.5); }
    public double strengthMainTriggerHearts() { return c.getDouble("strength.main.trigger-hearts", 4); }
    public int strengthMainWeaknessAmp() { return c.getInt("strength.main.weakness-amplifier", 4); }
    public int strengthMainDuration(int level) { return tierInt("strength.main.duration-seconds", level, 15); }
    public int strengthMainCooldown(int level) { return tierInt("strength.main.cooldown-seconds", level, 60); }

    // ---- defense ----
    public double defenseZeroChance(int level) { return tierDouble("defense.zero-damage-chance", level, 0.15); }
    public double defenseReflectChance(int level) { return tierDouble("defense.reflect-chance", level, 0.15); }
    public double defenseReflectMultiplier() { return c.getDouble("defense.reflect-multiplier", 2.0); }
    public double defenseMainTriggerHearts() { return c.getDouble("defense.main.trigger-hearts", 5); }
    public int defenseMainWeaknessAmp() { return c.getInt("defense.main.weakness-amplifier", 1); }
    public int defenseMainSlownessAmp() { return c.getInt("defense.main.slowness-amplifier", 0); }
    public int defenseMainDuration(int level) { return tierInt("defense.main.duration-seconds", level, 15); }
    public int defenseMainCooldown(int level) { return tierInt("defense.main.cooldown-seconds", level, 60); }

    // ---- mobility ----
    public int mobilityLungeCooldownSeconds() { return c.getInt("mobility.lunge-cooldown-seconds", 15); }
    public double mobilityMainTriggerHearts() { return c.getDouble("mobility.main.trigger-hearts", 4); }
    public int mobilityMainSlownessAmp() { return c.getInt("mobility.main.slowness-amplifier", 2); }
    public int mobilityMainFatigueAmp() { return c.getInt("mobility.main.mining-fatigue-amplifier", 0); }
    public int mobilityMainDuration(int level) { return tierInt("mobility.main.duration-seconds", level, 30); }
    public int mobilityMainCooldown(int level) { return tierInt("mobility.main.cooldown-seconds", level, 120); }

    // ---- ranger ----
    public double rangerDamageBonus(int level) { return tierDouble("ranger.damage-bonus", level, 0.30); }
    public int rangerGlowingArrowSeconds() { return c.getInt("ranger.glowing-arrow-seconds", 30); }
    public int rangerHitsRequired() { return c.getInt("ranger.main.hits-required", 3); }
    public int rangerSlownessAmp() { return c.getInt("ranger.main.slowness-amplifier", 9); }
    public int rangerFatigueAmp() { return c.getInt("ranger.main.mining-fatigue-amplifier", 2); }
    public int rangerMainDuration(int level) { return tierInt("ranger.main.duration-seconds", level, 15); }
    public int rangerMainCooldown(int level) { return tierInt("ranger.main.cooldown-seconds", level, 60); }

    // ---- vitality ----
    public int vitalityMaxHearts(int level) {
        return tierInt("vitality.max-hearts", level, level >= 2 ? 15 : level == 1 ? 13 : 11);
    }
    public double vitalityMainTriggerHearts() { return c.getDouble("vitality.main.trigger-hearts", 4); }
    public int vitalityRegenAmp() { return c.getInt("vitality.main.regeneration-amplifier", 4); }
    public int vitalityMainDuration(int level) { return tierInt("vitality.main.duration-seconds", level, 10); }
    public int vitalityMainCooldown(int level) { return tierInt("vitality.main.cooldown-seconds", level, 60); }

    // ---- elemental ----
    public int elementalNetherHearts(int level) {
        return tierInt("elemental.nether-hearts", level, level >= 2 ? 16 : 13);
    }

    // ---- invisibility ----
    public double invisMainTriggerHearts() { return c.getDouble("invisibility.main.trigger-hearts", 4); }
    public int invisMainDuration(int level) { return tierInt("invisibility.main.duration-seconds", level, 30); }
    public int invisMainCooldown(int level) { return tierInt("invisibility.main.cooldown-seconds", level, 90); }

    // ---- prosperity ----
    public double prosperityXpMultiplier(int level) { return tierDouble("prosperity.xp-multiplier", level, 1.5); }
    public double prosperityPotionMultiplier(int level) { return tierDouble("prosperity.potion-multiplier", level, 1.5); }
    public int prosperityExtraInvSlots(int level) {
        return level <= 0 ? 0 : tierInt("prosperity.extra-inventory-slots", level, level >= 2 ? 27 : 18);
    }
    public double prosperityTradeDiscountLow() { return c.getDouble("prosperity.trade-discount-low", 0.5); }
    public int curseCooldownHours(int level) { return tierInt("prosperity.curse.cooldown-hours", level, level >= 2 ? 3 : 5); }
    public int curseDurationHours() { return c.getInt("prosperity.curse.duration-hours", 2); }
    public int curseHearts() { return c.getInt("prosperity.curse.hearts", 9); }
}
