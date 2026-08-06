package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.Mine;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/** /mines [назва] - список усіх шахт (відкриті для всіх гравців) або телепорт до конкретної. */
public class MinesCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public MinesCommand(PrisonPlugin plugin) {
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

        if (args.length == 0) {
            plugin.getMessages().send(player, "mines-list-header");
            for (Mine mine : plugin.getMineManager().getAllMines().values()) {
                player.sendMessage(ChatColor.AQUA + mine.getId() + ChatColor.GRAY
                        + plugin.getMessages().get(player, "mines-list-world", "world", mine.getWorldName()));
            }
            plugin.getMessages().send(player, "mines-list-usage");
            return true;
        }

        String mineId = args[0];
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(mineId);
        if (mineOpt.isEmpty()) {
            plugin.getMessages().send(player, "mine-not-found");
            return true;
        }
        Mine mine = mineOpt.get();

        if (mine.getTeleport() == null) {
            plugin.getMessages().send(player, "mine-no-teleport-point");
            return true;
        }

        player.teleport(mine.getTeleport());
        plugin.getMessages().send(player, "mine-teleported", "mine", mine.getId());
        return true;
    }
}
