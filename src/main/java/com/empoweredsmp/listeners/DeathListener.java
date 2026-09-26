package com.empoweredsmp.listeners;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.ExtraInventoryManager;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final AbilityManager abilities;
    private final DataManager data;
    private final ExtraInventoryManager extraInv;
    private final Material fragmentMaterial;

    public DeathListener(AbilityManager abilities, DataManager data, ExtraInventoryManager extraInv,
                          Material fragmentMaterial) {
        this.abilities = abilities;
        this.data = data;
        this.extraInv = extraInv;
        this.fragmentMaterial = fragmentMaterial;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        PlayerData victimData = abilities.get(victim);

        // Death counter: 1/4 per death; at 4/4 lose a level (never drops below 0).
        boolean lostLevel = victimData.addDeathQuarter();
        data.save(victim.getUniqueId());
        if (lostLevel) {
            victim.sendMessage(Component.text("You lost a level from too many deaths! You are now level "
                    + victimData.getLevel() + ".", NamedTextColor.RED));
        } else {
            victim.sendMessage(Component.text("Death counter: " + victimData.getDeathCounter() + "/4",
                    NamedTextColor.GRAY));
        }

        // PvP kill: killer always gets a Level Fragment, regardless of the victim's level.
        Player killer = victim.getKiller();
        if (killer != null) {
            killer.getInventory().addItem(ItemUtil.buildLevelFragment(fragmentMaterial));
            killer.sendMessage(Component.text("You received a Level Fragment.", NamedTextColor.AQUA));
        }

        // Prosperity: extra inventory contents drop on death.
        if (abilities.isProsperity(victim)) {
            extraInv.dropContentsOnDeath(victim, event.getDrops());
        }
    }
}
