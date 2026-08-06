package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.model.AuctionListing;
import com.prison.core.model.Currency;
import com.prison.core.util.CoinFormat;
import com.prison.core.util.ItemBuilder;
import com.prison.core.util.RubleFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Повноцінне меню аукціону: 45 слотів під лоти + нижній ряд навігації
 * (назад, 3 вкладки, сортування, статистика, оновлення, попередня/наступна
 * сторінка). Один екземпляр GUI обслуговує лише одну валюту (COINS або
 * RUBLES) - викликається окремо з /auc та /rauc.
 */
public class AuctionGUI {

    private static final int ITEMS_PER_PAGE = 45;

    private AuctionGUI() {
    }

    public static void open(PrisonPlugin plugin, Player player, Currency currency,
                             AuctionHolder.Tab tab, AuctionHolder.Sort sort, int page) {
        AuctionHolder holder = new AuctionHolder(currency);
        holder.setTab(tab);
        holder.setSort(sort);

        String currencyName = plugin.getMessages().get(player, currency == Currency.COINS ? "currency-coins" : "currency-rubles");
        String title = plugin.getMessages().get(player, "gui-auction-title") + " (" + currencyName + ") " + (page + 1);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        List<AuctionListing> listings = switch (tab) {
            case ALL -> plugin.getAuctionManager().getPublicListings(currency);
            case PERSONAL -> plugin.getAuctionManager().getPersonalOffersFor(player.getUniqueId(), currency);
            case MINE -> plugin.getAuctionManager().getListingsBySeller(player.getUniqueId(), currency);
        };
        sortListings(listings, sort);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, listings.size());
        for (int i = start; i < end; i++) {
            AuctionListing listing = listings.get(i);
            int slot = i - start;
            inv.setItem(slot, buildIcon(plugin, player, listing, tab));
            holder.put(slot, listing.getId());
        }

        boolean hasNext = end < listings.size();
        holder.setHasNextPage(hasNext);
        holder.setPage(page);

        renderNavBar(plugin, player, inv, holder);

        player.openInventory(inv);
    }

    private static void sortListings(List<AuctionListing> listings, AuctionHolder.Sort sort) {
        Comparator<AuctionListing> comparator = switch (sort) {
            case TIME_DESC -> Comparator.comparingLong(AuctionListing::getListedAt).reversed();
            case TIME_ASC -> Comparator.comparingLong(AuctionListing::getListedAt);
            case PRICE_ASC -> Comparator.comparingDouble(AuctionListing::getPrice);
            case PRICE_DESC -> Comparator.comparingDouble(AuctionListing::getPrice).reversed();
        };
        listings.sort(comparator);
    }

    private static ItemStack buildIcon(PrisonPlugin plugin, Player player, AuctionListing listing, AuctionHolder.Tab tab) {
        ItemStack icon = listing.getItem().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            String currencyName = plugin.getMessages().get(player,
                    listing.getCurrency() == Currency.COINS ? "currency-coins" : "currency-rubles");

            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(plugin.getMessages().get(player, "gui-auction-seller", "seller", listing.getSellerName()));
            lore.add(plugin.getMessages().get(player, "gui-auction-price",
                    "price", formatPrice(listing.getPrice(), listing.getCurrency()), "currency", currencyName));
            lore.add(plugin.getMessages().get(player, "gui-auction-time-left",
                    "time", com.prison.core.util.TimeFormatter.format(plugin, player, listing.getRemainingMillis())));

            if (listing.isPersonal()) {
                lore.add(plugin.getMessages().get(player, "gui-auction-personal-for", "player", listing.getTargetPlayerName()));
            }

            boolean own = listing.getSellerUuid().equals(player.getUniqueId());
            if (own) {
                lore.add(plugin.getMessages().get(player, "gui-auction-click-cancel"));
            } else if (!listing.isPersonal() || player.getUniqueId().equals(listing.getTargetPlayerUuid())) {
                lore.add(plugin.getMessages().get(player, "gui-auction-click-buy"));
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static void renderNavBar(PrisonPlugin plugin, Player player, Inventory inv, AuctionHolder holder) {
        Currency currency = holder.getCurrency();

        inv.setItem(AuctionHolder.SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .name(plugin.getMessages().get(player, "gui-auction-back"))
                .build());

        inv.setItem(AuctionHolder.SLOT_TAB_ALL, new ItemBuilder(Material.CHEST)
                .name(plugin.getMessages().get(player, "gui-auction-tab-all"))
                .glow(holder.getTab() == AuctionHolder.Tab.ALL)
                .build());

        inv.setItem(AuctionHolder.SLOT_TAB_PERSONAL, new ItemBuilder(Material.PLAYER_HEAD)
                .name(plugin.getMessages().get(player, "gui-auction-tab-personal"))
                .glow(holder.getTab() == AuctionHolder.Tab.PERSONAL)
                .build());

        inv.setItem(AuctionHolder.SLOT_TAB_MINE, new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getMessages().get(player, "gui-auction-tab-mine"))
                .glow(holder.getTab() == AuctionHolder.Tab.MINE)
                .build());

        inv.setItem(AuctionHolder.SLOT_SORT, new ItemBuilder(Material.HOPPER)
                .name(plugin.getMessages().get(player, "gui-auction-sort"))
                .lore(plugin.getMessages().get(player, sortLoreKey(holder.getSort())))
                .build());

        inv.setItem(AuctionHolder.SLOT_STATS, new ItemBuilder(Material.BOOK)
                .name(plugin.getMessages().get(player, "gui-auction-stats"))
                .lore(plugin.getMessages().get(player, "gui-auction-stats-click"))
                .build());

        inv.setItem(AuctionHolder.SLOT_REFRESH, new ItemBuilder(Material.CLOCK)
                .name(plugin.getMessages().get(player, "gui-auction-refresh"))
                .build());

        if (holder.getPage() > 0) {
            inv.setItem(AuctionHolder.SLOT_PREV, new ItemBuilder(Material.ARROW)
                    .name(plugin.getMessages().get(player, "gui-auction-prev-page"))
                    .build());
        }
        if (holder.hasNextPage()) {
            inv.setItem(AuctionHolder.SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .name(plugin.getMessages().get(player, "gui-auction-next-page"))
                    .build());
        }
    }

    private static String sortLoreKey(AuctionHolder.Sort sort) {
        return switch (sort) {
            case TIME_DESC -> "gui-auction-sort-time-desc";
            case TIME_ASC -> "gui-auction-sort-time-asc";
            case PRICE_ASC -> "gui-auction-sort-price-asc";
            case PRICE_DESC -> "gui-auction-sort-price-desc";
        };
    }

    /** Рублі завжди цілим числом (RubleFormat), монети - дробові до 1000, цілі від 1000 (CoinFormat). */
    private static String formatPrice(double price, Currency currency) {
        return currency == Currency.RUBLES ? RubleFormat.format(price) : CoinFormat.format(price);
    }
}
