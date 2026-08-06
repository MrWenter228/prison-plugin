package com.prison.core.model;

import java.util.UUID;

/**
 * Прогрес одного гравця. Тримається в пам'яті (кеш) і синхронізується
 * з диском через PlayerDataManager.
 */
public class PlayerData {

    private final UUID uuid;
    private int level;
    private long blocksMinedForLevel;
    private double balance;
    private double rubles;
    private boolean autoSellEnabled;
    private String locale;
    /** Час (System.currentTimeMillis), до якого діє бустер. 0 = бустера немає/минув. */
    private long coinsBoosterExpiry;
    private long blocksBoosterExpiry;
    /** Сила активного бустера (2/3/5) - має сенс лише поки відповідний expiry ще не минув. */
    private double coinsBoosterMultiplier;
    private double blocksBoosterMultiplier;

    public PlayerData(UUID uuid, int level, long blocksMinedForLevel, double balance, double rubles,
                       boolean autoSellEnabled, String locale, long coinsBoosterExpiry, long blocksBoosterExpiry,
                       double coinsBoosterMultiplier, double blocksBoosterMultiplier) {
        this.uuid = uuid;
        this.level = level;
        this.blocksMinedForLevel = blocksMinedForLevel;
        this.balance = balance;
        this.rubles = rubles;
        this.autoSellEnabled = autoSellEnabled;
        this.locale = locale;
        this.coinsBoosterExpiry = coinsBoosterExpiry;
        this.blocksBoosterExpiry = blocksBoosterExpiry;
        this.coinsBoosterMultiplier = coinsBoosterMultiplier;
        this.blocksBoosterMultiplier = blocksBoosterMultiplier;
    }

    public UUID getUuid() {
        return uuid;
    }

    /** Рівень гравця (1-40, як на VimeWorld). Відображається на ванільній XP-панелі. */
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /** Скільки блоків видобуто в шахтах з моменту останнього підвищення рівня. */
    public long getBlocksMinedForLevel() {
        return blocksMinedForLevel;
    }

    public void setBlocksMinedForLevel(long blocksMinedForLevel) {
        this.blocksMinedForLevel = blocksMinedForLevel;
    }

    public void addBlocksMined(long amount) {
        this.blocksMinedForLevel += amount;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    /** @return true якщо списання вдалося (було достатньо коштів) */
    public boolean subtractBalance(double amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public boolean isAutoSellEnabled() {
        return autoSellEnabled;
    }

    public void setAutoSellEnabled(boolean autoSellEnabled) {
        this.autoSellEnabled = autoSellEnabled;
    }

    public double getRubles() {
        return rubles;
    }

    public void setRubles(double rubles) {
        this.rubles = rubles;
    }

    public void addRubles(double amount) {
        this.rubles += amount;
    }

    /** @return true якщо списання вдалося (було достатньо рублів) */
    public boolean subtractRubles(double amount) {
        if (rubles < amount) {
            return false;
        }
        rubles -= amount;
        return true;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public long getCoinsBoosterExpiry() {
        return coinsBoosterExpiry;
    }

    public void setCoinsBoosterExpiry(long coinsBoosterExpiry) {
        this.coinsBoosterExpiry = coinsBoosterExpiry;
    }

    public long getBlocksBoosterExpiry() {
        return blocksBoosterExpiry;
    }

    public void setBlocksBoosterExpiry(long blocksBoosterExpiry) {
        this.blocksBoosterExpiry = blocksBoosterExpiry;
    }

    public double getCoinsBoosterMultiplier() {
        return coinsBoosterMultiplier;
    }

    public void setCoinsBoosterMultiplier(double coinsBoosterMultiplier) {
        this.coinsBoosterMultiplier = coinsBoosterMultiplier;
    }

    public double getBlocksBoosterMultiplier() {
        return blocksBoosterMultiplier;
    }

    public void setBlocksBoosterMultiplier(double blocksBoosterMultiplier) {
        this.blocksBoosterMultiplier = blocksBoosterMultiplier;
    }
}
