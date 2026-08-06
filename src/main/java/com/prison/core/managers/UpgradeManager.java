package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Прокачка спорядження за принципом VimeWorld: предмет фізично змінює
 * матеріал (дерево -> камінь -> залізо -> алмаз), і кожен тір несе один
 * або декілька рівнів зачарування (напр. фінальний тір лопати одразу дає
 * Ефективність III + Удачу I). Перехід на наступний тір коштує монети +
 * певну кількість сировини (config: upgrades.<category>.tiers). Кінцевий
 * предмет завжди незламний (ItemMeta#setUnbreakable(true)).
 *
 * Лопати (SHOVEL) винесені в окрему категорію від решти інструментів -
 * прокачуються не рудою, а земляною сировиною (землею/піском/гравієм),
 * тематично пов'язаною з копанням.
 *
 * Поточний тір предмета НЕ зберігається окремо (без PDC-тегів) - він
 * визначається напряму з фізичних властивостей предмета (матеріал +
 * набір зачарувань), що завжди збігається з одним із рядків tiers.
 */
public class UpgradeManager {

    public enum Category { TOOL, SHOVEL, SWORD, ARMOR }

    public record EnchantSpec(Enchantment enchantment, int level) {
    }

    public static class Tier {
        public final String materialPrefix;
        public final List<EnchantSpec> enchants;
        public final double costCoins;
        public final Material costMaterial;
        public final int costAmount;

        public Tier(String materialPrefix, List<EnchantSpec> enchants, double costCoins, Material costMaterial, int costAmount) {
            this.materialPrefix = materialPrefix;
            this.enchants = enchants;
            this.costCoins = costCoins;
            this.costMaterial = costMaterial;
            this.costAmount = costAmount;
        }
    }

    // SHOVEL винесений в окрему категорію - НЕ входить у TOOL_SUFFIXES
    private static final String[] TOOL_SUFFIXES = {"PICKAXE", "AXE", "HOE"};
    private static final String[] SHOVEL_SUFFIXES = {"SHOVEL"};
    private static final String[] SWORD_SUFFIXES = {"SWORD"};
    private static final String[] ARMOR_SUFFIXES = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

    private final PrisonPlugin plugin;
    private final Map<Category, List<Tier>> tiers = new EnumMap<>(Category.class);

    public UpgradeManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        tiers.clear();
        loadCategory(Category.TOOL, "tool");
        loadCategory(Category.SHOVEL, "shovel");
        loadCategory(Category.SWORD, "sword");
        loadCategory(Category.ARMOR, "armor");
        plugin.getLogger().info("Завантажено тірів прокачки: TOOL=" + tiers.get(Category.TOOL).size()
                + " SHOVEL=" + tiers.get(Category.SHOVEL).size()
                + " SWORD=" + tiers.get(Category.SWORD).size() + " ARMOR=" + tiers.get(Category.ARMOR).size());
    }

    @SuppressWarnings("unchecked")
    private void loadCategory(Category category, String path) {
        List<Tier> list = new ArrayList<>();

        for (Map<?, ?> raw : plugin.getConfig().getMapList("upgrades." + path + ".tiers")) {
            String material = String.valueOf(raw.get("material"));
            double costCoins = raw.get("cost-coins") instanceof Number n ? n.doubleValue() : 0;
            Material costMaterial = raw.get("cost-material") != null
                    ? Material.matchMaterial(String.valueOf(raw.get("cost-material"))) : null;
            int costAmount = raw.get("cost-amount") instanceof Number n ? n.intValue() : 0;

            List<EnchantSpec> enchants = new ArrayList<>();
            Object enchantsRaw = raw.get("enchants");
            if (enchantsRaw instanceof List<?> enchantList) {
                for (Object o : enchantList) {
                    if (!(o instanceof Map<?, ?> enchantMap)) continue;
                    String enchantName = String.valueOf(enchantMap.get("enchant"));
                    int level = enchantMap.get("level") instanceof Number n ? n.intValue() : 0;
                    Enchantment enchantment = Enchantment.getByName(enchantName);
                    if (enchantment != null) {
                        enchants.add(new EnchantSpec(enchantment, level));
                    } else {
                        plugin.getLogger().warning("Невідоме зачарування у upgrades." + path + ".tiers: " + enchantName);
                    }
                }
            }

            list.add(new Tier(material, enchants, costCoins, costMaterial, costAmount));
        }

        tiers.put(category, list);
    }

    /** Визначає суфікс типу предмета (PICKAXE/SHOVEL/SWORD/HELMET тощо), або null якщо не апгрейджиться. */
    private String suffixOf(Material type) {
        String name = type.name();
        for (String suffix : TOOL_SUFFIXES) {
            if (name.endsWith("_" + suffix)) return suffix;
        }
        for (String suffix : SHOVEL_SUFFIXES) {
            if (name.endsWith("_" + suffix)) return suffix;
        }
        for (String suffix : SWORD_SUFFIXES) {
            if (name.endsWith("_" + suffix)) return suffix;
        }
        for (String suffix : ARMOR_SUFFIXES) {
            if (name.endsWith("_" + suffix)) return suffix;
        }
        return null;
    }

    /** Матеріальний префікс предмета (напр. "STONE" для STONE_PICKAXE), або null. */
    private String materialPrefixOf(Material type) {
        String suffix = suffixOf(type);
        if (suffix == null) return null;
        String name = type.name();
        return name.substring(0, name.length() - suffix.length() - 1);
    }

    public Category categoryOf(ItemStack item) {
        if (item == null) return null;
        return categoryOf(item.getType());
    }

    public Category categoryOf(Material type) {
        String suffix = suffixOf(type);
        if (suffix == null) return null;
        for (String s : TOOL_SUFFIXES) if (s.equals(suffix)) return Category.TOOL;
        for (String s : SHOVEL_SUFFIXES) if (s.equals(suffix)) return Category.SHOVEL;
        for (String s : SWORD_SUFFIXES) if (s.equals(suffix)) return Category.SWORD;
        for (String s : ARMOR_SUFFIXES) if (s.equals(suffix)) return Category.ARMOR;
        return null;
    }

    public List<Tier> getTiers(Category category) {
        return tiers.getOrDefault(category, List.of());
    }

    private boolean matchesTier(ItemStack item, Tier tier) {
        String prefix = materialPrefixOf(item.getType());
        if (!tier.materialPrefix.equalsIgnoreCase(prefix)) return false;
        for (EnchantSpec spec : tier.enchants) {
            if (item.getEnchantmentLevel(spec.enchantment()) != spec.level()) return false;
        }
        return true;
    }

    /** Індекс поточного тіра предмета (за матеріалом+набором зачарувань). 0 якщо не знайдено точного збігу. */
    public int currentTierIndex(ItemStack item, Category category) {
        List<Tier> list = getTiers(category);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (matchesTier(item, list.get(i))) {
                return i;
            }
        }
        return 0;
    }

    public Optional<Tier> getNextTier(ItemStack item, Category category) {
        int idx = currentTierIndex(item, category);
        List<Tier> list = getTiers(category);
        if (idx + 1 < list.size()) {
            return Optional.of(list.get(idx + 1));
        }
        return Optional.empty();
    }

    public boolean isMaxTier(ItemStack item, Category category) {
        return getNextTier(item, category).isEmpty();
    }

    /** Будує новий ItemStack для заданого тіра (той самий "суфікс" типу предмета, що й у оригіналу). */
    public ItemStack buildItemForTier(Material originalType, Category category, Tier tier) {
        String suffix = suffixOf(originalType);
        Material newType = Material.matchMaterial(tier.materialPrefix + "_" + suffix);
        if (newType == null) {
            newType = originalType; // захист від помилки в конфізі - краще лишити як було, ніж впасти
        }

        ItemStack item = new ItemStack(newType);
        for (EnchantSpec spec : tier.enchants) {
            if (spec.level() > 0) {
                item.addUnsafeEnchantment(spec.enchantment(), spec.level());
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true); // головна вимога - апгрейднуте спорядження ніколи не ламається
            item.setItemMeta(meta);
        }

        return item;
    }
}
