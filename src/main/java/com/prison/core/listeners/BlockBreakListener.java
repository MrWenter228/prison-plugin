package com.prison.core.listeners;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.Mine;
import com.prison.core.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Optional;

/**
 * Обробляє видобуток блоків у шахтах: автопродаж (autosell), захист меж
 * шахти (бедрок) та прогрес рівня гравця (кожен видобутий у шахті блок
 * наближає до наступного рівня). Доступ до шахт більше не залежить від
 * рангу - система рангів видалена, усі шахти відкриті для всіх гравців.
 */
public class BlockBreakListener implements Listener {

    private final PrisonPlugin plugin;

    public BlockBreakListener(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material border = Material.matchMaterial(
                plugin.getConfig().getString("mines.border-block", "BEDROCK"));

        if (event.getBlock().getType() == border) {
            event.setCancelled(true);
            return;
        }

        Optional<Mine> mineOpt = plugin.getMineManager().findMineAt(event.getBlock().getLocation());
        if (mineOpt.isEmpty()) {
            return; // блок поза шахтою - плагін не втручається
        }

        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());

        // Автопродаж
        if (data.isAutoSellEnabled() && plugin.getConfig().getBoolean("sellwand.enabled", true)) {
            Material type = event.getBlock().getType();
            double price = plugin.getEconomyManager().getEffectivePrice(type, data);
            if (price > 0) {
                event.setDropItems(false);
                data.addBalance(price);
            }
        }

        // Прогрес рівня: кожен видобутий у шахті блок наближає до наступного рівня
        plugin.getLevelManager().addMinedBlock(data);
        plugin.getLevelManager().updateXpBar(player, data);
    }
}
