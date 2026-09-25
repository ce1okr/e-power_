package com.empoweredsmp.commands;

import com.empoweredsmp.data.GlobalState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * One command class handles all three gates; which gate it flips is decided
 * by the command label ("unlocknether" / "unlockend" / "unlockvillagers")
 * since all three are permission-gated to empoweredsmp.op and behave the same
 * way: irreversible once run.
 */
public class UnlockCommand implements CommandExecutor {

    private final GlobalState globalState;

    public UnlockCommand(GlobalState globalState) {
        this.globalState = globalState;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean changed;
        String what;
        switch (label.toLowerCase()) {
            case "unlocknether" -> {
                changed = globalState.unlockNether();
                what = "The Nether";
            }
            case "unlockend" -> {
                changed = globalState.unlockEnd();
                what = "The End";
            }
            case "unlockvillagers" -> {
                changed = globalState.unlockVillagers();
                what = "Villager trading";
            }
            default -> {
                sender.sendMessage(Component.text("Unknown unlock command.", NamedTextColor.RED));
                return true;
            }
        }

        if (!changed) {
            sender.sendMessage(Component.text(what + " is already unlocked.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text(what + " has been permanently unlocked for everyone!", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text(what + " has been unlocked for everyone!", NamedTextColor.GOLD));
        return true;
    }
}
