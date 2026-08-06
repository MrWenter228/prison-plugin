package com.prison.core.gui;

import com.prison.core.model.Currency;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionHolder implements InventoryHolder {

    public enum Tab { ALL, PERSONAL, MINE }
    public enum Sort { TIME_DESC, TIME_ASC, PRICE_ASC, PRICE_DESC }

    public static final int SLOT_BACK = 45;
    public static final int SLOT_TAB_ALL = 46;
    public static final int SLOT_TAB_PERSONAL = 47;
    public static final int SLOT_TAB_MINE = 48;
    public static final int SLOT_SORT = 49;
    public static final int SLOT_STATS = 50;
    public static final int SLOT_REFRESH = 51;
    public static final int SLOT_PREV = 52;
    public static final int SLOT_NEXT = 53;

    private Inventory inventory;
    private final Currency currency;
    private Tab tab = Tab.ALL;
    private Sort sort = Sort.TIME_DESC;
    private int page = 0;
    private boolean hasNextPage;
    private final Map<Integer, UUID> slotMap = new HashMap<>();

    public AuctionHolder(Currency currency) {
        this.currency = currency;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Tab getTab() {
        return tab;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public Sort nextSort() {
        Sort[] values = Sort.values();
        return values[(sort.ordinal() + 1) % values.length];
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public void put(int slot, UUID listingId) {
        slotMap.put(slot, listingId);
    }

    public UUID get(int slot) {
        return slotMap.get(slot);
    }
}
