package com.prison.core.managers;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.AuctionListing;
import com.prison.core.model.Currency;
import com.prison.core.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Аукціон між гравцями: виставити предмет за монети або рублі (окремі
 * "ринки" - /auc та /rauc), будь-хто може купити, або приватно запропонувати
 * конкретному гравцю. Лоти зберігаються в auctions.yml, переживають рестарт
 * сервера. Протухлі (expired) лоти автоматично знімаються фоновою задачею -
 * предмет повертається продавцю миттєво (якщо онлайн) або в поштову скриньку
 * (MailboxManager), якщо офлайн.
 */
public class AuctionManager {

    public enum BuyResult { SUCCESS, NOT_FOUND, OWN_ITEM, NOT_ENOUGH_MONEY, NOT_YOUR_OFFER }
    public enum CancelResult { SUCCESS, NOT_FOUND, NOT_OWNER }
    public enum ListResult { SUCCESS, LIMIT_REACHED, INVALID_PRICE, TARGET_NOT_FOUND }

    private final PrisonPlugin plugin;
    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();
    private File file;

    public AuctionManager(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "auctions.yml");
        listings.clear();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("listings");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            try {
                UUID id = UUID.fromString(key);
                UUID seller = UUID.fromString(s.getString("seller"));
                String sellerName = s.getString("sellerName", "?");
                ItemStack item = s.getItemStack("item");
                double price = s.getDouble("price");
                Currency currency = Currency.valueOf(s.getString("currency", "COINS"));
                long listedAt = s.getLong("listedAt");
                long expiresAt = s.getLong("expiresAt", listedAt + defaultDurationMillis());
                UUID targetUuid = s.contains("targetUuid") ? UUID.fromString(s.getString("targetUuid")) : null;
                String targetName = s.getString("targetName", null);

                if (item != null) {
                    listings.put(id, new AuctionListing(id, seller, sellerName, item, price, currency,
                            listedAt, expiresAt, targetUuid, targetName));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Пропущено некоректний лот аукціону: " + key + " (" + e.getMessage() + ")");
            }
        }
        plugin.getLogger().info("Завантажено лотів аукціону: " + listings.size());
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (AuctionListing listing : listings.values()) {
            String path = "listings." + listing.getId();
            config.set(path + ".seller", listing.getSellerUuid().toString());
            config.set(path + ".sellerName", listing.getSellerName());
            config.set(path + ".item", listing.getItem());
            config.set(path + ".price", listing.getPrice());
            config.set(path + ".currency", listing.getCurrency().name());
            config.set(path + ".listedAt", listing.getListedAt());
            config.set(path + ".expiresAt", listing.getExpiresAt());
            if (listing.getTargetPlayerUuid() != null) {
                config.set(path + ".targetUuid", listing.getTargetPlayerUuid().toString());
                config.set(path + ".targetName", listing.getTargetPlayerName());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти auctions.yml: " + e.getMessage());
        }
    }

    private long defaultDurationMillis() {
        return plugin.getConfig().getLong("auction.listing-duration-hours", 72) * 3600_000L;
    }

    public Collection<AuctionListing> getListings() {
        return listings.values();
    }

    public Optional<AuctionListing> getListing(UUID id) {
        return Optional.ofNullable(listings.get(id));
    }

    /** Публічні (не персональні) лоти певної валюти. */
    public List<AuctionListing> getPublicListings(Currency currency) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.getCurrency() == currency && !l.isPersonal()) result.add(l);
        }
        return result;
    }

    /** Персональні пропозиції, виставлені САМЕ для цього гравця. */
    public List<AuctionListing> getPersonalOffersFor(UUID playerUuid, Currency currency) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.getCurrency() == currency && l.isPersonal() && playerUuid.equals(l.getTargetPlayerUuid())) {
                result.add(l);
            }
        }
        return result;
    }

    /** Усі активні лоти цього гравця (публічні + персональні, які він виставив), певної валюти. */
    public List<AuctionListing> getListingsBySeller(UUID sellerUuid, Currency currency) {
        List<AuctionListing> result = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.getCurrency() == currency && l.getSellerUuid().equals(sellerUuid)) result.add(l);
        }
        return result;
    }

    public int countBySeller(UUID sellerUuid) {
        int count = 0;
        for (AuctionListing listing : listings.values()) {
            if (listing.getSellerUuid().equals(sellerUuid)) count++;
        }
        return count;
    }

    /** Виставляє предмет з руки гравця. targetPlayerName == null -> публічний лот, інакше - персональна пропозиція. */
    public ListResult list(Player seller, ItemStack item, double price, Currency currency, String targetPlayerName) {
        int maxListings = plugin.getConfig().getInt("auction.max-listings-per-player", 10);
        if (countBySeller(seller.getUniqueId()) >= maxListings) {
            return ListResult.LIMIT_REACHED;
        }
        double maxPrice = plugin.getConfig().getDouble("auction.max-price", 1_000_000_000);
        if (price <= 0 || price > maxPrice) {
            return ListResult.INVALID_PRICE;
        }

        UUID targetUuid = null;
        String targetName = null;
        if (targetPlayerName != null) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetPlayerName);
            if (target == null || target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                return ListResult.TARGET_NOT_FOUND;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        }

        long now = System.currentTimeMillis();
        UUID id = UUID.randomUUID();
        AuctionListing listing = new AuctionListing(id, seller.getUniqueId(), seller.getName(),
                item.clone(), price, currency, now, now + defaultDurationMillis(), targetUuid, targetName);
        listings.put(id, listing);
        save();
        return ListResult.SUCCESS;
    }

    /** Купівля лота. Гроші і предмет переміщуються тут; повернення предмета в інвентар/дроп - відповідальність викликача. */
    public BuyResult buy(Player buyer, UUID id, Consumer<ItemStack> onItemReceived) {
        AuctionListing listing = listings.get(id);
        if (listing == null) return BuyResult.NOT_FOUND;
        if (listing.getSellerUuid().equals(buyer.getUniqueId())) return BuyResult.OWN_ITEM;
        if (listing.isPersonal() && !buyer.getUniqueId().equals(listing.getTargetPlayerUuid())) {
            return BuyResult.NOT_YOUR_OFFER;
        }

        PlayerData buyerData = plugin.getPlayerDataManager().load(buyer.getUniqueId());
        if (!listing.getCurrency().subtract(buyerData, listing.getPrice())) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }

        PlayerData sellerData = plugin.getPlayerDataManager().load(listing.getSellerUuid());
        listing.getCurrency().add(sellerData, listing.getPrice());

        listings.remove(id);
        save();
        onItemReceived.accept(listing.getItem());
        return BuyResult.SUCCESS;
    }

    /** Скасування власного лота. Повернення предмета в інвентар/дроп - відповідальність викликача. */
    public CancelResult cancel(Player requester, UUID id, Consumer<ItemStack> onItemReturned) {
        AuctionListing listing = listings.get(id);
        if (listing == null) return CancelResult.NOT_FOUND;
        if (!listing.getSellerUuid().equals(requester.getUniqueId()) && !requester.hasPermission("prison.admin")) {
            return CancelResult.NOT_OWNER;
        }

        listings.remove(id);
        save();
        onItemReturned.accept(listing.getItem());
        return CancelResult.SUCCESS;
    }

    /**
     * Знімає всі протухлі лоти: предмет повертається продавцю миттєво,
     * якщо той онлайн, інакше кладеться в поштову скриньку (MailboxManager).
     * Викликається періодичною задачею з PrisonPlugin.
     */
    public void purgeExpired() {
        List<AuctionListing> expired = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.isExpired()) expired.add(l);
        }
        if (expired.isEmpty()) return;

        for (AuctionListing listing : expired) {
            listings.remove(listing.getId());
            Player online = Bukkit.getPlayer(listing.getSellerUuid());
            if (online != null && online.isOnline()) {
                Map<Integer, ItemStack> leftover = online.getInventory().addItem(listing.getItem());
                for (ItemStack extra : leftover.values()) {
                    online.getWorld().dropItem(online.getLocation(), extra);
                }
            } else {
                plugin.getMailboxManager().addItem(listing.getSellerUuid(), listing.getItem());
            }
        }
        save();
    }
}
