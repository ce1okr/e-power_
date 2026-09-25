package com.empoweredsmp.listeners;

import com.empoweredsmp.gui.ExtraInventoryHolder;
import com.empoweredsmp.managers.ExtraInventoryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ExtraInventoryListener implements Listener {

    private final ExtraInventoryManager extraInv;

    public ExtraInventoryListener(ExtraInventoryManager extraInv) {
        this.extraInv = extraInv;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ExtraInventoryHolder holder) {
            extraInv.onClose(holder.getOwner(), event.getInventory());
        }
    }
}
