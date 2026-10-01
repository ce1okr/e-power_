package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.managers.EnemyTracker;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hit-based abilities:
 *  - Strength: a % of melee hits do 1.5x damage.
 *  - Defense: a % of hits taken do 0 damage; a % reflect double damage onto the player who hit you.
 *  - Ranger: stronger shots, 30-second glowing arrows, and the "every 3rd hit" Main Ability.
 * Also records who attacked whom for EnemyTracker.
 */
public class CombatListener implements Listener {

    private static final String RANGER_COOLDOWN_TAG = "ranger_main";

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final CooldownManager cooldowns;
    private final EnemyTracker enemies;
    private final Config cfg;

    /** "shooterId:targetId" -> arrow hits toward the next Ranger Main Ability proc. */
    private final Map<String, Integer> rangerHitCounts = new HashMap<>();
    /** Players currently taking reflected damage; they don't roll Defense on it (prevents ping-pong). */
    private final Set<UUID> reflectedTargets = new HashSet<>();

    public CombatListener(Plugin plugin, AbilityManager abilities, CooldownManager cooldowns,
                          EnemyTracker enemies, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cooldowns = cooldowns;
        this.enemies = enemies;
        this.cfg = cfg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = EnemyTracker.resolveAttacker(event.getDamager());
        Entity victim = event.getEntity();
        Player victimPlayer = (victim instanceof Player pl) ? pl : null;

        // Remember who attacked whom, so "your enemies" works.
        if (victimPlayer != null && attacker != null && !attacker.equals(victimPlayer)) {
            enemies.recordHit(victimPlayer, attacker);
        }

        // ---- Strength: a % of melee hits do 1.5x damage ----
        if (event.getDamager() instanceof Player melee && abilities.isStrengthAtLeast(melee, 1)) {
            double chance = cfg.strengthHitChance(abilities.levelOf(melee));
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                event.setDamage(event.getDamage() * cfg.strengthHitMultiplier());
            }
        }

        // ---- Ranger: stronger shots + Main Ability ----
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter
                && abilities.isRangerAtLeast(shooter, 1)) {
            int level = abilities.levelOf(shooter);
            event.setDamage(event.getDamage() * (1.0 + cfg.rangerDamageBonus(level)));
            if (victimPlayer != null && projectile instanceof AbstractArrow && !victimPlayer.equals(shooter)) {
                handleRangerHit(shooter, victimPlayer, level);
            }
        }

        // ---- Defense: 0-damage hits and reflected hits ----
        if (victimPlayer != null
                && !reflectedTargets.contains(victimPlayer.getUniqueId())
                && abilities.isDefenseAtLeast(victimPlayer, 1)) {
            int level = abilities.levelOf(victimPlayer);
            double incoming = event.getFinalDamage();
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            boolean zero = rng.nextDouble() < cfg.defenseZeroChance(level);
            boolean reflect = attacker != null && !attacker.equals(victimPlayer)
                    && rng.nextDouble() < cfg.defenseReflectChance(level);

            if (zero) {
                event.setCancelled(true);
            }
            if (reflect) {
                double reflected = incoming * cfg.defenseReflectMultiplier();
                final Player target = attacker;
                final Player source = victimPlayer;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!target.isOnline() || target.isDead()) return;
                    reflectedTargets.add(target.getUniqueId());
                    try {
                        target.damage(reflected, source);
                    } finally {
                        reflectedTargets.remove(target.getUniqueId());
                    }
                });
            }
            if (zero && reflect) {
                victimPlayer.sendActionBar(Component.text("Blocked and reflected!", NamedTextColor.AQUA));
            } else if (zero) {
                victimPlayer.sendActionBar(Component.text("Blocked!", NamedTextColor.AQUA));
            } else if (reflect) {
                victimPlayer.sendActionBar(Component.text("Reflected!", NamedTextColor.AQUA));
            }
        }
    }

    /** Every Nth arrow hit on the same player: Slowness X + Glowing + Mining Fatigue III. */
    private void handleRangerHit(Player shooter, Player target, int level) {
        UUID shooterId = shooter.getUniqueId();
        if (cooldowns.isOnCooldown(shooterId, RANGER_COOLDOWN_TAG)) return;

        String key = shooterId + ":" + target.getUniqueId();
        int count = rangerHitCounts.merge(key, 1, Integer::sum);
        if (count < cfg.rangerHitsRequired()) return;
        rangerHitCounts.remove(key);

        int ticks = cfg.rangerMainDuration(level) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, cfg.rangerSlownessAmp(), false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, ticks, 0, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, cfg.rangerFatigueAmp(), false, true, true));

        cooldowns.set(shooterId, RANGER_COOLDOWN_TAG, cfg.rangerMainCooldown(level));
        shooter.sendActionBar(Component.text("Ranger: " + target.getName() + " is pinned down!", NamedTextColor.GOLD));
    }

    /** Ranger: spectral (glowing) arrows apply Glowing for 30 seconds instead of 10. */
    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof SpectralArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!abilities.isRangerAtLeast(shooter, 1)) return;
        arrow.setGlowingTicks(cfg.rangerGlowingArrowSeconds() * 20);
    }
}
