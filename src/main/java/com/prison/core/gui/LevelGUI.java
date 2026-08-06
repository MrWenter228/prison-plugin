package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.managers.LevelManager;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import com.prison.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


/**
 * Компактне меню рівня - єдиний ряд з 9 слотів (мінімум, який дозволяє
 * Minecraft для chest-інвентарів), лише один активний предмет - пляшка
 * досвіду по центру. Клік по ній підвищує рівень (якщо вимоги виконано),
 * лор пляшки завжди показує поточний прогрес і вимоги до наступного рівня.
 */
public class LevelGUI {

    private LevelGUI() {
    }

    public static void open(PrisonPlugin plugin, Player player) {
        LevelHolder holder = new LevelHolder();
        String title = plugin.getMessages().get(player, "gui-level-title");
        Inventory inv = Bukkit.createInventory(holder, 9, title);
        holder.setInventory(inv);

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) {
            if (i != LevelHolder.LEVELUP_SLOT) {
                inv.setItem(i, filler);
            }
        }

        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());
        LevelManager levelManager = plugin.getLevelManager();
        int level = data.getLevel();
        int maxLevel = levelManager.getMaxLevel();

        if (levelManager.isMaxLevel(data)) {
            inv.setItem(LevelHolder.LEVELUP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                    .name(plugin.getMessages().get(player, "gui-level-current", "level", String.valueOf(level), "max", String.valueOf(maxLevel)))
                    .lore(plugin.getMessages().get(player, "gui-level-max-item"))
                    .glow(true)
                    .build());
            player.openInventory(inv);
            return;
        }

        long requiredBlocks = levelManager.blocksRequiredForLevel(level);
        double cost = levelManager.costForLevel(level);
        boolean canAfford = data.getBalance() >= cost && data.getBlocksMinedForLevel() >= requiredBlocks;

        inv.setItem(LevelHolder.LEVELUP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(plugin.getMessages().get(player, "gui-level-current", "level", String.valueOf(level), "max", String.valueOf(maxLevel)))
                .lore(
                        plugin.getMessages().get(player, "gui-level-blocks-progress",
                                "mined", String.valueOf(data.getBlocksMinedForLevel()),
                                "required", String.valueOf(requiredBlocks)),
                        plugin.getMessages().get(player, "gui-level-cost", "cost", CoinFormat.format(cost)),
                        "",
                        plugin.getMessages().get(player, "gui-level-click")
                )
                .glow(canAfford)
                .build());

        player.openInventory(inv);
    }
}
