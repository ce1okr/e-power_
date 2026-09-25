package com.empoweredsmp.util;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    private final FileConfiguration c;

    public Config(FileConfiguration c) {
        this.c = c;
    }

    public int effectTickInterval() { return c.getInt("effect-tick-interval-ticks", 20); }

    // caps
    public int capGoldenApplesNormal() { return c.getInt("caps.golden-apples-normal", 96); }
    public int capGoldenApplesVitality() { return c.getInt("caps.golden-apples-vitality", 128); }
    public int capEnchantedGoldenApplesVitality() { return c.getInt("caps.enchanted-golden-apples-vitality-max", 5); }
    public int capXpBottlesNormal() { return c.getInt("caps.xp-bottles-normal", 128); }
    public int capXpBottlesProsperityStacks() { return c.getInt("caps.xp-bottles-prosperity-stacks", 3); }
    public int capTotemsNormal() { return c.getInt("caps.totems-normal", 1); }
    public int capTotemsProsperityL3() { return c.getInt("caps.totems-prosperity-level3", 2); }
    public int capCobwebsNormal() { return c.getInt("caps.cobwebs-normal", 64); }
    public int capCobwebsMobility() { return c.getInt("caps.cobwebs-mobility", 128); }
    public int capWindCharges() { return c.getInt("caps.wind-charges", 128); }
    public int capEnderPearlsMax() { return c.getInt("caps.ender-pearls-max", 6); }
    public int enderPearlCooldownSeconds() { return c.getInt("caps.ender-pearl-cooldown-seconds", 30); }

    // enchant caps
    public int protectionNormal() { return c.getInt("enchant-caps.protection-normal", 3); }
    public int protectionVitalityL1() { return c.getInt("enchant-caps.protection-vitality-level1", 4); }
    public int sharpnessNormal() { return c.getInt("enchant-caps.sharpness-normal", 4); }
    public int sharpnessStrengthL2() { return c.getInt("enchant-caps.sharpness-strength-level2", 5); }
    public int powerNormal() { return c.getInt("enchant-caps.power-normal", 4); }
    public int powerRangerL1() { return c.getInt("enchant-caps.power-ranger-level1", 5); }

    // vitality
    public int vitalityMaxHearts(int level) {
        return c.getInt("vitality.max-hearts-by-level." + level, 10);
    }
    public int heartSurgeTriggerHearts() { return c.getInt("vitality.heart-surge.trigger-hearts", 3); }
    public int heartSurgeRestoreHearts() { return c.getInt("vitality.heart-surge.restore-to-hearts", 16); }
    public int heartSurgeCooldownSeconds() { return c.getInt("vitality.heart-surge.cooldown-seconds", 240); }

    // invisibility
    public int invisTriggerHearts() { return c.getInt("invisibility.full-invis.trigger-hearts", 4); }
    public int invisCooldownSeconds() { return c.getInt("invisibility.full-invis.cooldown-seconds", 45); }
    public int invisDurationSeconds() { return c.getInt("invisibility.full-invis.duration-seconds", 8); }

    // defense
    public double defenseKnockbackResistL1() { return c.getDouble("defense.knockback-resistance-level1", 0.25); }
    public int defenseResurgeTriggerHearts() { return c.getInt("defense.resistance-surge.trigger-hearts", 3); }
    public int defenseResurgeAmplifier() { return c.getInt("defense.resistance-surge.amplifier", 4); }
    public int defenseResurgeDurationSeconds() { return c.getInt("defense.resistance-surge.duration-seconds", 20); }
    public int defenseResurgeCooldownSeconds() { return c.getInt("defense.resistance-surge.cooldown-seconds", 100); }

    // strength
    public double strengthL1CritChance() { return c.getDouble("strength.level1-crit-chance", 0.20); }
    public double strengthL2CritChance() { return c.getDouble("strength.level2-crit-chance", 0.45); }
    public int strengthSurgeTriggerHearts() { return c.getInt("strength.strength-surge.trigger-hearts", 4); }
    public int strengthSurgeAmplifier() { return c.getInt("strength.strength-surge.amplifier", 3); }
    public int strengthSurgeDurationSeconds() { return c.getInt("strength.strength-surge.duration-seconds", 10); }
    public int strengthSurgeCooldownSeconds() { return c.getInt("strength.strength-surge.cooldown-seconds", 80); }

    // mobility
    public int mobilityAuraTriggerHearts() { return c.getInt("mobility.level3-aura.trigger-hearts", 4); }
    public int mobilityAuraRadius() { return c.getInt("mobility.level3-aura.radius", 20); }
    public int mobilityAuraSlownessAmp() { return c.getInt("mobility.level3-aura.slowness-amplifier", 4); }
    public int mobilityAuraFatigueAmp() { return c.getInt("mobility.level3-aura.mining-fatigue-amplifier", 9); }
    public int mobilityAuraDurationSeconds() { return c.getInt("mobility.level3-aura.duration-seconds", 20); }
    public int mobilityAuraCooldownSeconds() { return c.getInt("mobility.level3-aura.cooldown-seconds", 120); }

    // ranger
    public double rangerL1DamageBonus() { return c.getDouble("ranger.level1-damage-bonus", 0.20); }
    public double rangerL2DamageBonus() { return c.getDouble("ranger.level2-damage-bonus", 0.40); }
    public int rangerSlownessAmp() { return c.getInt("ranger.slowness-on-fifth-hit.amplifier", 9); }
    public int rangerSlownessDurationSeconds() { return c.getInt("ranger.slowness-on-fifth-hit.duration-seconds", 15); }

    // elemental
    public int elementalGlowRadius() { return c.getInt("elemental.level3-glow-radius", 50); }

    // potion multiplier
    public double potionMultiplierL1() { return c.getDouble("potion-effect-duration-multiplier.level1", 1.5); }
    public double potionMultiplierL2() { return c.getDouble("potion-effect-duration-multiplier.level2", 2.0); }

    // prosperity
    public int prosperityExtraInvSlots(int level) {
        return c.getInt("prosperity.extra-inventory-slots." + level, 0);
    }
    public double prosperityXpMultiplier() { return c.getDouble("prosperity.xp-multiplier", 2.0); }
    public double prosperityHeroDiscount(int level) {
        return c.getDouble("prosperity.hero-of-village-discount." + level, 0.0);
    }
}
