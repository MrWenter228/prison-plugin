package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Система рівнів (1-40, як на VimeWorld) - замінює стару систему престижу.
 * Підвищення рівня вимагає ОДНОЧАСНО: певну кількість видобутих у шахті
 * блоків (з моменту останнього рівня) І оплату монетами. Вимоги для кожного
 * рівня НЕ рахуються за формулою - це готова таблиця (config: levels.requirements),
 * скопійована з реальних чисел VimeWorld, тому може бути нерівномірною.
 * Рівень постійно відображається на ванільній XP-панелі гравця (число рівня
 * + прогрес-бар заповнення).
 */
public class LevelManager {

    public enum LevelUpResult { SUCCESS, MAX_LEVEL, NOT_ENOUGH_BLOCKS, NOT_ENOUGH_MONEY }

    public record Requirement(long blocks, double coins) {
    }

    private final PrisonPlugin plugin;
    /** Ключ - рівень, НА ЯКИЙ підвищуємось (напр. requirements.get(2) = вимоги для переходу 1 -> 2). */
    private final Map<Integer, Requirement> requirements = new HashMap<>();

    public LevelManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        requirements.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("levels.requirements");
        if (section == null) {
            plugin.getLogger().warning("levels.requirements відсутній у config.yml - система рівнів не працюватиме!");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                long blocks = section.getLong(key + ".blocks");
                double coins = section.getDouble(key + ".coins");
                requirements.put(level, new Requirement(blocks, coins));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Некоректний ключ рівня в levels.requirements: " + key);
            }
        }
        plugin.getLogger().info("Завантажено вимог по рівнях: " + requirements.size());
    }

    public int getMaxLevel() {
        return plugin.getConfig().getInt("levels.max-level", 40);
    }

    public boolean isMaxLevel(PlayerData data) {
        return data.getLevel() >= getMaxLevel();
    }

    private Requirement requirementFor(int targetLevel) {
        return requirements.getOrDefault(targetLevel, new Requirement(0, 0));
    }

    /** Скільки блоків треба видобути, щоб піднятись З даного рівня на наступний. */
    public long blocksRequiredForLevel(int currentLevel) {
        return requirementFor(currentLevel + 1).blocks();
    }

    /** Скільки монет коштує підвищення З даного рівня на наступний. */
    public double costForLevel(int currentLevel) {
        return requirementFor(currentLevel + 1).coins();
    }

    /** Постійний множник до виторгу з блоків, що росте з рівнем (як бонус старого престижу). */
    public double getLevelMultiplier(int level) {
        double perLevel = plugin.getConfig().getDouble("levels.reward-multiplier-per-level", 0.02);
        return 1.0 + perLevel * (level - 1);
    }

    /** Зараховує 1 видобутий блок у прогрес рівня, з урахуванням активного бустера блоків. */
    public void addMinedBlock(PlayerData data) {
        double multiplier = plugin.getBoosterManager().getActiveMultiplier(data, BoosterManager.Type.BLOCKS);
        data.addBlocksMined(Math.round(multiplier));
    }

    public LevelUpResult attemptLevelUp(PlayerData data) {
        if (isMaxLevel(data)) {
            return LevelUpResult.MAX_LEVEL;
        }

        long required = blocksRequiredForLevel(data.getLevel());
        if (data.getBlocksMinedForLevel() < required) {
            return LevelUpResult.NOT_ENOUGH_BLOCKS;
        }

        double cost = costForLevel(data.getLevel());
        if (!data.subtractBalance(cost)) {
            return LevelUpResult.NOT_ENOUGH_MONEY;
        }

        data.setLevel(data.getLevel() + 1);
        data.setBlocksMinedForLevel(0);
        return LevelUpResult.SUCCESS;
    }

    /** Адмінська зміна рівня напряму (без перевірки вимог) - для /lvl set. Скидає прогрес блоків. */
    public void setLevel(PlayerData data, int level) {
        int clamped = Math.max(1, Math.min(getMaxLevel(), level));
        data.setLevel(clamped);
        data.setBlocksMinedForLevel(0);
    }

    /**
     * Синхронізує ванільну XP-панель гравця (число над панеллю досвіду + сама
     * смужка заповнення) з ігровим рівнем плагіна. Викликати при вході,
     * після кожного видобутого блоку в шахті та після підвищення рівня.
     */
    public void updateXpBar(Player player, PlayerData data) {
        player.setLevel(data.getLevel());

        if (isMaxLevel(data)) {
            player.setExp(1.0f);
            return;
        }

        long required = blocksRequiredForLevel(data.getLevel());
        float progress = required <= 0 ? 0f
                : (float) Math.min(1.0, (double) data.getBlocksMinedForLevel() / (double) required);
        player.setExp(progress);
    }
}
