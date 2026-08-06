package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.Mine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
 * Управляє шахтами: зберігання в mines.yml, побудова регіонів,
 * заповнення блоками (реген) та періодична перевірка порогу виробленості.
 */
public class MineManager {

    private final PrisonPlugin plugin;
    private final Map<String, Mine> mines = new ConcurrentHashMap<>();
    private final KeySetView<String, Boolean> resettingMines = ConcurrentHashMap.newKeySet();
    private File file;
    private FileConfiguration config;

    public MineManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "mines.yml");
        if (!file.exists()) {
            plugin.saveResource("mines.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        mines.clear();

        ConfigurationSection section = config.getConfigurationSection("mines");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection m = section.getConfigurationSection(id);
            if (m == null) continue;

            String world = m.getString("world", "world");
            int x1 = m.getInt("x1");
            int y1 = m.getInt("y1");
            int z1 = m.getInt("z1");
            int x2 = m.getInt("x2");
            int y2 = m.getInt("y2");
            int z2 = m.getInt("z2");

            Location teleport = null;
            ConfigurationSection tp = m.getConfigurationSection("teleport");
            if (tp != null) {
                teleport = new Location(Bukkit.getWorld(world),
                        tp.getDouble("x"), tp.getDouble("y"), tp.getDouble("z"),
                        (float) tp.getDouble("yaw"), (float) tp.getDouble("pitch"));
            }

            Map<Material, Double> composition = new LinkedHashMap<>();
            ConfigurationSection comp = m.getConfigurationSection("composition");
            if (comp != null) {
                for (String matKey : comp.getKeys(false)) {
                    Material mat = Material.matchMaterial(matKey);
                    if (mat != null) {
                        composition.put(mat, comp.getDouble(matKey));
                    } else {
                        plugin.getLogger().warning("Невідомий матеріал у шахті " + id + ": " + matKey);
                    }
                }
            }

            mines.put(id, new Mine(id, world, x1, y1, z1, x2, y2, z2, teleport, composition));
        }

        plugin.getLogger().info("Завантажено шахт: " + mines.size());
    }

    public void save() {
        config.set("mines", null); // повне перезаписування секції
        for (Mine mine : mines.values()) {
            String path = "mines." + mine.getId();
            config.set(path + ".world", mine.getWorldName());
            config.set(path + ".x1", mine.getX1());
            config.set(path + ".y1", mine.getY1());
            config.set(path + ".z1", mine.getZ1());
            config.set(path + ".x2", mine.getX2());
            config.set(path + ".y2", mine.getY2());
            config.set(path + ".z2", mine.getZ2());

            if (mine.getTeleport() != null) {
                Location tp = mine.getTeleport();
                config.set(path + ".teleport.x", tp.getX());
                config.set(path + ".teleport.y", tp.getY());
                config.set(path + ".teleport.z", tp.getZ());
                config.set(path + ".teleport.yaw", tp.getYaw());
                config.set(path + ".teleport.pitch", tp.getPitch());
            }

            for (Map.Entry<Material, Double> entry : mine.getComposition().entrySet()) {
                config.set(path + ".composition." + entry.getKey().name(), entry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти mines.yml: " + e.getMessage());
        }
    }

    public Optional<Mine> getMine(String id) {
        return Optional.ofNullable(mines.get(id));
    }

    public Map<String, Mine> getAllMines() {
        return mines;
    }

    public void registerMine(Mine mine) {
        mines.put(mine.getId(), mine);
        save();
    }

    public boolean deleteMine(String id) {
        boolean removed = mines.remove(id) != null;
        if (removed) save();
        return removed;
    }

    /** Знаходить шахту, що містить локацію (якщо гравець зараз у ній копає). */
    public Optional<Mine> findMineAt(Location location) {
        for (Mine mine : mines.values()) {
            if (mine.contains(location)) {
                return Optional.of(mine);
            }
        }
        return Optional.empty();
    }

    /**
     * Заповнює шахту блоками згідно з composition (реген шахти). Щоб уникнути
     * lag-spike на великих шахтах, ставить лише N блоків за тік
     * (config: mines.regen-blocks-per-tick) і продовжує через BukkitRunnable,
     * доки не пройде весь об'єм. Якщо реген цієї шахти вже триває - виклик ігнорується.
     */
    public void resetMine(Mine mine) {
        World world = Bukkit.getWorld(mine.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("Світ " + mine.getWorldName() + " не завантажений, реген шахти " + mine.getId() + " пропущено.");
            return;
        }

        if (!resettingMines.add(mine.getId())) {
            return; // вже регенерується
        }

        int blocksPerTick = Math.max(50, plugin.getConfig().getInt("mines.regen-blocks-per-tick", 2000));
        boolean broadcast = plugin.getConfig().getBoolean("mines.broadcast-reset", true);

        new BukkitRunnable() {
            int x = mine.getX1();
            int y = mine.getY1();
            int z = mine.getZ1();

            @Override
            public void run() {
                int processed = 0;
                while (processed < blocksPerTick) {
                    world.getBlockAt(x, y, z).setType(mine.randomMaterial(), false);
                    processed++;

                    z++;
                    if (z > mine.getZ2()) {
                        z = mine.getZ1();
                        y++;
                        if (y > mine.getY2()) {
                            y = mine.getY1();
                            x++;
                            if (x > mine.getX2()) {
                                // весь об'єм пройдено
                                resettingMines.remove(mine.getId());
                                if (broadcast) {
                                    Bukkit.broadcastMessage(plugin.getMessages().get("mine-reset-broadcast", "mine", mine.getId()));
                                }
                                cancel();
                                return;
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean isResetting(String mineId) {
        return resettingMines.contains(mineId);
    }
}
