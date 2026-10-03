package com.empoweredsmp.listeners;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.ExtraInventoryManager;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.ItemUtil;
import com.empoweredsmp.util.Tiers;
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
        int tierBefore = victimData.getLevel();

        // Death counter: 1/4 per death; at 4/4 you drop one tier (never below Level 0).
        boolean rolledOver = victimData.addDeathQuarter();
        data.save(victim.getUniqueId());

        // A Level 0 player's 4/4 death has no tier to take, so it doesn't drop a fragment.
        boolean noTierToLose = rolledOver && tierBefore == 0;

        if (noTierToLose) {
            victim.sendMessage(Component.text("4/4 deaths. You're already at Level 0, so there's no tier to lose. "
                    + "Death counter reset.", NamedTextColor.GRAY));
        } else if (rolledOver) {
            victim.sendMessage(Component.text("Too many deaths! You are now " + Tiers.name(victimData.getLevel()) + ".",
                    NamedTextColor.RED));
        } else {
            victim.sendMessage(Component.text("Death counter: " + victimData.getDeathCounter() + "/4",
                    NamedTextColor.GRAY));
        }

        // Every death drops one Level Fragment where the player died (PvP or not),
        // except the Level 0 4/4 death above.
        if (!noTierToLose) {
            event.getDrops().add(ItemUtil.buildLevelFragment(fragmentMaterial));
        }

        // Prosperity: extra inventory contents drop on death.
        if (abilities.isProsperity(victim)) {
            extraInv.dropContentsOnDeath(victim, event.getDrops());
        }
    }
}
