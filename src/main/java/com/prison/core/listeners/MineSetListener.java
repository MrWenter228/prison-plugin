package com.prison.core.listeners;

import com.prison.core.PrisonPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MineSetListener implements Listener {

    private final PrisonPlugin plugin;

    public MineSetListener(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        String tag = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getMineWandKey(), PersistentDataType.STRING);
        if (tag == null || !tag.equals("wand")) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getSelectionManager().setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
            plugin.getMessages().send(player, "minewand-pos1-set", "coords", format(event.getClickedBlock().getLocation()));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getSelectionManager().setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
            plugin.getMessages().send(player, "minewand-pos2-set", "coords", format(event.getClickedBlock().getLocation()));
        }
    }

    private String format(org.bukkit.Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
