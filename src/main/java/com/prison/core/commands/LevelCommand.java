package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.gui.LevelGUI;
import com.prison.core.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /lvl              - відкриває компактне меню рівня (одна пляшка)
 * /lvl set <гравець> <рівень>  - адмінська зміна рівня будь-якого гравця (1-40)
 */
public class LevelCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public LevelCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            handleSet(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.level")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        LevelGUI.open(plugin, player);
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prison.admin")) {
            sender.sendMessage(plugin.getMessages().get("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().get("lvl-set-usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getMessages().get("target-not-found"));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().get("lvl-set-usage"));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().load(target.getUniqueId());
        plugin.getLevelManager().setLevel(data, level);

        sender.sendMessage(plugin.getMessages().get("lvl-set-success",
                "player", target.getName(), "level", String.valueOf(data.getLevel())));

        if (target.isOnline() && target instanceof Player onlinePlayer) {
            plugin.getLevelManager().updateXpBar(onlinePlayer, data);
            plugin.getPerksManager().applyPerks(onlinePlayer, data);
        }
    }
}
