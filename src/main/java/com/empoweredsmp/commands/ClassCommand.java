package com.empoweredsmp.commands;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * /class <player> <ability>
 * Requires empoweredsmp.admin (op by default). Assigns a player their ability,
 * or CHANGES it if they already have one, so an op can run it on themselves or
 * anyone else as many times as they like. A change resets the player to Level 0
 * (tier, death counter and Prosperity tool choice cleared) and grants the new
 * ability's Level 0 kit. Re-assigning the ability a player already has does nothing.
 */
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

        PlayerData data = abilities.get(target);
        Ability previous = data.getAbility();

        if (previous == ability) {
            sender.sendMessage(Component.text(target.getName() + " already has the " + ability
                    + " ability. Nothing changed.", NamedTextColor.YELLOW));
            return true;
        }

        data.setAbility(ability);
        data.resetForAbilityChange();
        abilities.save(target);
        starterKits.grantLevelZeroKit(target, ability);

        if (previous == null) {
            sender.sendMessage(Component.text("Assigned " + target.getName() + " the " + ability + " ability.",
                    NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been assigned the " + ability + " ability!",
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Changed " + target.getName() + "'s ability from " + previous
                    + " to " + ability + " (reset to Level 0).", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your ability has been changed to " + ability
                    + ". You are back at Level 0.", NamedTextColor.GREEN));
        }
        return true;
    }
}
