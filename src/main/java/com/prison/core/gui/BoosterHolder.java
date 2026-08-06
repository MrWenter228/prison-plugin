package com.prison.core.gui;

import com.prison.core.managers.BoosterManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Маркер меню /booster. Сітка: для кожного типу (COINS/BLOCKS) - один рядок
 * статусу + один рядок з 9 кнопками (3 сили х2/х3/х5 x 3 терміни день/тиждень/місяць).
 */
public class BoosterHolder implements InventoryHolder {

    public static final int COINS_STATUS_SLOT = 4;
    public static final int COINS_BUTTONS_START = 9;   // 9..17
    public static final int BLOCKS_STATUS_SLOT = 31;
    public static final int BLOCKS_BUTTONS_START = 36;  // 36..44

    private Inventory inventory;
    private final Map<Integer, BoosterManager.Type> typeBySlot = new HashMap<>();
    private final Map<Integer, Integer> multiplierBySlot = new HashMap<>();
    private final Map<Integer, BoosterManager.Duration> durationBySlot = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void register(int slot, BoosterManager.Type type, int multiplier, BoosterManager.Duration duration) {
        typeBySlot.put(slot, type);
        multiplierBySlot.put(slot, multiplier);
        durationBySlot.put(slot, duration);
    }

    public BoosterManager.Type getType(int slot) {
        return typeBySlot.get(slot);
    }

    public Integer getMultiplier(int slot) {
        return multiplierBySlot.get(slot);
    }

    public BoosterManager.Duration getDuration(int slot) {
        return durationBySlot.get(slot);
    }

    /** Слот для комбінації (рядок сили х2/х3/х5, стовпець день/тиждень/місяць), 0-based обидва. */
    public static int slotFor(int buttonsStart, int multiplierIndex, int durationIndex) {
        return buttonsStart + multiplierIndex * 3 + durationIndex;
    }
}
