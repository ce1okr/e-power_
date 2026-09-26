package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.util.Config;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Prosperity Level 1: "Double Experience Levels". Multiplies the raw XP amount
 * on every gain (orbs, breeding, furnace/grindstone collection, etc.) by
 * prosperity.xp-multiplier in config.yml (default 2.0), which in turn speeds
 * up leveling by the same factor.
 */
public class XpListener implements Listener {

    private final AbilityManager abilities;
    private final Config cfg;

    public XpListener(AbilityManager abilities, Config cfg) {
        this.abilities = abilities;
        this.cfg = cfg;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player p = event.getPlayer();
        if (abilities.isProsperityAtLeast(p, 1) && event.getAmount() > 0) {
            event.setAmount((int) Math.round(event.getAmount() * cfg.prosperityXpMultiplier()));
        }
    }
}
