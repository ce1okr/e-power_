package com.empoweredsmp.managers;

import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Every `effect-tick-interval-ticks`, re-applies each online player's permanent
 * passive effects and attribute changes based on ability + tier, and enforces
 * an active Prosperity curse. Passive effects are re-applied slightly longer
 * than the tick interval so they never visibly lapse.
 */
public class EffectManager extends BukkitRunnable {

    private final NamespacedKey maxHealthKey;
    private final NamespacedKey sneakKey;
    /** Leftover from the old version (Defense knockback resistance). Cleaned off players if present. */
    private final NamespacedKey legacyKnockbackKey;

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Config cfg;

    /** Elemental players currently carrying the Nether health boost (so we heal once on entry). */
    private final Set<UUID> netherBoosted = new HashSet<>();
    /** Players we silenced (Invisibility High Tier), so we can un-silence them if they stop qualifying. */
    private final Set<UUID> silencedByUs = new HashSet<>();

    public EffectManager(Plugin plugin, AbilityManager abilities, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cfg = cfg;
        this.maxHealthKey = new NamespacedKey(plugin, "max_health_bonus");
        this.sneakKey = new NamespacedKey(plugin, "sneak_speed_bonus");
        this.legacyKnockbackKey = new NamespacedKey(plugin, "kb_resist_bonus");
    }

    public void start() {
        runTaskTimer(plugin, 0L, cfg.effectTickInterval());
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            apply(p);
        }
    }

    /** Silent passive buff: no particles, no HUD icon. */
    private void give(Player p, PotionEffectType type, int amplifier, int durationTicks) {
        p.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false, false));
    }

    /** Curse effects are deliberately visible so the cursed player knows what's happening. */
    private void giveCurse(Player p, PotionEffectType type, int durationTicks) {
        p.addPotionEffect(new PotionEffect(type, durationTicks, 0, false, true, true));
    }

    private void apply(Player p) {
        PlayerData d = abilities.get(p);
        Ability ability = d.getAbility();
        int level = d.getLevel();
        UUID id = p.getUniqueId();
        boolean cursed = d.isCursed(System.currentTimeMillis());
        int dur = cfg.effectTickInterval() + 40; // outlives the gap between ticks

        removeLegacyKnockbackModifier(p);

        // ---- Max health ----
        boolean netherBoost = ability == Ability.ELEMENTAL && level >= 1
                && p.getWorld().getEnvironment() == World.Environment.NETHER;
        int hearts = 10;
        if (ability == Ability.VITALITY) {
            hearts = cfg.vitalityMaxHearts(level);
        } else if (netherBoost) {
            hearts = cfg.elementalNetherHearts(level);
        }
        if (cursed) {
            hearts = cfg.curseHearts();
        }
        setMaxHealth(p, hearts * 2.0);

        // Elemental: fill the extra hearts once when they enter the Nether.
        if (netherBoost && !cursed) {
            if (netherBoosted.add(id)) {
                double gained = hearts * 2.0 - 20.0;
                if (gained > 0) {
                    p.setHealth(Math.min(currentMaxHealth(p), p.getHealth() + gained));
                }
            }
        } else {
            netherBoosted.remove(id);
        }

        // ---- Mobility High: crouch-walking at normal walking speed ----
        setSneakAtWalkingSpeed(p, ability == Ability.MOBILITY && level >= 2);

        // ---- Invisibility High: make zero sound ----
        if (ability == Ability.INVISIBILITY && level >= 2) {
            p.setSilent(true);
            silencedByUs.add(id);
        } else if (silencedByUs.remove(id)) {
            p.setSilent(false);
        }

        // ---- Prosperity curse (applies to anyone who has been cursed): Weakness I + Slowness I + 9 hearts ----
        if (cursed) {
            giveCurse(p, PotionEffectType.WEAKNESS, dur);
            giveCurse(p, PotionEffectType.SLOWNESS, dur);
        }

        if (ability == null) return;

        // ---- Passive effects by ability and tier ----
        switch (ability) {
            case STRENGTH -> {
                if (level >= 1) give(p, PotionEffectType.STRENGTH, level >= 2 ? 1 : 0, dur);
            }
            case DEFENSE -> {
                if (level >= 2) give(p, PotionEffectType.RESISTANCE, 0, dur);
            }
            case MOBILITY -> {
                if (level >= 1) {
                    give(p, PotionEffectType.SPEED, level >= 2 ? 1 : 0, dur);
                    give(p, PotionEffectType.WEAVING, 0, dur);
                }
            }
            case RANGER -> {
                if (level >= 1) give(p, PotionEffectType.SPEED, 0, dur);
            }
            case VITALITY -> {
                // Regeneration I from the Level 0 kit, kept at every tier.
                give(p, PotionEffectType.REGENERATION, 0, dur);
            }
            case ELEMENTAL -> {
                if (level >= 1) {
                    give(p, PotionEffectType.FIRE_RESISTANCE, 0, dur);
                    give(p, PotionEffectType.WATER_BREATHING, 0, dur);
                    give(p, PotionEffectType.HASTE, level >= 2 ? 1 : 0, dur);
                }
                if (level >= 2) {
                    give(p, PotionEffectType.DOLPHINS_GRACE, 0, dur);
                }
            }
            case INVISIBILITY -> {
                if (level >= 1) {
                    give(p, PotionEffectType.NIGHT_VISION, 0, dur);
                    give(p, PotionEffectType.INVISIBILITY, 0, dur);
                    give(p, PotionEffectType.SPEED, 0, dur);
                }
            }
            default -> { }
        }
    }

    private double currentMaxHealth(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr == null ? 20.0 : attr.getValue();
    }

    private void setMaxHealth(Player p, double totalHp) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(maxHealthKey))
                .toList()
                .forEach(attr::removeModifier);
        double delta = totalHp - attr.getBaseValue();
        if (Math.abs(delta) > 0.001) {
            attr.addModifier(new AttributeModifier(maxHealthKey, delta, AttributeModifier.Operation.ADD_NUMBER));
        }
        if (p.getHealth() > attr.getValue()) {
            p.setHealth(attr.getValue());
        }
    }

    /** Vanilla sneaking speed is 0.3 (max 1.0); +0.7 makes crouch-walking as fast as walking. */
    private void setSneakAtWalkingSpeed(Player p, boolean enabled) {
        AttributeInstance attr = p.getAttribute(Attribute.SNEAKING_SPEED);
        if (attr == null) return;
        boolean present = attr.getModifiers().stream().anyMatch(m -> m.getKey().equals(sneakKey));
        if (enabled && !present) {
            attr.addModifier(new AttributeModifier(sneakKey, 0.7, AttributeModifier.Operation.ADD_NUMBER));
        } else if (!enabled && present) {
            attr.getModifiers().stream()
                    .filter(m -> m.getKey().equals(sneakKey))
                    .toList()
                    .forEach(attr::removeModifier);
        }
    }

    private void removeLegacyKnockbackModifier(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(legacyKnockbackKey))
                .toList()
                .forEach(attr::removeModifier);
    }
}
