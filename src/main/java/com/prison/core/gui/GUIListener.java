package com.prison.core.gui;

import com.prison.core.PrisonPlugin;
import com.prison.core.managers.AuctionManager;
import com.prison.core.managers.BoosterManager;
import com.prison.core.managers.LevelManager;
import com.prison.core.managers.UpgradeManager;
import com.prison.core.model.AuctionListing;
import com.prison.core.model.Mine;
import com.prison.core.model.PlayerData;
import com.prison.core.util.CoinFormat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class GUIListener implements Listener {

    private final PrisonPlugin plugin;

    public GUIListener(PrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (holder == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (holder instanceof LevelHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            if (event.getSlot() == LevelHolder.LEVELUP_SLOT) {
                attemptLevelUp(player);
                LevelGUI.open(plugin, player);
            }
        } else if (holder instanceof MinesHolder minesHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            String mineId = minesHolder.get(event.getSlot());
            if (mineId == null) return;
            teleportToMine(player, mineId);
        } else if (holder instanceof UpgradeHolder upgradeHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            if (event.getSlot() == upgradeHolder.getActionSlot()) {
                attemptUpgrade(player);
                UpgradeGUI.open(plugin, player);
            }
        } else if (holder instanceof AuctionHolder auctionHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            handleAuctionClick(player, auctionHolder, event.getSlot());
        } else if (holder instanceof BoosterHolder boosterHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInventory) return;
            handleBoosterClick(player, boosterHolder, event.getSlot());
        }
    }

    /** Спроба підвищити рівень (система 1-40: блоки + монети). */
    private void attemptLevelUp(Player player) {
        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());
        LevelManager levelManager = plugin.getLevelManager();

        LevelManager.LevelUpResult result = levelManager.attemptLevelUp(data);
        switch (result) {
            case SUCCESS -> {
                plugin.getMessages().send(player, "level-up-success", "level", String.valueOf(data.getLevel()));
                levelManager.updateXpBar(player, data);
                plugin.getPerksManager().applyPerks(player, data);
            }
            case MAX_LEVEL -> plugin.getMessages().send(player, "level-up-max");
            case NOT_ENOUGH_BLOCKS -> {
                long required = levelManager.blocksRequiredForLevel(data.getLevel());
                plugin.getMessages().send(player, "level-up-not-enough-blocks",
                        "mined", String.valueOf(data.getBlocksMinedForLevel()),
                        "required", String.valueOf(required));
            }
            case NOT_ENOUGH_MONEY -> plugin.getMessages().send(player, "level-up-not-enough-money",
                    "cost", CoinFormat.format(levelManager.costForLevel(data.getLevel())));
        }
    }

    private void teleportToMine(Player player, String mineId) {
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(mineId);
        if (mineOpt.isEmpty() || mineOpt.get().getTeleport() == null) {
            plugin.getMessages().send(player, "mine-not-found");
            return;
        }

        player.closeInventory();
        player.teleport(mineOpt.get().getTeleport());
        plugin.getMessages().send(player, "mine-teleported", "mine", mineId);
    }

    /** Спроба апгрейднути тір предмета в руці (матеріал + зачарування, як на VimeWorld). */
    private void attemptUpgrade(Player player) {
        UpgradeManager upgradeManager = plugin.getUpgradeManager();
        ItemStack item = player.getInventory().getItemInMainHand();
        UpgradeManager.Category category = upgradeManager.categoryOf(item);
        if (category == null) {
            plugin.getMessages().send(player, "gui-upgrade-need-item");
            return;
        }

        Optional<UpgradeManager.Tier> nextOpt = upgradeManager.getNextTier(item, category);
        if (nextOpt.isEmpty()) {
            plugin.getMessages().send(player, "gui-upgrade-max");
            return;
        }
        UpgradeManager.Tier tier = nextOpt.get();

        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());
        if (data.getBalance() < tier.costCoins) {
            plugin.getMessages().send(player, "gui-upgrade-not-enough-money", "cost", CoinFormat.format(tier.costCoins));
            return;
        }
        if (tier.costMaterial != null && !player.getInventory().containsAtLeast(new ItemStack(tier.costMaterial), tier.costAmount)) {
            plugin.getMessages().send(player, "gui-upgrade-not-enough-material",
                    "amount", String.valueOf(tier.costAmount), "material", tier.costMaterial.name());
            return;
        }

        if (tier.costMaterial != null) {
            player.getInventory().removeItem(new ItemStack(tier.costMaterial, tier.costAmount));
        }
        data.subtractBalance(tier.costCoins);

        ItemStack newItem = upgradeManager.buildItemForTier(item.getType(), category, tier);
        player.getInventory().setItemInMainHand(newItem);

        plugin.getMessages().send(player, "gui-upgrade-bought");
    }

    private void handleAuctionClick(Player player, AuctionHolder holder, int slot) {
        var currency = holder.getCurrency();

        if (slot == AuctionHolder.SLOT_BACK) {
            AuctionGUI.open(plugin, player, currency, AuctionHolder.Tab.ALL, AuctionHolder.Sort.TIME_DESC, 0);
            return;
        }
        if (slot == AuctionHolder.SLOT_TAB_ALL) {
            AuctionGUI.open(plugin, player, currency, AuctionHolder.Tab.ALL, holder.getSort(), 0);
            return;
        }
        if (slot == AuctionHolder.SLOT_TAB_PERSONAL) {
            AuctionGUI.open(plugin, player, currency, AuctionHolder.Tab.PERSONAL, holder.getSort(), 0);
            return;
        }
        if (slot == AuctionHolder.SLOT_TAB_MINE) {
            AuctionGUI.open(plugin, player, currency, AuctionHolder.Tab.MINE, holder.getSort(), 0);
            return;
        }
        if (slot == AuctionHolder.SLOT_SORT) {
            AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.nextSort(), 0);
            return;
        }
        if (slot == AuctionHolder.SLOT_STATS) {
            sendAuctionStats(player, currency);
            return;
        }
        if (slot == AuctionHolder.SLOT_REFRESH) {
            AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.getSort(), holder.getPage());
            return;
        }
        if (slot == AuctionHolder.SLOT_PREV) {
            if (holder.getPage() > 0) {
                AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.getSort(), holder.getPage() - 1);
            }
            return;
        }
        if (slot == AuctionHolder.SLOT_NEXT) {
            if (holder.hasNextPage()) {
                AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.getSort(), holder.getPage() + 1);
            }
            return;
        }

        java.util.UUID listingId = holder.get(slot);
        if (listingId == null) return;

        AuctionManager auctionManager = plugin.getAuctionManager();
        Optional<AuctionListing> listingOpt = auctionManager.getListing(listingId);
        if (listingOpt.isEmpty()) {
            AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.getSort(), holder.getPage());
            return;
        }
        AuctionListing listing = listingOpt.get();

        if (listing.getSellerUuid().equals(player.getUniqueId())) {
            AuctionManager.CancelResult result = auctionManager.cancel(player, listingId, item -> giveOrDrop(player, item));
            if (result == AuctionManager.CancelResult.SUCCESS) {
                plugin.getMessages().send(player, "auction-cancel-success");
            }
        } else {
            AuctionManager.BuyResult result = auctionManager.buy(player, listingId, item -> giveOrDrop(player, item));
            switch (result) {
                case SUCCESS -> plugin.getMessages().send(player, "auction-buy-success");
                case NOT_ENOUGH_MONEY -> plugin.getMessages().send(player, "auction-not-enough-money");
                case NOT_YOUR_OFFER -> plugin.getMessages().send(player, "auction-not-your-offer");
                case NOT_FOUND, OWN_ITEM -> {}
            }
        }

        AuctionGUI.open(plugin, player, currency, holder.getTab(), holder.getSort(), holder.getPage());
    }

    private void sendAuctionStats(Player player, com.prison.core.model.Currency currency) {
        AuctionManager auctionManager = plugin.getAuctionManager();
        var publicListings = auctionManager.getPublicListings(currency);
        var myListings = auctionManager.getListingsBySeller(player.getUniqueId(), currency);
        var personalOffers = auctionManager.getPersonalOffersFor(player.getUniqueId(), currency);

        double myTotalValue = 0;
        for (AuctionListing l : myListings) myTotalValue += l.getPrice();

        String currencyName = plugin.getMessages().get(player, currency == com.prison.core.model.Currency.COINS ? "currency-coins" : "currency-rubles");
        String formattedValue = currency == com.prison.core.model.Currency.RUBLES
                ? com.prison.core.util.RubleFormat.format(myTotalValue) : CoinFormat.format(myTotalValue);

        plugin.getMessages().send(player, "auction-stats-header", "currency", currencyName);
        plugin.getMessages().send(player, "auction-stats-total", "count", String.valueOf(publicListings.size()));
        plugin.getMessages().send(player, "auction-stats-mine", "count", String.valueOf(myListings.size()), "value", formattedValue);
        plugin.getMessages().send(player, "auction-stats-personal", "count", String.valueOf(personalOffers.size()));
    }

    private void giveOrDrop(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
    }

    private void handleBoosterClick(Player player, BoosterHolder holder, int slot) {
        BoosterManager.Type type = holder.getType(slot);
        BoosterManager.Duration duration = holder.getDuration(slot);
        Integer multiplier = holder.getMultiplier(slot);
        if (type == null || duration == null || multiplier == null) return; // клік по статус-іконці чи заповнювачу

        PlayerData data = plugin.getPlayerDataManager().load(player.getUniqueId());
        BoosterManager.PurchaseResult result = plugin.getBoosterManager().purchase(data, type, multiplier, duration);

        switch (result) {
            case SUCCESS -> plugin.getMessages().send(player, "booster-purchase-success");
            case NOT_ENOUGH_RUBLES -> plugin.getMessages().send(player, "booster-not-enough-rubles",
                    "cost", com.prison.core.util.RubleFormat.format(plugin.getBoosterManager().getPrice(type, multiplier, duration)));
        }

        BoosterGUI.open(plugin, player);
    }
}
