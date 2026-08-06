package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import com.prison.core.util.RubleFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/** /rubles give <гравець> <сума>  - адмінська видача рублів (донат-валюта). */
public class RublesCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public RublesCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("prison.admin")) {
            sender.sendMessage(plugin.getMessages().get("no-permission"));
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(plugin.getMessages().get("rubles-give-usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getMessages().get("target-not-found"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().get("pay-invalid-amount"));
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(plugin.getMessages().get("pay-invalid-amount"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().load(target.getUniqueId());
        data.addRubles(amount);

        sender.sendMessage(plugin.getMessages().get("rubles-give-success",
                "amount", RubleFormat.format(amount), "player", target.getName()));

        if (target.isOnline() && target instanceof Player onlinePlayer) {
            plugin.getMessages().send(onlinePlayer, "rubles-give-received", "amount", RubleFormat.format(amount));
        }
        return true;
    }
}
