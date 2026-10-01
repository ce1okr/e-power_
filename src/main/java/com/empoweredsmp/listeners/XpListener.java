package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.util.Config;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Prosperity: experience gains are multiplied (x1.5 Low Tier, x2 High Tier,
 * configurable under prosperity.xp-multiplier).
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
        if (!abilities.isProsperityAtLeast(p, 1) || event.getAmount() <= 0) return;
        double multiplier = cfg.prosperityXpMultiplier(abilities.levelOf(p));
        event.setAmount((int) Math.round(event.getAmount() * multiplier));
    }
}
