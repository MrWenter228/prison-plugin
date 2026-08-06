package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.gui.AuctionGUI;
import com.prison.core.gui.AuctionHolder;
import com.prison.core.managers.AuctionManager;
import com.prison.core.model.Currency;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Один клас обслуговує обидва аукціони - /auc (монети) і /rauc (рублі),
 * валюта передається в конструкторі при реєстрації команди.
 *
 * /auc                                       - відкрити аукціон (GUI: вкладки/сортування/сторінки)
 * /auc sell <ціна>                           - публічний лот з руки
 * /auc sell <ціна> <гравець>                 - персональна пропозиція конкретному гравцю
 */
public class AuctionCommand implements CommandExecutor {

    private final PrisonPlugin plugin;
    private final Currency currency;

    public AuctionCommand(PrisonPlugin plugin, Currency currency) {
        this.plugin = plugin;
        this.currency = currency;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.auction")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            AuctionGUI.open(plugin, player, currency, AuctionHolder.Tab.ALL, AuctionHolder.Sort.TIME_DESC, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            handleSell(player, args);
            return true;
        }

        plugin.getMessages().send(player, "auction-usage");
        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "auction-sell-usage");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            plugin.getMessages().send(player, "auction-empty-hand");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(player, "auction-invalid-price");
            return;
        }

        String targetPlayerName = args.length >= 3 ? args[2] : null;

        AuctionManager.ListResult result = plugin.getAuctionManager().list(player, item, price, currency, targetPlayerName);
        switch (result) {
            case SUCCESS -> {
                player.getInventory().setItemInMainHand(null);
                plugin.getMessages().send(player, targetPlayerName != null ? "auction-sell-personal-success" : "auction-sell-success");
            }
            case LIMIT_REACHED -> plugin.getMessages().send(player, "auction-limit-reached");
            case INVALID_PRICE -> plugin.getMessages().send(player, "auction-invalid-price");
            case TARGET_NOT_FOUND -> plugin.getMessages().send(player, "target-not-found");
        }
    }
}
