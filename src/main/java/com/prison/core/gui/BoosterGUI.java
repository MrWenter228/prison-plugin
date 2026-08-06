package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.managers.BoosterManager;
import com.prison.core.model.PlayerData;
import com.prison.core.util.ItemBuilder;
import com.prison.core.util.RubleFormat;
import com.prison.core.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Магазин бустерів за рублі: окремо бустер монет (множить виторг з блоків) і
 * бустер блоків (множить прогрес рівня), кожен доступний одразу на трьох
 * рівнях сили - x2, x3, x5 - і на день/тиждень/місяць. Повторна покупка
 * додає термін до вже активного бустера, а сила одразу перемикається на
 * щойно куплену.
 */
public class BoosterGUI {

    private static final Material[] TIER_ICONS = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND};

    private BoosterGUI() {
    }

    public static void open(PrisonPlugin plugin, Player player) {
        BoosterHolder holder = new BoosterHolder();
        String title = plugin.getMessages().get(player, "gui-booster-title");
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        BoosterManager boosterManager = plugin.getBoosterManager();
        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());

        buildSection(plugin, player, inv, holder, data, BoosterManager.Type.COINS,
                BoosterHolder.COINS_STATUS_SLOT, BoosterHolder.COINS_BUTTONS_START, Material.GOLD_INGOT);
        buildSection(plugin, player, inv, holder, data, BoosterManager.Type.BLOCKS,
                BoosterHolder.BLOCKS_STATUS_SLOT, BoosterHolder.BLOCKS_BUTTONS_START, Material.DIAMOND_PICKAXE);

        player.openInventory(inv);
    }

    private static void buildSection(PrisonPlugin plugin, Player player, Inventory inv, BoosterHolder holder,
                                      PlayerData data, BoosterManager.Type type, int statusSlot, int buttonsStart,
                                      Material statusIcon) {
        BoosterManager boosterManager = plugin.getBoosterManager();
        String typeKey = type == BoosterManager.Type.COINS ? "coins" : "blocks";
        boolean active = boosterManager.isActive(data, type);

        ItemBuilder statusBuilder = new ItemBuilder(statusIcon)
                .name(plugin.getMessages().get(player, "gui-booster-" + typeKey + "-title"))
                .glow(active);
        if (active) {
            statusBuilder.lore(
                    plugin.getMessages().get(player, "gui-booster-active",
                            "multiplier", String.valueOf((int) boosterManager.getActiveMultiplier(data, type)),
                            "time", TimeFormatter.format(plugin, player, boosterManager.getRemainingMillis(data, type)))
            );
        } else {
            statusBuilder.lore(plugin.getMessages().get(player, "gui-booster-inactive"));
        }
        inv.setItem(statusSlot, statusBuilder.build());

        for (int multIndex = 0; multIndex < BoosterManager.MULTIPLIERS.length; multIndex++) {
            int multiplier = BoosterManager.MULTIPLIERS[multIndex];
            BoosterManager.Duration[] durations = BoosterManager.Duration.values();
            for (int durIndex = 0; durIndex < durations.length; durIndex++) {
                int slot = BoosterHolder.slotFor(buttonsStart, multIndex, durIndex);
                placeBuyButton(plugin, player, inv, holder, data, type, multiplier, durations[durIndex], slot);
            }
        }
    }

    private static void placeBuyButton(PrisonPlugin plugin, Player player, Inventory inv, BoosterHolder holder,
                                        PlayerData data, BoosterManager.Type type, int multiplier,
                                        BoosterManager.Duration duration, int slot) {
        BoosterManager boosterManager = plugin.getBoosterManager();
        double price = boosterManager.getPrice(type, multiplier, duration);
        boolean canAfford = data.getRubles() >= price;
        String durationKey = switch (duration) {
            case DAY -> "gui-booster-day";
            case WEEK -> "gui-booster-week";
            case MONTH -> "gui-booster-month";
        };
        Material icon = TIER_ICONS[java.util.Arrays.binarySearch(BoosterManager.MULTIPLIERS, multiplier)];

        ItemStack item = new ItemBuilder(icon)
                .name(plugin.getMessages().get(player, "gui-booster-tier", "multiplier", String.valueOf(multiplier))
                        + " " + plugin.getMessages().get(player, durationKey))
                .lore(
                        plugin.getMessages().get(player, "gui-booster-price", "price", RubleFormat.format(price)),
                        "",
                        plugin.getMessages().get(player, "gui-booster-click")
                )
                .glow(canAfford)
                .build();

        inv.setItem(slot, item);
        holder.register(slot, type, multiplier, duration);
    }
}
