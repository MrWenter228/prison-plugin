package com.prison.core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Тримач компактного меню /lvl. Minecraft вимагає, щоб розмір chest-інвентаря
 * був кратний 9 (7 слотів технічно неможливі), тож використовується
 * найменший можливий розмір - один рядок з 9 слотів. Активний лише
 * центральний слот (пляшка досвіду), решта - декоративне скляне обрамлення.
 */
public class LevelHolder implements InventoryHolder {

    public static final int LEVELUP_SLOT = 4;

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
