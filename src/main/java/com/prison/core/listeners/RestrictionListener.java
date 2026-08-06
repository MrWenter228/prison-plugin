package com.prison.core.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Обмеження ігрового світу під кастомну систему рівнів плагіна:
 *  - крафт повністю вимкнено (незалежно від рецепта);
 *  - досвід за вбивство мобів не нараховується (щоб не збивав кастомну
 *    XP-панель, яку контролює LevelManager - вона показує ігровий рівень
 *    плагіна, а не ванільний досвід).
 *
 * Дві точки блокування XP навмисно дублюються:
 *  - EntityDeathEvent#setDroppedExp(0) - не дає з'явитись орбам досвіду взагалі;
 *  - PlayerExpChangeEvent - підстраховка на випадок XP з інших джерел
 *    (плавлення в печі, риболовля, розведення тварин тощо), щоб жодним чином
 *    не збити ванільну XP-панель, якою керує LevelManager.
 */
public class RestrictionListener implements Listener {

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }
}
