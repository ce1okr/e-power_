package com.empoweredsmp.commands;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/**
 * /toolchoice <tool1> <tool2>
 * Prosperity Level 2 only. Locks in which 2 of the 4 netherite tool types
 * (pickaxe, axe, shovel, hoe) the player may use. One-time choice: once set,
 * it cannot be changed (mirrors the "abilities are permanent" theme used
 * elsewhere in this plugin). Prosperity Level 3 doesn't need this — they can
 * use all 4 automatically.
 */
public class ToolChoiceCommand implements CommandExecutor {

    private static final Map<String, Material> TOOL_ALIASES = Map.of(
            "pickaxe", Material.NETHERITE_PICKAXE,
            "axe", Material.NETHERITE_AXE,
            "shovel", Material.NETHERITE_SHOVEL,
            "hoe", Material.NETHERITE_HOE
    );

    private final AbilityManager abilities;

    public ToolChoiceCommand(AbilityManager abilities) {
        this.abilities = abilities;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        PlayerData d = abilities.get(p);

        if (d.hasChosenProsperityTools()) {
            sender.sendMessage(Component.text("You've already locked in your netherite tools: "
                    + toolName(d.getProsperityTool1()) + " and " + toolName(d.getProsperityTool2())
                    + ". This choice is permanent.", NamedTextColor.RED));
            return true;
        }

        if (!abilities.canChooseProsperityTools(p)) {
            if (abilities.isProsperityAtLeast(p, 3)) {
                sender.sendMessage(Component.text("You're Prosperity level 3 — you can already use all "
                        + "netherite tools, no need to choose.", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("You need to be Prosperity level 2 to choose your netherite tools.",
                        NamedTextColor.RED));
            }
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /toolchoice <tool1> <tool2>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Tools: pickaxe, axe, shovel, hoe", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Warning: this choice is permanent!", NamedTextColor.GOLD));
            return true;
        }

        Material tool1 = TOOL_ALIASES.get(args[0].toLowerCase(Locale.ROOT));
        Material tool2 = TOOL_ALIASES.get(args[1].toLowerCase(Locale.ROOT));

        if (tool1 == null || tool2 == null) {
            sender.sendMessage(Component.text("Unknown tool. Choose from: pickaxe, axe, shovel, hoe",
                    NamedTextColor.RED));
            return true;
        }
        if (tool1.equals(tool2)) {
            sender.sendMessage(Component.text("You must pick two different tools.", NamedTextColor.RED));
            return true;
        }

        boolean ok = d.setProsperityTools(tool1, tool2);
        if (ok) {
            abilities.save(p);
            sender.sendMessage(Component.text("Locked in! You can now use Netherite " + toolName(tool1)
                    + " and Netherite " + toolName(tool2) + ". This cannot be changed.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not set your tool choice (already chosen).", NamedTextColor.RED));
        }
        return true;
    }

    private String toolName(Material m) {
        return switch (m) {
            case NETHERITE_PICKAXE -> "Pickaxe";
            case NETHERITE_AXE -> "Axe";
            case NETHERITE_SHOVEL -> "Shovel";
            case NETHERITE_HOE -> "Hoe";
            default -> m.name();
        };
    }
}
