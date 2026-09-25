package com.empoweredsmp.listeners;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FragmentInteractListener implements Listener {

    private final AbilityManager abilities;
    private final DataManager data;
    private final StarterKitManager starterKits;

    public FragmentInteractListener(AbilityManager abilities, DataManager data, StarterKitManager starterKits) {
        this.abilities = abilities;
        this.data = data;
        this.starterKits = starterKits;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (ItemUtil.isLevelFragment(item)) {
            event.setCancelled(true);
            PlayerData d = abilities.get(p);
            d.resetDeathCounter();
            data.save(p.getUniqueId());
            consumeOne(p, item);
            p.sendMessage(Component.text("Level Fragment consumed. Death counter reset to 0/4.", NamedTextColor.AQUA));
        } else if (ItemUtil.isLevelUpgrader(item)) {
            event.setCancelled(true);
            PlayerData d = abilities.get(p);
            if (!d.hasAbility()) {
                p.sendMessage(Component.text("You need an assigned ability before you can level up.", NamedTextColor.RED));
                return;
            }
            if (d.getLevel() >= 3) {
                p.sendMessage(Component.text("You are already at the maximum level (3).", NamedTextColor.RED));
                return;
            }
            d.setLevel(d.getLevel() + 1);
            data.save(p.getUniqueId());
            consumeOne(p, item);
            p.sendMessage(Component.text("Level Upgrader consumed! You are now level " + d.getLevel() + ".",
                    NamedTextColor.GOLD));
            starterKits.grantLevelUpKit(p, d.getAbility(), d.getLevel());
        }
    }

    private void consumeOne(Player p, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
    }
}
