package com.prison.core.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Один лот на аукціоні: хто продає, що продає, за скільки, в якій валюті, і чи є персональною пропозицією. */
public class AuctionListing {

    private final UUID id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final Currency currency;
    private final long listedAt;
    private final long expiresAt;
    /** null = публічний лот, видимий усім. Не-null = персональна пропозиція лише для цього гравця. */
    private final UUID targetPlayerUuid;
    private final String targetPlayerName;

    public AuctionListing(UUID id, UUID sellerUuid, String sellerName, ItemStack item,
                           double price, Currency currency, long listedAt, long expiresAt,
                           UUID targetPlayerUuid, String targetPlayerName) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.currency = currency;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = targetPlayerName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long getListedAt() {
        return listedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingMillis() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public boolean isPersonal() {
        return targetPlayerUuid != null;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }
}
