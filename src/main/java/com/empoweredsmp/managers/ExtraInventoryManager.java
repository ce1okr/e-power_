package com.empoweredsmp.managers;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.gui.ExtraInventoryHolder;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExtraInventoryManager {

    private final DataManager data;
    private final AbilityManager abilities;
    private final Config cfg;
    private final Map<UUID, Inventory> open = new HashMap<>();

    public ExtraInventoryManager(DataManager data, AbilityManager abilities, Config cfg) {
        this.data = data;
        this.abilities = abilities;
        this.cfg = cfg;
    }

    public int slotsFor(Player p) {
        if (!abilities.isProsperity(p)) return 0;
        int level = abilities.levelOf(p);
        return cfg.prosperityExtraInvSlots(level);
    }

    /** Rounds a raw slot count up to the nearest multiple of 9 (chest inventory rows), min 9. */
    private int rows(int slots) {
        if (slots <= 0) return 0;
        return Math.max(9, ((slots + 8) / 9) * 9);
    }

    public boolean open(Player p) {
        int slots = slotsFor(p);
        if (slots <= 0) {
            p.sendMessage(Component.text("You don't have an extra inventory yet (Prosperity ability required)."));
            return false;
        }
        Inventory inv = Bukkit.createInventory(new ExtraInventoryHolder(p.getUniqueId()), rows(slots),
                Component.text("Extra Inventory"));
        data.loadExtraInventory(p.getUniqueId(), inv);
        open.put(p.getUniqueId(), inv);
        p.openInventory(inv);
        return true;
    }

    public void onClose(UUID uuid, Inventory inv) {
        data.saveExtraInventory(uuid, inv);
        open.remove(uuid);
    }

    public void dropContentsOnDeath(Player p, List<ItemStack> drops) {
        Inventory inv = open.get(p.getUniqueId());
        ItemStack[] contents;
        if (inv != null) {
            contents = inv.getContents();
        } else {
            // load from disk into a scratch inventory just to read contents
            int slots = rows(slotsFor(p));
            if (slots <= 0) return;
            Inventory scratch = Bukkit.createInventory(null, slots);
            data.loadExtraInventory(p.getUniqueId(), scratch);
            contents = scratch.getContents();
        }
        for (ItemStack item : contents) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                drops.add(item);
            }
        }
        // Clear it either way so items aren't duplicated on next login.
        data.clearExtraInventoryFile(p.getUniqueId());
        if (inv != null) inv.clear();
    }
}
