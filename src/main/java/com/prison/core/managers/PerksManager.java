package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Пасивні бонуси за рівень: додаткові серця здоров'я та додатковий урон.
 * Обидва застосовуються через справжні Bukkit AttributeModifier (реально
 * впливають на бій, а не косметика) з фіксованим UUID - тому повторне
 * застосування (при вході чи підвищенні рівня) коректно ЗАМІНЮЄ попереднє
 * значення, а не стакається щоразу.
 */
public class PerksManager {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-0001-4000-8000-000000000001");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("a1b2c3d4-0002-4000-8000-000000000002");

    private final PrisonPlugin plugin;

    public PerksManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    /** Скільки додаткових "сердець" (по 2 HP кожне) дає рівень, з урахуванням ліміту полосок здоров'я. */
    public double getBonusHealth(int level) {
        List<Integer> levels = plugin.getConfig().getIntegerList("perks.hearts.levels");
        int hearts = 0;
        for (int milestone : levels) {
            if (level >= milestone) hearts++;
        }
        double bonus = hearts * 2.0; // 1 серце = 2 HP

        int maxBars = plugin.getConfig().getInt("perks.hearts.max-bars", 4);
        double maxBonus = Math.max(0, (maxBars - 1) * 20.0); // база - 1 полоска (20 HP), решта - бонус
        return Math.min(bonus, maxBonus);
    }

    /** Скільки додаткового урону дає рівень, з урахуванням ліміту (perks.damage.max-bonus). */
    public double getBonusDamage(int level) {
        List<Integer> levels = plugin.getConfig().getIntegerList("perks.damage.levels");
        int count = 0;
        for (int milestone : levels) {
            if (level >= milestone) count++;
        }
        double maxBonus = plugin.getConfig().getDouble("perks.damage.max-bonus", 6);
        return Math.min(count, maxBonus);
    }

    /**
     * Перераховує й застосовує обидва бонуси до гравця відповідно до його
     * поточного рівня. Викликати при вході на сервер та одразу після
     * підвищення/встановлення рівня.
     */
    public void applyPerks(Player player, PlayerData data) {
        applyHealth(player, data.getLevel());
        applyDamage(player, data.getLevel());
    }

    private void applyHealth(Player player, int level) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return;

        attribute.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(HEALTH_MODIFIER_UUID))
                .findFirst()
                .ifPresent(attribute::removeModifier);

        double bonus = getBonusHealth(level);
        if (bonus > 0) {
            attribute.addModifier(new AttributeModifier(HEALTH_MODIFIER_UUID, "prison_level_health",
                    bonus, AttributeModifier.Operation.ADD_NUMBER));
        }

        if (player.getHealth() > attribute.getValue()) {
            player.setHealth(attribute.getValue());
        }
    }

    private void applyDamage(Player player, int level) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attribute == null) return;

        attribute.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(DAMAGE_MODIFIER_UUID))
                .findFirst()
                .ifPresent(attribute::removeModifier);

        double bonus = getBonusDamage(level);
        if (bonus > 0) {
            attribute.addModifier(new AttributeModifier(DAMAGE_MODIFIER_UUID, "prison_level_damage",
                    bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
    }
}
