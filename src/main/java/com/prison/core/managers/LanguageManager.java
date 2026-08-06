package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.util.GradientUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Завантажує /lang/*.yml (uk, ru, en) і віддає локалізовані повідомлення.
 * Локаль конкретного гравця зберігається у PlayerData; для CommandSender без
 * профілю (консоль) або якщо гравець ще не завантажений - береться
 * language.default з config.yml.
 *
 * Підтримка форматування:
 *  - звичайні &-коди кольору/стилю
 *  - &#RRGGBB суцільний hex-колір
 *  - &#RRGGBB&#RRGGBBтекст градієнт по символах (два hex-коди підряд)
 * Обробка градієнта відбувається ДО перекладу '&'-кодів, щоб залишкові
 * стильові коди (&l, &n...) продовжували працювати як завжди.
 */
public class LanguageManager {

    private static final List<String> SUPPORTED = List.of("ru", "en");

    private final PrisonPlugin plugin;
    private final Map<String, FileConfiguration> locales = new LinkedHashMap<>();
    private String defaultLocale;

    public LanguageManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        locales.clear();
        defaultLocale = plugin.getConfig().getString("language.default", "ru");
        if (!SUPPORTED.contains(defaultLocale)) {
            defaultLocale = "ru";
        }

        for (String code : SUPPORTED) {
            File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + code + ".yml", false);
            }
            locales.put(code, YamlConfiguration.loadConfiguration(file));
        }
        plugin.getLogger().info("Завантажено мовних файлів: " + locales.size());
    }

    public boolean isSupported(String code) {
        return locales.containsKey(code.toLowerCase());
    }

    public List<String> getSupportedLocales() {
        return SUPPORTED;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    /** Визначає локаль відправника: гравець -> його PlayerData.locale, інакше дефолтна. */
    public String resolveLocale(CommandSender sender) {
        if (sender instanceof Player player) {
            var data = plugin.getPlayerDataManager().load(player.getUniqueId());
            if (data.getLocale() != null && isSupported(data.getLocale())) {
                return data.getLocale();
            }
        }
        return defaultLocale;
    }

    private String prefix(String locale) {
        return process(rawFor(locale, "prefix"));
    }

    private String rawFor(String locale, String key) {
        FileConfiguration config = locales.getOrDefault(locale, locales.get(defaultLocale));
        String value = config.getString(key);
        if (value == null) {
            // fallback на дефолтну локаль, якщо ключ відсутній у обраній мові
            FileConfiguration fallback = locales.get(defaultLocale);
            value = fallback != null ? fallback.getString(key) : null;
        }
        return value != null ? value : key;
    }

    /** Градієнт/hex обробка + переклад &-кодів. */
    private String process(String raw) {
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', GradientUtil.apply(raw));
    }

    /** Дістає рядок за ключем для дефолтної локалі (використовується там, де немає CommandSender). */
    public String get(String key, String... placeholders) {
        return getForLocale(defaultLocale, key, placeholders);
    }

    public String get(CommandSender sender, String key, String... placeholders) {
        return getForLocale(resolveLocale(sender), key, placeholders);
    }

    public String getForLocale(String locale, String key, String... placeholders) {
        String raw = rawFor(locale, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return process(raw);
    }

    public void send(Player player, String key, String... placeholders) {
        String locale = resolveLocale(player);
        player.sendMessage(prefix(locale) + getForLocale(locale, key, placeholders));
    }

    public void sendRaw(Player player, String message) {
        player.sendMessage(prefix(resolveLocale(player)) + process(message));
    }

    /** Кольорування довільного тексту (градієнт+hex+легасі), без ключа/локалі - для службових рядків. */
    public String colorize(String raw) {
        return process(raw);
    }
}
