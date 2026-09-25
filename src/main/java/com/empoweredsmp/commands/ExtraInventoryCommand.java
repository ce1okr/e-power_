package com.empoweredsmp.commands;

import com.empoweredsmp.managers.ExtraInventoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExtraInventoryCommand implements CommandExecutor {

    private final ExtraInventoryManager extraInv;

    public ExtraInventoryCommand(ExtraInventoryManager extraInv) {
        this.extraInv = extraInv;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        extraInv.open(p);
        return true;
    }
}
