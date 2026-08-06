package com.prison.core.model;

/**
 * Дві валюти плагіна:
 *  COINS  (монети) - основна ігрова валюта, заробляється видобутком/автопродажем блоків
 *  RUBLES (рублі)  - друга валюта, видається адміном (/prisoneco) або через аукціон;
 *                    використовується як альтернативна валюта на аукціоні
 */
public enum Currency {
    COINS,
    RUBLES;

    public static Currency fromString(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase()) {
            case "coin", "coins", "монеты", "монети", "монета" -> COINS;
            case "ruble", "rubles", "рубль", "рубли", "рублі" -> RUBLES;
            default -> null;
        };
    }

    public double get(PlayerData data) {
        return this == COINS ? data.getBalance() : data.getRubles();
    }

    public void set(PlayerData data, double amount) {
        if (this == COINS) data.setBalance(amount);
        else data.setRubles(amount);
    }

    public void add(PlayerData data, double amount) {
        if (this == COINS) data.addBalance(amount);
        else data.addRubles(amount);
    }

    public boolean subtract(PlayerData data, double amount) {
        return this == COINS ? data.subtractBalance(amount) : data.subtractRubles(amount);
    }
}
