package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.util.Config;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {

    private static final java.util.Set<Material> SWORDS = java.util.Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD,
            Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

    private final AbilityManager abilities;
    private final Config cfg;

    /** shooter+target pair -> consecutive ranger-arrow hit count, for the every-5th-shot slowness. */
    private final Map<String, Integer> rangerHitCounts = new HashMap<>();

    public CombatListener(AbilityManager abilities, Config cfg) {
        this.abilities = abilities;
        this.cfg = cfg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Melee (Strength)
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            ItemStack hand = attacker.getInventory().getItemInMainHand();
            if (SWORDS.contains(hand.getType())) {
                if (abilities.isStrengthAtLeast(attacker, 1)) {
                    victim.setFireTicks(Math.max(victim.getFireTicks(), 8 * 20)); // Fire Aspect II ~ 8s
                    double chance = abilities.isStrengthAtLeast(attacker, 2)
                            ? cfg.strengthL2CritChance() : cfg.strengthL1CritChance();
                    if (ThreadLocalRandom.current().nextDouble() < chance) {
                        event.setDamage(event.getDamage() * 1.5);
                    }
                }
            }
        }

        // Ranged (Ranger)
        if (event.getDamager() instanceof Projectile projectile && event.getEntity() instanceof LivingEntity victim) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player shooter) {
                if (abilities.isRangerAtLeast(shooter, 1)) {
                    double bonus = abilities.isRangerAtLeast(shooter, 2)
                            ? cfg.rangerL2DamageBonus() : cfg.rangerL1DamageBonus();
                    event.setDamage(event.getDamage() * (1 + bonus));
                }
                if (abilities.isRangerAtLeast(shooter, 3) && projectile instanceof Arrow) {
                    String key = shooter.getUniqueId() + ":" + victim.getUniqueId();
                    int count = rangerHitCounts.merge(key, 1, Integer::sum);
                    if (count % 5 == 0) {
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                                cfg.rangerSlownessDurationSeconds() * 20, cfg.rangerSlownessAmp()));
                    }
                }
            }
        }
    }
}
