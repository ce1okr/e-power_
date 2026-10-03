package com.empoweredsmp.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Safety net, once a second: if a player is wearing armor that's illegal for them
 * (they lost a tier, were switched to another ability, or got it on some way the
 * click checks missed), it's taken off and put back in their inventory (or dropped
 * at their feet if the inventory is full). It is never deleted.
 */
public class IllegalGearSweeper extends BukkitRunnable {

    private final Plugin plugin;
    private final ItemRules rules;

    public IllegalGearSweeper(Plugin plugin, ItemRules rules) {
        this.plugin = plugin;
        this.rules = rules;
    }

    public void start() {
        runTaskTimer(plugin, 40L, 20L);
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sweep(p);
        }
    }

    private void sweep(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] worn = { inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots() };
        boolean removedAny = false;

        for (int i = 0; i < worn.length; i++) {
            ItemStack piece = worn[i];
            if (piece == null || piece.getType() == Material.AIR) continue;
            if (rules.isAllowed(p, piece)) continue;

            switch (i) {
                case 0 -> inv.setHelmet(null);
                case 1 -> inv.setChestplate(null);
                case 2 -> inv.setLeggings(null);
                default -> inv.setBoots(null);
            }
            Map<Integer, ItemStack> leftover = inv.addItem(piece);
            for (ItemStack extra : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), extra);
            }
            removedAny = true;
        }

        if (removedAny) {
            p.sendActionBar(Component.text("You can't wear that armor anymore. It was moved to your inventory.",
                    NamedTextColor.RED));
        }
    }
}
