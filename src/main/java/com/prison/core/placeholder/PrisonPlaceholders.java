package com.prison.core.placeholder;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import com.prison.core.util.RubleFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;


/**
 * Реєструє плейсхолдери %prison_...% для PlaceholderAPI (TAB, скорборди, чат-формати тощо).
 * Клас підключається лише якщо на сервері встановлений і увімкнений PlaceholderAPI -
 * див. PrisonPlugin#registerPlaceholders(), де це перевіряється перед new PrisonPlaceholders(...).register().
 *
 * Підтримувані плейсхолдери:
 *   %prison_balance%          - баланс монет, відформатований (напр. "12,345.00")
 *   %prison_rubles%           - баланс рублів, відформатований
 *   %prison_level%             - поточний рівень гравця (1-40)
 *   %prison_level_max%         - максимальний рівень (40)
 *   %prison_level_progress%    - блоків видобуто/потрібно до наступного рівня (напр. "320/500")
 *   %prison_booster_coins%     - "x2 (5г)" якщо активний бустер монет, інакше "-"
 *   %prison_booster_blocks%    - те саме для бустера блоків
 */
public class PrisonPlaceholders extends PlaceholderExpansion {

    private final PrisonPlugin plugin;

    public PrisonPlaceholders(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "prison";
    }

    @Override
    public String getAuthor() {
        return "MrWenter";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** true - плейсхолдер лишається зареєстрованим і після /papi reload, не потрібно чіпляти вручну. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            return "";
        }

        PlayerData data = plugin.getPlayerDataManager().load(offlinePlayer.getUniqueId());

        return switch (params.toLowerCase()) {
            case "balance" -> CoinFormat.format(data.getBalance());
            case "rubles" -> RubleFormat.format(data.getRubles());
            case "level" -> String.valueOf(data.getLevel());
            case "level_max" -> String.valueOf(plugin.getLevelManager().getMaxLevel());
            case "level_progress" -> data.getBlocksMinedForLevel() + "/" + plugin.getLevelManager().blocksRequiredForLevel(data.getLevel());
            case "booster_coins" -> boosterPlaceholder(data, com.prison.core.managers.BoosterManager.Type.COINS);
            case "booster_blocks" -> boosterPlaceholder(data, com.prison.core.managers.BoosterManager.Type.BLOCKS);
            default -> null; // невідомий параметр - PlaceholderAPI сам покаже плейсхолдер як є
        };
    }

    private String boosterPlaceholder(PlayerData data, com.prison.core.managers.BoosterManager.Type type) {
        var boosterManager = plugin.getBoosterManager();
        if (!boosterManager.isActive(data, type)) {
            return "-";
        }
        long hours = boosterManager.getRemainingMillis(data, type) / 3_600_000L;
        return "x" + (int) boosterManager.getActiveMultiplier(data, type) + " (" + hours + "г)";
    }
}
