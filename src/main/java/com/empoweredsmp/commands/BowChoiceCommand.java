package com.empoweredsmp.commands;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.model.BowEnchantChoice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /bowchoice <infinity|mending>
 * Ranger Level 1+. Sets a standing preference; RangerBowListener keeps
 * whatever bow the player is holding in sync with it (removing the other
 * enchant, applying the chosen one). Can be changed anytime, unlike the
 * Prosperity tool choice.
 */
public class BowChoiceCommand implements CommandExecutor {

    private final AbilityManager abilities;

    public BowChoiceCommand(AbilityManager abilities) {
        this.abilities = abilities;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (!abilities.canChooseBowEnchant(p)) {
            sender.sendMessage(Component.text("You need the Ranger ability to do this.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /bowchoice <infinity|mending>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Current choice: " + abilities.getBowEnchantChoice(p),
                    NamedTextColor.GRAY));
            return true;
        }

        BowEnchantChoice choice = BowEnchantChoice.fromString(args[0]);
        if (choice == null) {
            sender.sendMessage(Component.text("Unknown choice. Use 'infinity' or 'mending'.", NamedTextColor.RED));
            return true;
        }

        abilities.setBowEnchantChoice(p, choice);
        sender.sendMessage(Component.text("Your bows will now carry " + choice.name().toLowerCase()
                + ". This applies next time you hold or re-equip a bow.", NamedTextColor.GREEN));
        return true;
    }
}
