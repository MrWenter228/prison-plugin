package com.prison.core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Маркер меню /upgrade. Драбина тірів малюється зліва направо (слоти 0..N),
 * тому слот наступного доступного тіра (єдиний клікабельний) змінюється
 * залежно від того, на якому тірі зараз предмет гравця - зберігається тут.
 */
public class UpgradeHolder implements InventoryHolder {

    private Inventory inventory;
    private int actionSlot = -1; // -1 = немає доступного апгрейду (вже максимальний тір)

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setActionSlot(int actionSlot) {
        this.actionSlot = actionSlot;
    }

    public int getActionSlot() {
        return actionSlot;
    }
}
