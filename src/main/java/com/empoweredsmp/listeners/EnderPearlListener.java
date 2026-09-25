package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

public class EnderPearlListener implements Listener {

    private final CooldownManager cooldowns;
    private final Config cfg;

    public EnderPearlListener(CooldownManager cooldowns, Config cfg) {
        this.cooldowns = cooldowns;
        this.cfg = cfg;
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        ProjectileSource source = pearl.getShooter();
        if (!(source instanceof Player p)) return;

        if (cooldowns.isOnCooldown(p.getUniqueId(), "ender_pearl")) {
            event.setCancelled(true);
            long remaining = cooldowns.remainingSeconds(p.getUniqueId(), "ender_pearl");
            p.sendMessage(Component.text("Ender Pearl on cooldown: " + remaining + "s", NamedTextColor.RED));
            return;
        }
        cooldowns.set(p.getUniqueId(), "ender_pearl", cfg.enderPearlCooldownSeconds());
    }
}
