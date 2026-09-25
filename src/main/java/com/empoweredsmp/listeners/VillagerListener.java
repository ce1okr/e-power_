package com.empoweredsmp.listeners;

import com.empoweredsmp.data.GlobalState;
import com.empoweredsmp.managers.AbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerListener implements Listener {

    private final AbilityManager abilities;
    private final GlobalState globalState;

    public VillagerListener(AbilityManager abilities, GlobalState globalState) {
        this.abilities = abilities;
        this.globalState = globalState;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractVillager)) return;
        Player p = event.getPlayer();
        if (globalState.isVillagersUnlocked()) return;
        if (abilities.isProsperity(p)) return;

        event.setCancelled(true);
        p.sendMessage(Component.text(
                "Villagers are off-limits until the server unlocks them (Prosperity players excepted).",
                NamedTextColor.RED));
    }
}
