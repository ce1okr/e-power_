package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Below-threshold-hearts abilities: Vitality Heart Surge, Invisibility Full
 * Invis, Defense Resistance Surge, Strength Strength Surge, Mobility area
 * slow/fatigue aura. All evaluated on EntityDamageEvent (monitor priority,
 * after damage is applied) so the check reflects post-hit health.
 */
public class SurgeListener implements Listener {

    private final AbilityManager abilities;
    private final CooldownManager cooldowns;
    private final Config cfg;
    private final Plugin plugin;

    public SurgeListener(Plugin plugin, AbilityManager abilities, CooldownManager cooldowns, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cooldowns = cooldowns;
        this.cfg = cfg;
    }

    private double heartsOf(Player p) {
        return p.getHealth() / 2.0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (p.isDead()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || p.isDead()) return;
            double hearts = heartsOf(p);

            if (abilities.isVitalityAtLeast(p, 3) && hearts <= cfg.heartSurgeTriggerHearts()
                    && !cooldowns.isOnCooldown(p.getUniqueId(), "heart_surge")) {
                cooldowns.set(p.getUniqueId(), "heart_surge", cfg.heartSurgeCooldownSeconds());
                double targetHp = cfg.heartSurgeRestoreHearts() * 2.0;
                var attr = p.getAttribute(Attribute.MAX_HEALTH);
                double cap = attr != null ? attr.getValue() : targetHp;
                p.setHealth(Math.min(targetHp, cap));
            }

            if (abilities.isInvisibilityAtLeast(p, 3) && hearts <= cfg.invisTriggerHearts()
                    && !cooldowns.isOnCooldown(p.getUniqueId(), "full_invis")) {
                cooldowns.set(p.getUniqueId(), "full_invis", cfg.invisCooldownSeconds());
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                        cfg.invisDurationSeconds() * 20, 0, true, false, false));
                // Note: hiding armor/held items fully requires client-side packet trickery
                // or a resource pack; vanilla invisibility already hides the entity model.
            }

            if (abilities.isDefenseAtLeast(p, 3) && hearts <= cfg.defenseResurgeTriggerHearts()
                    && !cooldowns.isOnCooldown(p.getUniqueId(), "resistance_surge")) {
                cooldowns.set(p.getUniqueId(), "resistance_surge", cfg.defenseResurgeCooldownSeconds());
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                        cfg.defenseResurgeDurationSeconds() * 20, cfg.defenseResurgeAmplifier(), true, true, true));
            }

            if (abilities.isStrengthAtLeast(p, 3) && hearts <= cfg.strengthSurgeTriggerHearts()
                    && !cooldowns.isOnCooldown(p.getUniqueId(), "strength_surge")) {
                cooldowns.set(p.getUniqueId(), "strength_surge", cfg.strengthSurgeCooldownSeconds());
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                        cfg.strengthSurgeDurationSeconds() * 20, cfg.strengthSurgeAmplifier(), true, true, true));
            }

            if (abilities.isMobilityAtLeast(p, 3) && hearts <= cfg.mobilityAuraTriggerHearts()
                    && !cooldowns.isOnCooldown(p.getUniqueId(), "mobility_aura")) {
                cooldowns.set(p.getUniqueId(), "mobility_aura", cfg.mobilityAuraCooldownSeconds());
                for (Entity nearby : p.getNearbyEntities(cfg.mobilityAuraRadius(), cfg.mobilityAuraRadius(), cfg.mobilityAuraRadius())) {
                    if (nearby instanceof LivingEntity enemy && !nearby.equals(p)) {
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                                cfg.mobilityAuraDurationSeconds() * 20, cfg.mobilityAuraSlownessAmp()));
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,
                                cfg.mobilityAuraDurationSeconds() * 20, cfg.mobilityAuraFatigueAmp()));
                    }
                }
            }
        });
    }
}
