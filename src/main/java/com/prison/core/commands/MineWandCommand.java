package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MineWandCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public MineWandCommand(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("player-only"));
            return true;
        }
        if (!player.hasPermission("prison.admin")) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }

        ItemStack wand = new ItemBuilder(Material.GOLDEN_AXE)
                .name(plugin.getMessages().get(player, "minewand-item-name"))
                .lore(plugin.getMessages().get(player, "minewand-item-lore-1"),
                        plugin.getMessages().get(player, "minewand-item-lore-2"))
                .glow(true)
                .build();

        // getItemMeta() повертає копію - тег потрібно проставити і застосувати через setItemMeta()
        var meta = wand.getItemMeta();
        meta.getPersistentDataContainer().set(plugin.getMineWandKey(), PersistentDataType.STRING, "wand");
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
        plugin.getMessages().send(player, "minewand-given");
        return true;
    }
}
