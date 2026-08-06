package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Власна легка "економіка" плагіна: баланс і рівень гравця зберігаються у
 * /playerdata/<uuid>.yml. Кеш у пам'яті для швидкого доступу, запис на диск -
 * при виході гравця та періодично (autosave), щоб уникнути втрати даних
 * при падінні сервера.
 */
public class PlayerDataManager {

    private final PrisonPlugin plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private File dataFolder;

    public PlayerDataManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData load(UUID uuid) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) return cached;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        PlayerData data;
        String defaultLocale = plugin.getConfig().getString("language.default", "ru");
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int level = Math.max(1, yaml.getInt("level", 1));
            long blocksMined = yaml.getLong("blocksMinedForLevel", 0);
            double balance = yaml.getDouble("balance", plugin.getConfig().getDouble("economy.starting-balance", 0));
            double rubles = yaml.getDouble("rubles", plugin.getConfig().getDouble("economy.starting-rubles", 0));
            boolean autoSell = yaml.getBoolean("autosell", true);
            String locale = yaml.getString("locale", defaultLocale);
            long coinsBoosterExpiry = yaml.getLong("coinsBoosterExpiry", 0);
            long blocksBoosterExpiry = yaml.getLong("blocksBoosterExpiry", 0);
            double coinsBoosterMultiplier = yaml.getDouble("coinsBoosterMultiplier", 0);
            double blocksBoosterMultiplier = yaml.getDouble("blocksBoosterMultiplier", 0);
            data = new PlayerData(uuid, level, blocksMined, balance, rubles, autoSell, locale,
                    coinsBoosterExpiry, blocksBoosterExpiry, coinsBoosterMultiplier, blocksBoosterMultiplier);
        } else {
            data = new PlayerData(uuid, 1, 0,
                    plugin.getConfig().getDouble("economy.starting-balance", 0),
                    plugin.getConfig().getDouble("economy.starting-rubles", 0), true, defaultLocale, 0, 0, 0, 0);
        }

        cache.put(uuid, data);
        return data;
    }

    public void save(PlayerData data) {
        File file = new File(dataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("level", data.getLevel());
        yaml.set("blocksMinedForLevel", data.getBlocksMinedForLevel());
        yaml.set("balance", data.getBalance());
        yaml.set("rubles", data.getRubles());
        yaml.set("autosell", data.isAutoSellEnabled());
        yaml.set("locale", data.getLocale());
        yaml.set("coinsBoosterExpiry", data.getCoinsBoosterExpiry());
        yaml.set("blocksBoosterExpiry", data.getBlocksBoosterExpiry());
        yaml.set("coinsBoosterMultiplier", data.getCoinsBoosterMultiplier());
        yaml.set("blocksBoosterMultiplier", data.getBlocksBoosterMultiplier());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти playerdata для " + data.getUuid() + ": " + e.getMessage());
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
    }

    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
}
