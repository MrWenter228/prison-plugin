package com.prison.core;

import com.prison.core.commands.*;
import com.prison.core.gui.GUIListener;
import com.prison.core.listeners.BlockBreakListener;
import com.prison.core.listeners.DeathProtectionListener;
import com.prison.core.listeners.MineSetListener;
import com.prison.core.listeners.PlayerConnectionListener;
import com.prison.core.listeners.RestrictionListener;
import com.prison.core.managers.AuctionManager;
import com.prison.core.managers.BoosterManager;
import com.prison.core.managers.EconomyManager;
import com.prison.core.managers.LanguageManager;
import com.prison.core.managers.LevelManager;
import com.prison.core.managers.MailboxManager;
import com.prison.core.managers.MineManager;
import com.prison.core.managers.PerksManager;
import com.prison.core.managers.PlayerDataManager;
import com.prison.core.managers.SelectionManager;
import com.prison.core.managers.UpgradeManager;
import com.prison.core.model.Currency;
import com.prison.core.placeholder.PrisonPlaceholders;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrisonPlugin extends JavaPlugin {

    private static PrisonPlugin instance;

    private MineManager mineManager;
    private EconomyManager economyManager;
    private PlayerDataManager playerDataManager;
    private LanguageManager languageManager;
    private SelectionManager selectionManager;
    private LevelManager levelManager;
    private UpgradeManager upgradeManager;
    private AuctionManager auctionManager;
    private MailboxManager mailboxManager;
    private BoosterManager boosterManager;
    private PerksManager perksManager;
    private NamespacedKey mineWandKey;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        mineWandKey = new NamespacedKey(this, "mine_wand");

        mineManager = new MineManager(this);
        mineManager.load();

        economyManager = new EconomyManager(this);
        economyManager.load();

        playerDataManager = new PlayerDataManager(this);
        playerDataManager.init();

        languageManager = new LanguageManager(this);
        languageManager.load();

        selectionManager = new SelectionManager();

        levelManager = new LevelManager(this);
        levelManager.load();

        upgradeManager = new UpgradeManager(this);
        upgradeManager.load();

        mailboxManager = new MailboxManager(this);
        mailboxManager.load();

        auctionManager = new AuctionManager(this);
        auctionManager.load();

        boosterManager = new BoosterManager(this);
        perksManager = new PerksManager(this);

        registerCommands();
        registerListeners();
        registerPlaceholders();
        startAutoSaveTask();
        startMineWatcherTask();
        startAuctionExpiryTask();

        getLogger().info("Prison увімкнено. Шахт: " + mineManager.getAllMines().size());
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (mineManager != null) {
            mineManager.save();
        }
        if (auctionManager != null) {
            auctionManager.save();
        }
        if (mailboxManager != null) {
            mailboxManager.save();
        }
        getLogger().info("Prison вимкнено, дані збережено.");
    }

    private void registerCommands() {
        getCommand("lvl").setExecutor(new LevelCommand(this));
        getCommand("mines").setExecutor(new MinesCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("minewand").setExecutor(new MineWandCommand(this));
        getCommand("mineset").setExecutor(new MineSetCommand(this));
        getCommand("minesgui").setExecutor(new MinesGuiCommand(this));
        getCommand("upgrade").setExecutor(new UpgradeCommand(this));
        getCommand("autosell").setExecutor(new AutosellCommand(this));
        getCommand("auc").setExecutor(new AuctionCommand(this, Currency.COINS));
        getCommand("rauc").setExecutor(new AuctionCommand(this, Currency.RUBLES));
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("rubles").setExecutor(new RublesCommand(this));
        getCommand("booster").setExecutor(new BoosterCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MineSetListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(), this);
    }

    /**
     * Реєструє %prison_...% плейсхолдери в PlaceholderAPI, якщо він встановлений
     * і увімкнений. Клас PrisonPlaceholders завантажується лише всередині цього
     * блоку (лінива класозавантаженість) - без наявного PlaceholderAPI.jar на
     * класпаті виклик new PrisonPlaceholders() кине NoClassDefFoundError, тож
     * перевірка getPlugin("PlaceholderAPI") != null є обов'язковою і йде першою.
     */
    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI не знайдено - плейсхолдери %prison_...% пропущено.");
            return;
        }
        new PrisonPlaceholders(this).register();
        getLogger().info("PlaceholderAPI знайдено - плейсхолдери %prison_...% зареєстровано.");
    }

    /** Періодичне автозбереження балансів/прогресу гравців, щоб мінімізувати втрати при крашах. */
    private void startAutoSaveTask() {
        long intervalTicks = 20L * 60L * 5L; // кожні 5 хвилин
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            playerDataManager.saveAll();
        }, intervalTicks, intervalTicks);
    }

    /** Періодична перевірка виробленості шахт та автоматичний реген при досягненні порогу. */
    private void startMineWatcherTask() {
        long intervalTicks = 20L * getConfig().getLong("mines.check-interval-seconds", 15);
        double threshold = getConfig().getDouble("mines.auto-reset-threshold", 0.4);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var mine : mineManager.getAllMines().values()) {
                var world = getServer().getWorld(mine.getWorldName());
                if (world == null) continue;

                int samples = Math.min(500, (int) Math.max(50, mine.totalBlocks() / 20));
                double ratio = mine.sampleFillRatio(world, samples);
                if (ratio < threshold) {
                    mineManager.resetMine(mine);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    /** Періодичне зняття протухлих лотів аукціону (повернення продавцю або в поштову скриньку). */
    private void startAuctionExpiryTask() {
        long intervalTicks = 20L * getConfig().getLong("auction.expiry-check-interval-seconds", 300);
        getServer().getScheduler().runTaskTimer(this, () -> auctionManager.purgeExpired(), intervalTicks, intervalTicks);
    }

    public static PrisonPlugin getInstance() {
        return instance;
    }

    public MineManager getMineManager() {
        return mineManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    /** Назва методу збережена як getMessages() для сумісності з рештою коду. */
    public LanguageManager getMessages() {
        return languageManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public NamespacedKey getMineWandKey() {
        return mineWandKey;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public MailboxManager getMailboxManager() {
        return mailboxManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public PerksManager getPerksManager() {
        return perksManager;
    }
}
