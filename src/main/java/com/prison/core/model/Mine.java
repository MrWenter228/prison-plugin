package com.prison.core.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Модель шахти: куб-регіон + склад блоків (composition) для регенерації.
 */
public class Mine {

    private final String id;
    private final String world;
    private final int x1, y1, z1, x2, y2, z2;
    private final Location teleport;
    /** Матеріал -> відсоток (0-100), сума має дорівнювати 100. */
    private final Map<Material, Double> composition;
    private final Random random = new Random();

    public Mine(String id, String world, int x1, int y1, int z1,
                int x2, int y2, int z2, Location teleport,
                Map<Material, Double> composition) {
        this.id = id;
        this.world = world;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.teleport = teleport;
        this.composition = composition == null ? new LinkedHashMap<>() : composition;
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return world;
    }

    public Location getTeleport() {
        return teleport;
    }

    public Map<Material, Double> getComposition() {
        return composition;
    }

    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) {
            return false;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public long totalBlocks() {
        return (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
    }

    /** Вибирає випадковий матеріал за вагами composition. AIR якщо composition порожній. */
    public Material randomMaterial() {
        if (composition.isEmpty()) {
            return Material.STONE;
        }
        double roll = random.nextDouble() * 100.0;
        double cumulative = 0;
        for (Map.Entry<Material, Double> entry : composition.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        // fallback через похибки округлення
        return composition.keySet().iterator().next();
    }

    /**
     * Точний підрахунок частки "непорожніх" блоків. Дорого для великих шахт -
     * використовуйте sampleFillRatio() для періодичних перевірок на живому сервері.
     */
    public double calculateFillRatio(World bukkitWorld) {
        long total = totalBlocks();
        if (total == 0) return 1.0;
        long filled = 0;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (bukkitWorld.getBlockAt(x, y, z).getType() != Material.AIR) {
                        filled++;
                    }
                }
            }
        }
        return (double) filled / (double) total;
    }

    /**
     * Швидка оцінка частки заповненості шляхом випадкової вибірки N точок.
     * Значно дешевше за calculateFillRatio() на великих шахтах і достатньо
     * точне (закон великих чисел) для прийняття рішення про ресет.
     */
    public double sampleFillRatio(World bukkitWorld, int samples) {
        long total = totalBlocks();
        if (total == 0) return 1.0;
        if (samples >= total) {
            return calculateFillRatio(bukkitWorld);
        }
        int filled = 0;
        int width = x2 - x1 + 1;
        int height = y2 - y1 + 1;
        int depth = z2 - z1 + 1;
        for (int i = 0; i < samples; i++) {
            int x = x1 + random.nextInt(width);
            int y = y1 + random.nextInt(height);
            int z = z1 + random.nextInt(depth);
            if (bukkitWorld.getBlockAt(x, y, z).getType() != Material.AIR) {
                filled++;
            }
        }
        return (double) filled / (double) samples;
    }
}
