package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Проста "пошта" для предметів, які не можна віддати гравцю миттєво
 * (наприклад, лот на аукціоні протух, поки продавець був офлайн). Предмети
 * лежать тут, поки гравець не зайде на сервер - тоді видаються автоматично.
 * Зберігається в mailbox.yml, переживає рестарт сервера.
 */
public class MailboxManager {

    private final PrisonPlugin plugin;
    private final Map<UUID, List<ItemStack>> mail = new LinkedHashMap<>();
    private File file;

    public MailboxManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "mailbox.yml");
        mail.clear();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("mail");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<?> rawList = section.getList(key);
                if (rawList == null) continue;
                List<ItemStack> items = new ArrayList<>();
                for (Object o : rawList) {
                    if (o instanceof ItemStack item) items.add(item);
                }
                if (!items.isEmpty()) mail.put(uuid, items);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : mail.entrySet()) {
            config.set("mail." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти mailbox.yml: " + e.getMessage());
        }
    }

    public void addItem(UUID uuid, ItemStack item) {
        mail.computeIfAbsent(uuid, u -> new ArrayList<>()).add(item);
        save();
    }

    public boolean hasMail(UUID uuid) {
        List<ItemStack> items = mail.get(uuid);
        return items != null && !items.isEmpty();
    }

    /** Видає всю пошту гравцю (додає в інвентар, дропає під ноги якщо переповнений) і очищує скриньку. */
    public void deliver(Player player) {
        List<ItemStack> items = mail.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) return;

        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack extra : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), extra);
            }
        }
        save();
    }
}
