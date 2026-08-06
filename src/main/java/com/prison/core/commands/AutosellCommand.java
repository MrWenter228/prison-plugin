package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * /autosell on|off  - явне керування автопродажем блоків під час копання.
 * /autosell all      - одразу продати всі блоки з інвентаря (те саме, що /sellall).
 */
public class AutosellCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public AutosellCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.economy")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());

        if (args.length < 1) {
            plugin.getMessages().send(player, "autosell-usage");
            plugin.getMessages().send(player, data.isAutoSellEnabled() ? "autosell-status-on" : "autosell-status-off");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                data.setAutoSellEnabled(true);
                plugin.getMessages().send(player, "autosell-on");
            }
            case "off" -> {
                data.setAutoSellEnabled(false);
                plugin.getMessages().send(player, "autosell-off");
            }
            case "all" -> {
                double total = plugin.getEconomyManager().sellInventory(player, data);
                if (total <= 0) {
                    plugin.getMessages().send(player, "sell-nothing");
                } else {
                    plugin.getMessages().send(player, "sell-success", "amount", CoinFormat.format(total));
                }
            }
            default -> plugin.getMessages().send(player, "autosell-usage");
        }
        return true;
    }
}
