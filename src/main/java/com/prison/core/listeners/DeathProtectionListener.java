package com.prison.core.listeners;

import com.prison.core.PrisonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * При смерті гравця кирка, сокира, лопата, меч, броня, ножиці та вудка НЕ
 * випадають на землю (і не губляться) - вони прибираються зі списку дропу
 * в момент смерті й повертаються гравцю одразу після респавну. Все інше
 * (блоки, їжа, звичайні предмети) випадає як зазвичай.
 */
public class DeathProtectionListener implements Listener {

    private static final Set<Material> PROTECTED_EXACT = EnumSet.of(Material.SHEARS, Material.FISHING_ROD);

    private final PrisonPlugin plugin;
    private final Map<UUID, List<ItemStack>> pendingReturn = new HashMap<>();

    public DeathProtectionListener(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean isProtected(Material type) {
        String name = type.name();
        return name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_SWORD")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || PROTECTED_EXACT.contains(type);
    }

    private static boolean isArmor(Material type) {
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> keep = new ArrayList<>();

        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item != null && item.getType() != Material.AIR && isProtected(item.getType())) {
                keep.add(item);
                iterator.remove();
            }
        }

        if (!keep.isEmpty()) {
            pendingReturn.put(event.getEntity().getUniqueId(), keep);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> items = pendingReturn.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) {
            return;
        }

        // Через тік - одразу після респавну інвентар гарантовано готовий приймати предмети
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack item : items) {
                if (isArmor(item.getType()) && tryEquipArmor(inventory, item)) {
                    continue;
                }
                Map<Integer, ItemStack> leftover = inventory.addItem(item);
                for (ItemStack extra : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), extra);
                }
            }
        });
    }

    /** Пробує одразу вдягнути броню у відповідний слот, якщо він вільний. @return true якщо вдягнуто */
    private boolean tryEquipArmor(PlayerInventory inventory, ItemStack item) {
        String name = item.getType().name();
        if (name.endsWith("_HELMET") && isEmpty(inventory.getHelmet())) {
            inventory.setHelmet(item);
            return true;
        }
        if (name.endsWith("_CHESTPLATE") && isEmpty(inventory.getChestplate())) {
            inventory.setChestplate(item);
            return true;
        }
        if (name.endsWith("_LEGGINGS") && isEmpty(inventory.getLeggings())) {
            inventory.setLeggings(item);
            return true;
        }
        if (name.endsWith("_BOOTS") && isEmpty(inventory.getBoots())) {
            inventory.setBoots(item);
            return true;
        }
        return false;
    }

    private boolean isEmpty(ItemStack slot) {
        return slot == null || slot.getType() == Material.AIR;
    }
}
