package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/** /pay <гравець> <сума>  - переказ лише монет (рублі - донат-валюта, не передаються між гравцями напряму). */
public class PayCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public PayCommand(PrisonPlugin plugin) {
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

        if (args.length < 2) {
            plugin.getMessages().send(player, "pay-usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessages().send(player, "target-not-found");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            plugin.getMessages().send(player, "pay-self");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(player, "pay-invalid-amount");
            return true;
        }

        if (amount <= 0) {
            plugin.getMessages().send(player, "pay-invalid-amount");
            return true;
        }

        PlayerData senderData = plugin.getPlayerDataManager().load(player.getUniqueId());
        if (!senderData.subtractBalance(amount)) {
            plugin.getMessages().send(player, "pay-not-enough");
            return true;
        }

        PlayerData targetData = plugin.getPlayerDataManager().load(target.getUniqueId());
        targetData.addBalance(amount);

        plugin.getMessages().send(player, "pay-success", "amount", CoinFormat.format(amount), "player", target.getName());
        plugin.getMessages().send(target, "pay-received", "amount", CoinFormat.format(amount), "player", player.getName());
        return true;
    }
}
