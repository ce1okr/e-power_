package com.empoweredsmp.listeners;

import com.empoweredsmp.gui.AbilityChoiceHolder;
import com.empoweredsmp.managers.RerollManager;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class RerollListener implements Listener {

    private final RerollManager rerollManager;

    public RerollListener(RerollManager rerollManager) {
        this.rerollManager = rerollManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (ItemUtil.isRandomReroll(item)) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            rerollManager.performRandomReroll(p);
        } else if (ItemUtil.isChoiceReroll(item)) {
            event.setCancelled(true);
            // The token itself isn't consumed here — only once a valid ability is
            // actually clicked in the GUI (see onGuiClick), so closing without
            // picking loses nothing.
            rerollManager.openPicker(p);
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbilityChoiceHolder)) return;
        event.setCancelled(true); // this GUI is view/click-only; nothing can be taken out

        ItemStack clicked = event.getCurrentItem();
        Ability picked = ItemUtil.readAbilityIcon(clicked);
        if (picked == null) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;

        if (!consumeOneChoiceToken(p)) {
            p.sendMessage(Component.text("You no longer have an Ability Selector to spend.", NamedTextColor.RED));
            p.closeInventory();
            return;
        }

        rerollManager.applyReroll(p, picked);
        p.closeInventory();
    }

    /** Removes exactly one Ability Selector token from anywhere in the player's inventory. */
    private boolean consumeOneChoiceToken(Player p) {
        for (ItemStack stack : p.getInventory().getContents()) {
            if (ItemUtil.isChoiceReroll(stack)) {
                stack.setAmount(stack.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}
