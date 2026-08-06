package com.prison.core.listeners;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PrisonPlugin plugin;

    public PlayerConnectionListener(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Прогрівання кешу одразу при вході, щоб уникнути затримки на першій команді
        PlayerData data = plugin.getPlayerDataManager().load(event.getPlayer().getUniqueId());
        // Синхронізуємо ванільну XP-панель з ігровим рівнем гравця одразу при заході
        plugin.getLevelManager().updateXpBar(event.getPlayer(), data);
        // Застосовуємо пасивні бонуси рівня (додаткові серця/урон)
        plugin.getPerksManager().applyPerks(event.getPlayer(), data);
        // Видаємо предмети з "пошти" (напр. протухлі лоти аукціону, поки гравець був офлайн)
        if (plugin.getMailboxManager().hasMail(event.getPlayer().getUniqueId())) {
            plugin.getMailboxManager().deliver(event.getPlayer());
            plugin.getMessages().send(event.getPlayer(), "mailbox-delivered");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}
