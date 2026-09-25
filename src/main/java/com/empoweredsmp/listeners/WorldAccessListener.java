package com.empoweredsmp.listeners;

import com.empoweredsmp.data.GlobalState;
import com.empoweredsmp.managers.AbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldAccessListener implements Listener {

    private final AbilityManager abilities;
    private final GlobalState globalState;

    public WorldAccessListener(AbilityManager abilities, GlobalState globalState) {
        this.abilities = abilities;
        this.globalState = globalState;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player p = event.getPlayer();
        World.Environment targetEnv = event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                ? World.Environment.NETHER : World.Environment.THE_END;

        if (targetEnv == World.Environment.NETHER) {
            boolean allowed = globalState.isNetherUnlocked() || abilities.isElementalAtLeast(p, 0);
            if (!allowed) {
                event.setCancelled(true);
                p.sendMessage(Component.text(
                        "The Nether is locked. Elemental-ability players may enter early.",
                        NamedTextColor.RED));
            }
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (!globalState.isEndUnlocked()) {
                event.setCancelled(true);
                p.sendMessage(Component.text("The End is locked until an admin unlocks it.", NamedTextColor.RED));
            }
        }
    }
}
