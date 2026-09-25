package com.empoweredsmp.listeners;

import com.empoweredsmp.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;

/**
 * Ranger Level 2: "A Custom Crossbow with Quick Charge X". Vanilla enchants
 * cap Quick Charge at 5, so the "beyond vanilla" version of this ability is
 * implemented here directly: right-clicking an unloaded Quick Draw Crossbow
 * skips the vanilla charge animation entirely and loads it instantly.
 * Firing an already-loaded crossbow is untouched — vanilla handles that.
 */
public class RangerCrossbowListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CROSSBOW) return;
        if (!ItemUtil.isRangerCrossbow(item)) return;

        if (!(item.getItemMeta() instanceof CrossbowMeta meta)) return;
        if (meta.hasChargedProjectiles()) return; // already loaded: let vanilla fire it normally

        ItemStack ammo = findAmmo(p);
        if (ammo == null) {
            p.sendMessage(Component.text("You have no arrows to load.", NamedTextColor.RED));
            return;
        }

        event.setCancelled(true); // skip vanilla's charge-time animation entirely

        meta.setChargedProjectiles(java.util.List.of(new ItemStack(ammo.getType())));
        item.setItemMeta(meta);

        if (p.getGameMode() != GameMode.CREATIVE) {
            ammo.setAmount(ammo.getAmount() - 1);
        }

        p.getWorld().playSound(p.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.2f);
    }

    /** Finds the first arrow-family stack (normal, tipped, or spectral) in the player's inventory. */
    private ItemStack findAmmo(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            return new ItemStack(Material.ARROW); // creative: infinite, nothing to consume
        }
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack == null) continue;
            Material t = stack.getType();
            if (t == Material.ARROW || t == Material.TIPPED_ARROW || t == Material.SPECTRAL_ARROW) {
                return stack;
            }
        }
        return null;
    }
}
