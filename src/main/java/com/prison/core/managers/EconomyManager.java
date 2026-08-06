package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Відповідає за ціни блоків, продаж інвентаря та бонус рівня до цін.
 */
public class EconomyManager {

    private final PrisonPlugin plugin;
    private final Map<Material, Double> prices = new HashMap<>();
    private File file;

    public EconomyManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "prices.yml");
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        prices.clear();

        if (config.getConfigurationSection("prices") != null) {
            for (String key : config.getConfigurationSection("prices").getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    prices.put(mat, config.getDouble("prices." + key));
                } else {
                    plugin.getLogger().warning("Невідомий матеріал у prices.yml: " + key);
                }
            }
        }
        plugin.getLogger().info("Завантажено цін на блоки: " + prices.size());
    }

    public double getPrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    /** Розраховує вартість одного блоку з урахуванням рівня гравця та активного бустера монет. */
    public double getEffectivePrice(Material material, PlayerData data) {
        double levelMultiplier = plugin.getLevelManager().getLevelMultiplier(data.getLevel());
        double boosterMultiplier = plugin.getBoosterManager().getActiveMultiplier(data, BoosterManager.Type.COINS);
        return getPrice(material) * levelMultiplier * boosterMultiplier;
    }

    /**
     * Продає всі предмети інвентаря гравця, що мають ціну в prices.yml.
     * @return сумарний виторг (0, якщо нічого продавати не було)
     */
    public double sellInventory(Player player, PlayerData data) {
        PlayerInventory inv = player.getInventory();
        double total = 0;

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            double unitPrice = getEffectivePrice(item.getType(), data);
            if (unitPrice <= 0) continue;

            total += unitPrice * item.getAmount();
            contents[i] = null;
        }

        if (total > 0) {
            inv.setStorageContents(contents);
            data.addBalance(total);
        }
        return total;
    }
}
