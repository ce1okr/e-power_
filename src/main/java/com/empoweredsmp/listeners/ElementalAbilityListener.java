package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Elemental High Tier: immune to Mining Fatigue. */
public class ElementalAbilityListener implements Listener {

    private final AbilityManager abilities;

    public ElementalAbilityListener(AbilityManager abilities) {
        this.abilities = abilities;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMiningFatigue(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        PotionEffect effect = event.getNewEffect();
        if (effect == null) return;
        if (effect.getType().equals(PotionEffectType.MINING_FATIGUE) && abilities.isElementalAtLeast(p, 2)) {
            event.setCancelled(true);
        }
    }
}
