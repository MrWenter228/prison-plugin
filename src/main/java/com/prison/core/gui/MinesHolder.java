package com.prison.core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class MinesHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, String> slotToMine = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void put(int slot, String mineId) {
        slotToMine.put(slot, mineId);
    }

    public String get(int slot) {
        return slotToMine.get(slot);
    }
}
