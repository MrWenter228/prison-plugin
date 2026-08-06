package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.managers.UpgradeManager;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import com.prison.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Меню прокачки у стилі VimeWorld: горизонтальна драбина тірів зліва
 * направо (дерево -> камінь -> залізо -> ... -> алмаз), кожен слот - один
 * тір з реальною іконкою предмета цього тіра. Пройдені тіри - зелена
 * позначка, поточний - світіння, наступний - клікабельний з ціною, решта -
 * замкнені бар'єром.
 */
public class UpgradeGUI {


    private UpgradeGUI() {
    }

    public static void open(PrisonPlugin plugin, Player player) {
        UpgradeHolder holder = new UpgradeHolder();
        String title = plugin.getMessages().get(player, "gui-upgrade-title");
        Inventory inv = Bukkit.createInventory(holder, 9, title);
        holder.setInventory(inv);

        UpgradeManager upgradeManager = plugin.getUpgradeManager();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        UpgradeManager.Category category = upgradeManager.categoryOf(heldItem);

        if (category == null) {
            ItemStack warn = new ItemBuilder(Material.BARRIER)
                    .name(plugin.getMessages().get(player, "gui-upgrade-need-item"))
                    .build();
            inv.setItem(4, warn);
            player.openInventory(inv);
            return;
        }

        List<UpgradeManager.Tier> tiers = upgradeManager.getTiers(category);
        int currentIndex = upgradeManager.currentTierIndex(heldItem, category);
        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());

        for (int i = 0; i < tiers.size() && i < 9; i++) {
            UpgradeManager.Tier tier = tiers.get(i);
            ItemStack icon = upgradeManager.buildItemForTier(heldItem.getType(), category, tier);

            if (i < currentIndex) {
                inv.setItem(i, decorate(plugin, player, icon, "gui-upgrade-passed", false));
            } else if (i == currentIndex) {
                inv.setItem(i, decorate(plugin, player, icon, "gui-upgrade-current", true));
            } else if (i == currentIndex + 1) {
                boolean hasMaterial = tier.costMaterial == null
                        || countInInventory(player, tier.costMaterial) >= tier.costAmount;
                boolean canAfford = data.getBalance() >= tier.costCoins && hasMaterial;

                ItemBuilder builder = new ItemBuilder(icon.getType())
                        .name(plugin.getMessages().get(player, "gui-upgrade-next"))
                        .glow(canAfford);
                if (tier.costMaterial != null) {
                    builder.lore(
                            plugin.getMessages().get(player, "gui-upgrade-cost-material",
                                    "amount", String.valueOf(tier.costAmount),
                                    "material", formatMaterial(tier.costMaterial),
                                    "have", String.valueOf(countInInventory(player, tier.costMaterial))),
                            plugin.getMessages().get(player, "gui-upgrade-cost-coins", "cost", CoinFormat.format(tier.costCoins)),
                            "",
                            plugin.getMessages().get(player, "gui-upgrade-click")
                    );
                } else {
                    builder.lore(
                            plugin.getMessages().get(player, "gui-upgrade-cost-coins", "cost", CoinFormat.format(tier.costCoins)),
                            "",
                            plugin.getMessages().get(player, "gui-upgrade-click")
                    );
                }
                inv.setItem(i, applyEnchantVisual(builder.build(), icon));
                holder.setActionSlot(i);
            } else {
                inv.setItem(i, decorate(plugin, player, new ItemStack(Material.BARRIER), "gui-upgrade-locked", false));
            }
        }

        player.openInventory(inv);
    }

    private static ItemStack decorate(PrisonPlugin plugin, Player player, ItemStack base, String loreKey, boolean glow) {
        ItemStack copy = base.clone();
        var meta = copy.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(plugin.getMessages().get(player, loreKey)));
            copy.setItemMeta(meta);
        }
        if (glow) {
            copy = new ItemBuilder(copy.getType()).glow(true).build();
            var meta2 = copy.getItemMeta();
            if (meta2 != null) {
                meta2.setLore(List.of(plugin.getMessages().get(player, loreKey)));
                copy.setItemMeta(meta2);
            }
        }
        return copy;
    }

    /** Переносить справжнє зачарування (для коректного відображення в тултипі) на щойно побудований предмет лору. */
    private static ItemStack applyEnchantVisual(ItemStack withLore, ItemStack tierIconWithEnchant) {
        withLore.addUnsafeEnchantments(tierIconWithEnchant.getEnchantments());
        return withLore;
    }

    private static int countInInventory(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static String formatMaterial(Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
