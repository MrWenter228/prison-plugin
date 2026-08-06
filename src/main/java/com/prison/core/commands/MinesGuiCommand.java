package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.gui.MinesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinesGuiCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public MinesGuiCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.mines")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        MinesGUI.open(plugin, player);
        return true;
    }
}
