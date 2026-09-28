package com.empoweredsmp.managers;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.gui.AbilityChoiceHolder;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random reroll: uniformly picks 1 of the 8 abilities (including possibly the
 * player's current one) and resets them to Level 0 with it, granting that
 * ability's Level 0 kit. Choice reroll: same reset/grant, but the ability is
 * whatever the player picks from openPicker()'s GUI instead of being random.
 */
public class RerollManager {

    private final DataManager data;
    private final AbilityManager abilities;
    private final StarterKitManager starterKits;

    public RerollManager(DataManager data, AbilityManager abilities, StarterKitManager starterKits) {
        this.data = data;
        this.abilities = abilities;
        this.starterKits = starterKits;
    }

    public void performRandomReroll(Player p) {
        Ability[] all = Ability.values();
        Ability picked = all[ThreadLocalRandom.current().nextInt(all.length)];
        applyReroll(p, picked);
    }

    public void applyReroll(Player p, Ability newAbility) {
        PlayerData d = abilities.get(p);
        d.forceReroll(newAbility);
        data.save(p.getUniqueId());
        starterKits.grantLevelZeroKit(p, newAbility);
        p.sendMessage(Component.text("Your ability has been rerolled to " + newAbility
                + "! You are back to Level 0.", NamedTextColor.LIGHT_PURPLE));
    }

    public void openPicker(Player p) {
        Ability[] all = Ability.values();
        Inventory inv = Bukkit.createInventory(new AbilityChoiceHolder(p.getUniqueId()), 9,
                Component.text("Choose Your Ability"));
        for (int i = 0; i < all.length; i++) {
            inv.setItem(i, ItemUtil.buildAbilityIcon(all[i], iconFor(all[i])));
        }
        p.openInventory(inv);
    }

    private Material iconFor(Ability ability) {
        return switch (ability) {
            case PROSPERITY -> Material.EMERALD;
            case INVISIBILITY -> Material.POTION;
            case ELEMENTAL -> Material.BLAZE_POWDER;
            case VITALITY -> Material.GOLDEN_APPLE;
            case RANGER -> Material.BOW;
            case MOBILITY -> Material.FEATHER;
            case DEFENSE -> Material.SHIELD;
            case STRENGTH -> Material.DIAMOND_SWORD;
        };
    }
}
