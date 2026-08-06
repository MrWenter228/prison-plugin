package com.prison.core.commands;

import com.prison.core.PrisonPlugin;
import com.prison.core.managers.SelectionManager;
import com.prison.core.model.Mine;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * /mineset create <назва>          - створити шахту з поточного виділення (wand)
 * /mineset setblock <назва> <MAT> <%> - додати/оновити блок у складі шахти
 * /mineset settp <назва>           - встановити точку телепортації = поточна позиція гравця
 * /mineset reset <назва>           - примусовий реген шахти
 * /mineset delete <назва>          - видалити шахту
 * /mineset list                    - список усіх шахт
 */
public class MineSetCommand implements CommandExecutor {

    private final PrisonPlugin plugin;

    public MineSetCommand(PrisonPlugin plugin) {
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
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "setblock" -> handleSetBlock(player, args);
            case "settp" -> handleSetTeleport(player, args);
            case "reset" -> handleReset(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        for (String key : new String[]{"mineset-usage-create", "mineset-usage-setblock", "mineset-usage-settp",
                "mineset-usage-reset", "mineset-usage-delete", "mineset-usage-list"}) {
            player.sendMessage(ChatColor.GOLD + plugin.getMessages().get(player, key));
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "mineset-create-usage");
            return;
        }
        String id = args[1];

        SelectionManager.Selection selection = plugin.getSelectionManager().get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null) {
            plugin.getMessages().send(player, "mineset-select-first");
            return;
        }

        Location p1 = selection.pos1;
        Location p2 = selection.pos2;
        if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            plugin.getMessages().send(player, "mineset-different-worlds");
            return;
        }

        Map<Material, Double> defaultComposition = new LinkedHashMap<>();
        defaultComposition.put(Material.STONE, 100.0);

        Mine mine = new Mine(id, p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                player.getLocation(), defaultComposition);

        plugin.getMineManager().registerMine(mine);
        plugin.getSelectionManager().clear(player.getUniqueId());

        plugin.getMessages().send(player, "mineset-create-success", "id", id, "blocks", String.valueOf(mine.totalBlocks()));
        plugin.getMessages().send(player, "mineset-create-hint");
    }

    private void handleSetBlock(Player player, String[] args) {
        if (args.length < 4) {
            plugin.getMessages().send(player, "mineset-setblock-usage");
            return;
        }
        String id = args[1];
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(id);
        if (mineOpt.isEmpty()) {
            plugin.getMessages().send(player, "mine-not-found");
            return;
        }

        Material material = Material.matchMaterial(args[2].toUpperCase());
        if (material == null) {
            plugin.getMessages().send(player, "mineset-unknown-material", "material", args[2]);
            return;
        }

        double percent;
        try {
            percent = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(player, "mineset-percent-not-number");
            return;
        }

        Mine mine = mineOpt.get();
        mine.getComposition().put(material, percent);

        double sum = mine.getComposition().values().stream().mapToDouble(Double::doubleValue).sum();
        plugin.getMineManager().save();

        plugin.getMessages().send(player, "mineset-setblock-success",
                "material", material.name(), "percent", String.valueOf(percent), "id", id);
        if (Math.abs(sum - 100.0) > 0.01) {
            plugin.getMessages().send(player, "mineset-setblock-sum-warning", "sum", String.valueOf(sum));
        }
    }

    private void handleSetTeleport(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "mineset-settp-usage");
            return;
        }
        String id = args[1];
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(id);
        if (mineOpt.isEmpty()) {
            plugin.getMessages().send(player, "mine-not-found");
            return;
        }

        Mine old = mineOpt.get();
        Mine updated = new Mine(old.getId(), old.getWorldName(),
                old.getX1(), old.getY1(), old.getZ1(),
                old.getX2(), old.getY2(), old.getZ2(),
                player.getLocation(), old.getComposition());

        plugin.getMineManager().registerMine(updated);
        plugin.getMessages().send(player, "mineset-settp-success", "id", id);
    }

    private void handleReset(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "mineset-reset-usage");
            return;
        }
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(args[1]);
        if (mineOpt.isEmpty()) {
            plugin.getMessages().send(player, "mine-not-found");
            return;
        }
        plugin.getMineManager().resetMine(mineOpt.get());
        plugin.getMessages().send(player, "mineset-reset-success", "id", args[1]);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "mineset-delete-usage");
            return;
        }
        boolean removed = plugin.getMineManager().deleteMine(args[1]);
        if (removed) {
            plugin.getMessages().send(player, "mineset-delete-success", "id", args[1]);
        } else {
            plugin.getMessages().send(player, "mine-not-found");
        }
    }

    private void handleList(Player player) {
        plugin.getMessages().send(player, "mineset-list-header", "count", String.valueOf(plugin.getMineManager().getAllMines().size()));
        for (Mine mine : plugin.getMineManager().getAllMines().values()) {
            player.sendMessage(ChatColor.AQUA + mine.getId() + ChatColor.GRAY
                    + plugin.getMessages().get(player, "mineset-list-line",
                            "world", mine.getWorldName(), "blocks", String.valueOf(mine.totalBlocks())));
        }
    }
}
