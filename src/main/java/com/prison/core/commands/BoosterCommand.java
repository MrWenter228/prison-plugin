package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.gui.BoosterGUI;
import com.prison.core.managers.BoosterManager;
import com.prison.core.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /booster                                                     - відкрити магазин бустерів (за рублі)
 * /booster give <гравець> <coins|blocks> <2|3|5> <day|week|month>  - адмінська видача бустера безкоштовно
 */
public class BoosterCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public BoosterCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            handleGive(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.booster")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        BoosterGUI.open(plugin, player);
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prison.admin")) {
            sender.sendMessage(plugin.getMessages().get("no-permission"));
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(plugin.getMessages().get("booster-give-usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getMessages().get("target-not-found"));
            return;
        }

        BoosterManager.Type type = switch (args[2].toLowerCase()) {
            case "coins", "coin" -> BoosterManager.Type.COINS;
            case "blocks", "block" -> BoosterManager.Type.BLOCKS;
            default -> null;
        };

        int multiplier;
        try {
            multiplier = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            multiplier = -1;
        }
        int parsedMultiplier = multiplier;
        boolean validMultiplier = java.util.Arrays.stream(BoosterManager.MULTIPLIERS).anyMatch(m -> m == parsedMultiplier);

        BoosterManager.Duration duration = switch (args[4].toLowerCase()) {
            case "day" -> BoosterManager.Duration.DAY;
            case "week" -> BoosterManager.Duration.WEEK;
            case "month" -> BoosterManager.Duration.MONTH;
            default -> null;
        };

        if (type == null || !validMultiplier || duration == null) {
            sender.sendMessage(plugin.getMessages().get("booster-give-usage"));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().load(target.getUniqueId());
        plugin.getBoosterManager().grant(data, type, multiplier, duration);

        String typeName = plugin.getMessages().get(type == BoosterManager.Type.COINS ? "gui-booster-coins-title" : "gui-booster-blocks-title");
        sender.sendMessage(plugin.getMessages().get("booster-give-success",
                "player", target.getName(), "type", typeName, "multiplier", String.valueOf(multiplier)));

        if (target.isOnline() && target instanceof Player onlinePlayer) {
            plugin.getMessages().send(onlinePlayer, "booster-give-received", "type", typeName, "multiplier", String.valueOf(multiplier));
        }
    }
}
