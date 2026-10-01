package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.managers.EnemyTracker;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

/**
 * The "below N hearts" Main Abilities for Strength, Defense, Mobility, Vitality
 * and Invisibility (Low and High Tier). Checked after every hit, once the damage
 * has been applied. Ranger's Main Ability is in CombatListener (it fires on
 * arrow hits), Elemental's is the Nether health boost in EffectManager, and
 * Prosperity's is /curse.
 *
 * Abilities that hit enemies only fire (and only start their cooldown) when
 * there is someone to hit.
 */
public class MainAbilityListener implements Listener {

    private static final String COOLDOWN_TAG = "main_ability";

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final CooldownManager cooldowns;
    private final EnemyTracker enemies;
    private final Config cfg;

    public MainAbilityListener(Plugin plugin, AbilityManager abilities, CooldownManager cooldowns,
                               EnemyTracker enemies, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cooldowns = cooldowns;
        this.enemies = enemies;
        this.cfg = cfg;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            attacker = EnemyTracker.resolveAttacker(byEntity.getDamager());
        }
        final Player finalAttacker = attacker;

        // Run next tick so p.getHealth() reflects the hit.
        Bukkit.getScheduler().runTask(plugin, () -> evaluate(p, finalAttacker));
    }

    private void evaluate(Player p, Player attacker) {
        if (!p.isOnline() || p.isDead()) return;

        PlayerData d = abilities.get(p);
        Ability ability = d.getAbility();
        int level = d.getLevel();
        if (ability == null || level < 1) return;

        UUID id = p.getUniqueId();
        if (cooldowns.isOnCooldown(id, COOLDOWN_TAG)) return;
        double hearts = p.getHealth() / 2.0;

        switch (ability) {
            case STRENGTH -> {
                if (hearts >= cfg.strengthMainTriggerHearts()) return;
                List<Player> targets = enemies.enemiesOf(p);
                if (targets.isEmpty()) return;
                int seconds = cfg.strengthMainDuration(level);
                for (Player t : targets) {
                    effect(t, PotionEffectType.WEAKNESS, seconds, cfg.strengthMainWeaknessAmp());
                }
                activate(p, cfg.strengthMainCooldown(level), "Strength: your enemies are weakened!");
            }
            case DEFENSE -> {
                if (hearts >= cfg.defenseMainTriggerHearts()) return;
                if (attacker == null || !attacker.isOnline() || attacker.isDead()) return;
                int seconds = cfg.defenseMainDuration(level);
                effect(attacker, PotionEffectType.WEAKNESS, seconds, cfg.defenseMainWeaknessAmp());
                effect(attacker, PotionEffectType.GLOWING, seconds, 0);
                effect(attacker, PotionEffectType.SLOWNESS, seconds, cfg.defenseMainSlownessAmp());
                activate(p, cfg.defenseMainCooldown(level), "Defense: " + attacker.getName() + " is vulnerable!");
            }
            case MOBILITY -> {
                if (hearts >= cfg.mobilityMainTriggerHearts()) return;
                List<Player> targets = enemies.enemiesOf(p);
                if (targets.isEmpty()) return;
                int seconds = cfg.mobilityMainDuration(level);
                for (Player t : targets) {
                    effect(t, PotionEffectType.SLOWNESS, seconds, cfg.mobilityMainSlownessAmp());
                    effect(t, PotionEffectType.MINING_FATIGUE, seconds, cfg.mobilityMainFatigueAmp());
                }
                activate(p, cfg.mobilityMainCooldown(level), "Mobility: your enemies are slowed!");
            }
            case VITALITY -> {
                if (hearts >= cfg.vitalityMainTriggerHearts()) return;
                effect(p, PotionEffectType.REGENERATION, cfg.vitalityMainDuration(level), cfg.vitalityRegenAmp());
                activate(p, cfg.vitalityMainCooldown(level), "Vitality: Regeneration surge!");
            }
            case INVISIBILITY -> {
                if (hearts >= cfg.invisMainTriggerHearts()) return;
                List<Player> targets = enemies.enemiesOf(p);
                if (targets.isEmpty()) return;
                int seconds = cfg.invisMainDuration(level);
                for (Player t : targets) {
                    effect(t, PotionEffectType.BLINDNESS, seconds, 0);
                    effect(t, PotionEffectType.NAUSEA, seconds, 0);
                    effect(t, PotionEffectType.DARKNESS, seconds, 0);
                }
                activate(p, cfg.invisMainCooldown(level), "Invisibility: your enemies are blinded!");
            }
            default -> { }
        }
    }

    /** Visible effect (particles + icon) so the target can tell what hit them. */
    private void effect(Player target, PotionEffectType type, int seconds, int amplifier) {
        target.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, false, true, true));
    }

    private void activate(Player p, int cooldownSeconds, String message) {
        cooldowns.set(p.getUniqueId(), COOLDOWN_TAG, cooldownSeconds);
        p.sendActionBar(Component.text(message, NamedTextColor.GOLD));
    }
}
