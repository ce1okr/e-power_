package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.managers.StarterKitManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class LungeListener implements Listener {

    private final CooldownManager cooldowns;

    public LungeListener(CooldownManager cooldowns) {
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != StarterKitManager.SPEAR_MATERIAL) return;

        if (cooldowns.isOnCooldown(p.getUniqueId(), "lunge")) {
            p.sendMessage(Component.text("Lunge on cooldown: "
                    + cooldowns.remainingSeconds(p.getUniqueId(), "lunge") + "s", NamedTextColor.RED));
            return;
        }
        cooldowns.set(p.getUniqueId(), "lunge", 15);

        Location loc = p.getLocation();
        Vector direction = loc.getDirection().normalize().multiply(1.6).setY(0.4);
        p.setVelocity(direction);
        p.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc, 10, 0.2, 0.2, 0.2, 0.02);
    }
}
