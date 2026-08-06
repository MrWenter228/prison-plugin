package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.gui.UpgradeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpgradeCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public UpgradeCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.upgrade")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        UpgradeGUI.open(plugin, player);
        return true;
    }
}
