package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * Elemental Level 2: immune to Piglins (even without gold armor) and immune
 * to Mining Fatigue. Elemental Level 3: Nether mob drops doubled (aside from
 * the Wither, which is explicitly excluded per the spec).
 */
public class ElementalAbilityListener implements Listener {

    private final AbilityManager abilities;

    public ElementalAbilityListener(AbilityManager abilities) {
        this.abilities = abilities;
    }

    // ---- Piglin immunity (Elemental L2+) ----

    @EventHandler
    public void onPiglinTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof PiglinAbstract)) return;
        if (!(event.getTarget() instanceof Player p)) return;
        if (abilities.isElementalAtLeast(p, 2)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiglinDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!(event.getDamager() instanceof PiglinAbstract)) return;
        if (abilities.isElementalAtLeast(p, 2)) {
            event.setCancelled(true);
        }
    }

    // ---- Mining Fatigue immunity (Elemental L2+) ----

    @EventHandler(ignoreCancelled = true)
    public void onMiningFatigue(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (event.getNewEffect() == null) return;
        if (event.getNewEffect().getType().equals(PotionEffectType.MINING_FATIGUE)
                && abilities.isElementalAtLeast(p, 2)) {
            event.setCancelled(true);
        }
    }

    // ---- Doubled Nether mob drops (Elemental L3+, excluding the Wither) ----

    @EventHandler
    public void onNetherMobDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getWorld().getEnvironment() != World.Environment.NETHER) return;
        if (victim.getType() == EntityType.WITHER) return;

        Player killer = victim.getKiller();
        if (killer == null || !abilities.isElementalAtLeast(killer, 3)) return;

        for (ItemStack drop : event.getDrops()) {
            if (drop == null) continue;
            int doubled = Math.min(drop.getMaxStackSize(), drop.getAmount() * 2);
            drop.setAmount(doubled);
        }
    }
}
