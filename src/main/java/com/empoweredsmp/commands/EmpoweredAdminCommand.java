package com.empoweredsmp.commands;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EmpoweredAdminCommand implements CommandExecutor {

    private final AbilityManager abilities;
    private final DataManager data;
    private final StarterKitManager starterKits;

    public EmpoweredAdminCommand(AbilityManager abilities, DataManager data, StarterKitManager starterKits) {
        this.abilities = abilities;
        this.data = data;
        this.starterKits = starterKits;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /empowered <info|setlevel|resetdeaths> <player> [value]",
                    NamedTextColor.RED));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (args.length < 2) {
            sender.sendMessage(Component.text("Missing player name.", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online: " + args[1], NamedTextColor.RED));
            return true;
        }
        PlayerData d = abilities.get(target);

        switch (sub) {
            case "info" -> sender.sendMessage(Component.text(target.getName() + " -> ability="
                    + d.getAbility() + " level=" + d.getLevel() + " deathCounter=" + d.getDeathCounter() + "/4",
                    NamedTextColor.YELLOW));
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /empowered setlevel <player> <0-3>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int lvl = Integer.parseInt(args[2]);
                    d.setLevel(lvl);
                    data.save(target.getUniqueId());
                    if (d.hasAbility()) {
                        starterKits.grantLevelUpKit(target, d.getAbility(), d.getLevel());
                    }
                    sender.sendMessage(Component.text("Set " + target.getName() + " to level " + d.getLevel(),
                            NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Level must be a number 0-3.", NamedTextColor.RED));
                }
            }
            case "resetdeaths" -> {
                d.resetDeathCounter();
                data.save(target.getUniqueId());
                sender.sendMessage(Component.text("Reset death counter for " + target.getName(), NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
        }
        return true;
    }
}
