package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.PlayerData;

/**
 * Бустери монет і блоків - тимчасові множники, що купуються за рублі
 * (або видаються адміном). Три рівні сили - x2, x3, x5 - кожен окремо на
 * день/тиждень/місяць. При покупці нового бустера термін ДОДАЄТЬСЯ до вже
 * активного (якщо був), а сила одразу перемикається на щойно куплену.
 */
public class BoosterManager {

    public enum Type { COINS, BLOCKS }
    public enum Duration { DAY, WEEK, MONTH }
    public enum PurchaseResult { SUCCESS, NOT_ENOUGH_RUBLES }

    /** Підтримувані сили бустера - лише ці значення допустимі як multiplier. */
    public static final int[] MULTIPLIERS = {2, 3, 5};

    private final PrisonPlugin plugin;

    public BoosterManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    private String path(Type type, int multiplier) {
        return "boosters." + (type == Type.COINS ? "coins" : "blocks") + ".tiers." + multiplier;
    }

    public double getPrice(Type type, int multiplier, Duration duration) {
        return plugin.getConfig().getDouble(path(type, multiplier) + ".price-" + durationKey(duration), 0);
    }

    private String durationKey(Duration duration) {
        return switch (duration) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
        };
    }

    public long getDurationMillis(Duration duration) {
        return switch (duration) {
            case DAY -> 24 * 3600_000L;
            case WEEK -> 7 * 24 * 3600_000L;
            case MONTH -> 30 * 24 * 3600_000L;
        };
    }

    private long getExpiry(PlayerData data, Type type) {
        return type == Type.COINS ? data.getCoinsBoosterExpiry() : data.getBlocksBoosterExpiry();
    }

    private void setExpiry(PlayerData data, Type type, long expiry) {
        if (type == Type.COINS) {
            data.setCoinsBoosterExpiry(expiry);
        } else {
            data.setBlocksBoosterExpiry(expiry);
        }
    }

    private void setActiveMultiplier(PlayerData data, Type type, double multiplier) {
        if (type == Type.COINS) {
            data.setCoinsBoosterMultiplier(multiplier);
        } else {
            data.setBlocksBoosterMultiplier(multiplier);
        }
    }

    private double getActiveMultiplierRaw(PlayerData data, Type type) {
        return type == Type.COINS ? data.getCoinsBoosterMultiplier() : data.getBlocksBoosterMultiplier();
    }

    public boolean isActive(PlayerData data, Type type) {
        return getExpiry(data, type) > System.currentTimeMillis();
    }

    /** Поточний множник з урахуванням активності бустера (1.0 якщо не активний). */
    public double getActiveMultiplier(PlayerData data, Type type) {
        return isActive(data, type) ? getActiveMultiplierRaw(data, type) : 1.0;
    }

    public long getRemainingMillis(PlayerData data, Type type) {
        return Math.max(0, getExpiry(data, type) - System.currentTimeMillis());
    }

    /** Продає бустер за рублі. Термін додається до поточного, сила одразу перемикається на нову покупку. */
    public PurchaseResult purchase(PlayerData data, Type type, int multiplier, Duration duration) {
        double price = getPrice(type, multiplier, duration);
        if (!data.subtractRubles(price)) {
            return PurchaseResult.NOT_ENOUGH_RUBLES;
        }
        grant(data, type, multiplier, duration);
        return PurchaseResult.SUCCESS;
    }

    /** Видає бустер напряму без оплати (адмінська команда). */
    public void grant(PlayerData data, Type type, int multiplier, Duration duration) {
        long now = System.currentTimeMillis();
        long base = Math.max(now, getExpiry(data, type));
        setExpiry(data, type, base + getDurationMillis(duration));
        setActiveMultiplier(data, type, multiplier);
    }
}
