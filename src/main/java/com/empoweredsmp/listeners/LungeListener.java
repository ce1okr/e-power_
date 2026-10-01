package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.util.Config;
import io.papermc.paper.event.entity.EntityLungeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Mobility's starter spear is a real vanilla Spear with Lunge III. Minecraft's
 * own Lunge handles the movement; this just enforces the 15-second cooldown
 * (mobility.lunge-cooldown-seconds) on any player lunging with any spear.
 */
public class LungeListener implements Listener {

    private static final String COOLDOWN_TAG = "lunge";

    private final CooldownManager cooldowns;
    private final Config cfg;

    public LungeListener(CooldownManager cooldowns, Config cfg) {
        this.cooldowns = cooldowns;
        this.cfg = cfg;
    }

    @EventHandler
    public void onLunge(EntityLungeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        if (cooldowns.isOnCooldown(p.getUniqueId(), COOLDOWN_TAG)) {
            event.setCancelled(true);
            p.sendMessage(Component.text("Lunge on cooldown: "
                    + cooldowns.remainingSeconds(p.getUniqueId(), COOLDOWN_TAG) + "s", NamedTextColor.RED));
            return;
        }
        cooldowns.set(p.getUniqueId(), COOLDOWN_TAG, cfg.mobilityLungeCooldownSeconds());
    }
}
