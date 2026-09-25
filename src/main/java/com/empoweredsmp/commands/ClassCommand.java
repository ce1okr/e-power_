package com.empoweredsmp.commands;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.model.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ClassCommand implements CommandExecutor {

    private final AbilityManager abilities;
    private final StarterKitManager starterKits;

    public ClassCommand(AbilityManager abilities, StarterKitManager starterKits) {
        this.abilities = abilities;
        this.starterKits = starterKits;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /class <player> <ability>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Abilities: " +
                    Arrays.stream(Ability.values()).map(Enum::name).collect(Collectors.joining(", ")),
                    NamedTextColor.GRAY));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online: " + args[0], NamedTextColor.RED));
            return true;
        }

        Ability ability = Ability.fromString(args[1]);
        if (ability == null) {
            sender.sendMessage(Component.text("Unknown ability: " + args[1], NamedTextColor.RED));
            return true;
        }

        if (abilities.get(target).hasAbility()) {
            sender.sendMessage(Component.text(target.getName() + " already has a permanent ability ("
                    + abilities.get(target).getAbility() + ") and cannot be reassigned.", NamedTextColor.RED));
            return true;
        }

        boolean ok = abilities.assign(target, ability);
        if (ok) {
            starterKits.grantLevelZeroKit(target, ability);
            sender.sendMessage(Component.text("Assigned " + target.getName() + " the " + ability
                    + " ability (permanent).", NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been permanently assigned the " + ability
                    + " ability!", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not assign ability (already set).", NamedTextColor.RED));
        }
        return true;
    }
}
