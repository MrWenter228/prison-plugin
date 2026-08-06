package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.Mine;
import com.prison.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** GUI-список усіх шахт (без рангового замка - вільний доступ для всіх гравців). */
public class MinesGUI {

    private MinesGUI() {
    }

    public static void open(PrisonPlugin plugin, Player player) {
        MinesHolder holder = new MinesHolder();
        String title = plugin.getMessages().get(player, "gui-mines-title");
        int mineCount = plugin.getMineManager().getAllMines().size();
        int size = Math.min(54, Math.max(9, ((mineCount / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        int slot = 0;
        for (Mine mine : plugin.getMineManager().getAllMines().values()) {
            if (slot >= size) break;

            ItemStack item = new ItemBuilder(Material.DIAMOND_ORE)
                    .name("&b" + mine.getId())
                    .lore("&7Світ: &f" + mine.getWorldName(),
                            "",
                            plugin.getMessages().get(player, "gui-mines-unlocked-click"))
                    .glow(true)
                    .build();

            holder.put(slot, mine.getId());
            inv.setItem(slot, item);
            slot++;
        }

        player.openInventory(inv);
    }
}
