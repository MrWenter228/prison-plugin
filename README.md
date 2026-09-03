# Prison

A custom Minecraft Prison plugin for **Spigot/Paper**, written in **Java 17**, inspired by the VimeWorld Prison gameplay style.

The plugin includes mines with automatic regeneration, a level system from 1 to 40, tier-based equipment upgrades, a player auction system, two currencies, localization, and gradient/HEX color support.

## 🚀 Easiest Way to Get the JAR: GitHub Actions

The repository contains `.github/workflows/build.yml`.

Simply upload the code to GitHub using the web interface, GitHub Desktop, or `git push`. GitHub Actions will automatically build `Prison.jar` and upload it to the **Artifacts** section of the workflow run.

If you push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

the workflow will additionally create an official **GitHub Release** with the JAR file attached.

## Local Build

```bash
mvn clean package
```

The compiled `Prison.jar` will be available in the `target/` directory.

The plugin is compiled against **Paper API 1.20.4-R0.1-SNAPSHOT**, while `plugin.yml` specifies:

```yaml
api-version: '1.16'
```

This is intended to provide broad compatibility with **1.16.5 and newer versions**.

The code intentionally relies only on stable legacy Bukkit API features and does not use Adventure Components or APIs introduced after approximately 1.20. Therefore, it may theoretically work on newer Minecraft versions as well, although those versions have not been physically tested.

## Installation

1. Put `Prison.jar` into your server's `plugins/` directory.
2. Restart the server. The plugin will generate:

   * `config.yml`
   * `mines.yml`
   * `prices.yml`
   * `lang/*.yml`
3. Run `/minewand` (requires `prison.admin`, OP by default) and select the region for the future mine:

   * Left click = position 1
   * Right click = position 2
4. Run `/mineset create mine_a` to create a mine. The mine will immediately be available to all players.
5. Configure the mine blocks:

```text
/mineset setblock mine_a STONE 70
/mineset setblock mine_a COAL_ORE 30
```

The total percentage must equal 100.

6. Stand at the desired spawn location and run:

```text
/mineset settp mine_a
```

7. Done! Players can now use:

```text
/mines mine_a
/lvl
/upgrade
/autosell on
```

## Main Commands

| Command                                                               | Description                                                                        |
| --------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `/lvl` (`/level`, `/levels`)                                          | Opens the level menu. Clicking the experience bottle increases the player's level. |
| `/mines [name]` / `/minesgui`                                         | Displays the available mines in chat or through a GUI.                             |
| `/upgrade` (`/up`, `/gear`, `/prokachka`)                             | Tier-based equipment upgrade system inspired by VimeWorld.                         |
| `/pay <player> <amount>`                                              | Transfers coins between players. Rubles cannot be transferred directly.            |
| `/autosell <on\|off\|all>`                                            | Enables/disables automatic selling or instantly sells the entire inventory.        |
| `/auc` / `/rauc`                                                      | Two separate player auctions: coins and rubles.                                    |
| `/lvl set <player> <level>`                                           | Admin command for directly setting a player's level from 1 to 40.                  |
| `/booster`                                                            | Opens the booster shop.                                                            |
| `/booster give <player> <coins\|blocks> <2\|3\|5> <day\|week\|month>` | Admin command for giving boosters.                                                 |
| `/money give <player> <amount>`                                       | Gives coins to a player.                                                           |
| `/rubles give <player> <amount>`                                      | Gives rubles to a player.                                                          |
| `/minewand`, `/mineset ...`                                           | Admin tools for creating and configuring mines.                                    |

> `/balance`, `/bal`, `/baltop`, and `/prisoninfo` have been removed. Player balance and progress can be displayed using `%prison_balance%` / `%prison_level%` through PlaceholderAPI, scoreboard, tab, or GUI item lore.

## Level System — 1 to 40

The `/lvl` command opens a simple one-row GUI with 9 slots. The only active item is an **experience bottle** in the center.

Its lore displays:

* Current level;
* Blocks mined;
* Blocks required for the next level;
* Level-up cost.

To reach the next level, the player must meet **both requirements**:

1. Mine a specific number of blocks since the previous level.
2. Pay the required amount of coins.

The requirements are not calculated using a formula. Each level from 2 to 40 has its own configurable requirements in:

```yaml
levels:
  requirements:
```

The progression is intentionally non-linear and based on predefined values.

Each level also provides a permanent percentage bonus to automatic selling:

```yaml
levels:
  reward-multiplier-per-level:
```

The player's plugin level is displayed on the vanilla Minecraft XP bar:

* XP number = current plugin level;
* XP bar progress = block progress toward the next level.

The XP bar is updated when the player joins, after mining blocks, and immediately after leveling up.

Admins can bypass the normal requirements with:

```text
/lvl set <player> <level>
```

This resets the player's block progress to 0.

## Tier-Based Equipment Upgrades

The `/upgrade` system is inspired by VimeWorld.

Instead of simply increasing enchantment levels, the **physical material of the item changes** when it is upgraded.

Example:

```text
Wood (Efficiency 0)
→ Wood Efficiency I
→ Stone Efficiency I
→ Iron Efficiency II
→ Diamond Efficiency III
```

The same system applies to:

* **Swords** — Sharpness instead of Efficiency;
* **Armor** — Protection instead of Efficiency;
* **Axes**;
* **Shovels**.

Shovels have their own 12-tier progression using materials related to digging:

```text
Wood
→ Wood Efficiency I
→ Wood Efficiency II
→ Wood Efficiency III
→ Stone Efficiency I
→ ...
→ Stone Efficiency IV
→ Iron Efficiency I
→ Iron Efficiency II
→ Iron Efficiency III
→ Iron Efficiency III + Fortune I
```

Each upgrade requires:

* Coins;
* A specific resource.

All upgrade requirements are configurable in:

```yaml
upgrades:
  tool:
    tiers:
  shovel:
    tiers:
  sword:
    tiers:
  armor:
    tiers:
```

A tier can contain multiple enchantments simultaneously.

For example:

```yaml
enchants:
  - enchant: EFFICIENCY
    level: 3
  - enchant: FORTUNE
    level: 1
```

### Unbreakable Equipment

Every item upgraded at least once automatically receives:

```java
ItemMeta#setUnbreakable(true)
```

This applies to:

* Pickaxes;
* Axes;
* Shovels;
* Swords;
* Armor.

The item will no longer lose durability.

The current tier is not stored using separate PDC tags. Instead, the plugin determines the tier from the item's **material and complete enchantment set**, which corresponds to one of the configured tiers.

The `/upgrade` GUI displays the entire tier progression:

* Green = completed tiers;
* Highlighted = current tier;
* Clickable = next available upgrade;
* Barrier = locked future tier.

## Equipment Protection on Death

Important equipment is protected when a player dies.

The following items do not drop:

* Pickaxes;
* Axes;
* Shovels;
* Swords;
* Armor;
* Shears;
* Fishing rods.

They are removed from the death drops and automatically returned to the player after respawn.

Armor is automatically equipped again if the corresponding slots are available.

Other items, such as blocks, food, and regular inventory items, behave normally and can drop on death.

This system is implemented in:

```text
DeathProtectionListener
```

## 💰 Two Currencies

The plugin has two separate currencies.

### Coins

**Coins** are the main in-game currency.

They can be earned through mining and automatic block selling and are used for:

* Level upgrades;
* Equipment upgrades;
* Player-to-player payments.

Players can transfer coins using:

```text
/pay <player> <amount>
```

### Rubles

**Rubles** are the donation currency.

They cannot be transferred directly between players using `/pay`.

Rubles can be obtained through:

* The ruble auction;
* Admin commands.

Admins can use:

```text
/money give <player> <amount>
/rubles give <player> <amount>
```

Balances can be displayed using PlaceholderAPI:

```text
%prison_balance%
%prison_rubles%
```

## 🏪 Auction System

The plugin provides two completely separate auction markets:

```text
/auc
```

for coins, and:

```text
/rauc
```

for rubles.

Both auctions use the same internal `AuctionCommand` implementation, while the currency is fixed when the command is registered.

### Listing an Item

Public listing:

```text
/auc sell <price>
```

Personal offer:

```text
/auc sell <price> <player>
```

Personal offers can only be seen and purchased by the specified player.

### Auction GUI

The auction uses a 54-slot GUI.

|  Slot | Button          | Action                                         |
| ----: | --------------- | ---------------------------------------------- |
|    45 | Back            | Resets filters and returns to the first page   |
|    46 | All Items       | Displays all public listings                   |
|    47 | Personal Offers | Displays offers specifically made for you      |
|    48 | My Listings     | Displays your public and personal listings     |
|    49 | Sorting         | Newest → Oldest → Lowest Price → Highest Price |
|    50 | Statistics      | Displays auction statistics                    |
|    51 | Refresh         | Refreshes the current auction view             |
| 52/53 | ◀ / ▶           | Previous / next page                           |

Each listing displays:

* Seller;
* Price;
* Currency;
* Remaining listing time.

The default listing duration is **72 hours** and can be configured using:

```yaml
auction:
  listing-duration-hours:
```

Clicking another player's listing purchases the item.

Clicking your own listing removes it from the auction and returns the item to your inventory.

### Expired Listings

A background task periodically checks for expired listings.

The interval is configured with:

```yaml
auction:
  expiry-check-interval-seconds:
```

If the seller is online, the item is immediately returned.

If the seller is offline, the item is placed into their personal mailbox:

```text
MailboxManager
mailbox.yml
```

The item is automatically delivered when the player joins the server.

Auction listings are stored in:

```text
auctions.yml
```

and survive server restarts.

The maximum number of active listings and maximum price are configurable in:

```yaml
auction:
```

## ⚡ Coin & Block Boosters

The `/booster` command opens the booster shop.

There are two types of boosters:

### Coin Booster

Multiplies the amount of coins received from selling blocks.

### Block Booster

Multiplies block progress toward the next level.

Both booster types have three power levels:

```text
x2
x3
x5
```

Each can be purchased for:

* 1 day;
* 1 week;
* 1 month.

Booster prices are configurable in:

```yaml
boosters:
```

Purchasing an already active booster extends its remaining duration and immediately switches its multiplier to the newly purchased level.

Admins can give boosters for free:

```text
/booster give <player> <coins|blocks> <2|3|5> <day|week|month>
```

## ❤️ Passive Level Bonuses

Player levels can provide real gameplay bonuses using Bukkit `AttributeModifier`.

Bonuses are recalculated when the player joins and after leveling up.

### Additional Health

Configured using:

```yaml
perks:
  hearts:
    levels:
    max-bars:
```

Each configured level can provide an additional heart.

### Additional Damage

Configured using:

```yaml
perks:
  damage:
    levels:
    max-bonus:
```

The default configuration starts providing additional damage from level 12.

These modifiers affect actual gameplay and are not purely cosmetic.

## 🌎 World Restrictions

To prevent conflicts with the custom Prison economy and level system:

### Crafting Disabled

All crafting recipes are disabled, including crafting through workbenches and custom inventories.

### Mob XP Disabled

Players do not receive vanilla experience from killing mobs.

The vanilla XP bar is controlled by `LevelManager` and displays the player's Prison level instead of normal Minecraft XP.

Implemented in:

```text
RestrictionListener
```

using:

* `CraftItemEvent`;
* `EntityExpEvent`.

## 🌈 Localization & Gradients

The plugin supports:

```text
ru
en
```

The default language is configured in:

```yaml
language:
  default:
```

Language files are located in:

```text
lang/*.yml
```

HEX colors and gradients can be used directly inside messages.

Example:

```yaml
level-up-success: "&#FF00FF&#00FFFFYou reached level {level}!"
some-message: "&#FF00AACustom HEX color"
```

Two HEX colors placed next to each other create a smooth character-by-character gradient.

Example:

```text
&#FF00FF&#00FFFFText
```

Gradient processing is handled by:

```text
GradientUtil
```

using:

```java
net.md_5.bungee.api.ChatColor.of(Color)
```

### Custom Time Formatting

Time units displayed in auction and booster lore are also configurable.

For example:

```text
2d 5h
3h 12m
```

The following language keys can be customized:

```yaml
time-days:
time-hours:
time-minutes:
```

This allows server owners to change the displayed abbreviations without modifying the Java source code.

Implemented in:

```text
util/TimeFormatter
```

## 🏗️ Architecture

```text
com.prison.core
 ├─ PrisonPlugin
 │   └─ Main plugin entry point, manager initialization and tasks
 │
 ├─ managers/
 │   ├─ MineManager
 │   │   └─ mines.yml, regions, asynchronous batched regeneration
 │   ├─ EconomyManager
 │   │   └─ prices.yml, selling, level multipliers
 │   ├─ PlayerDataManager
 │   │   └─ playerdata/<uuid>.yml, caching, player locale
 │   ├─ LanguageManager
 │   │   └─ lang/*.yml, HEX colors, gradients, per-player locale
 │   ├─ LevelManager
 │   │   └─ Level system 1-40 and XP bar
 │   ├─ UpgradeManager
 │   │   └─ Tier-based equipment upgrades
 │   ├─ AuctionManager
 │   │   └─ auctions.yml, listings, purchases, personal offers
 │   ├─ MailboxManager
 │   │   └─ Offline expired auction item delivery
 │   ├─ BoosterManager
 │   │   └─ Coin/block boosters x2/x3/x5
 │   ├─ PerksManager
 │   │   └─ Health and damage AttributeModifiers
 │   └─ SelectionManager
 │       └─ Temporary position selection for the mine wand
 │
 ├─ model/
 │   └─ Mine, PlayerData, Currency, AuctionListing
 │
 ├─ commands/
 │   └─ Individual command classes
 │
 ├─ gui/
 │   └─ LevelGUI, MinesGUI, UpgradeGUI, AuctionGUI,
 │       BoosterGUI, GUI holders and GUI listeners
 │
 ├─ listeners/
 │   └─ BlockBreak, Join/Quit, wand interaction,
 │       DeathProtectionListener, RestrictionListener
 │
 └─ util/
     └─ ItemBuilder, GradientUtil, TimeFormatter
```

## ⚙️ Key Mechanics

### Public Mines

There is no rank-based mine access system.

Every mine created by an administrator is immediately available to all players through:

```text
/mines
/minesgui
```

### Asynchronous Batched Mine Regeneration

The mine regeneration system uses background checks and batched block regeneration.

When the amount of remaining blocks falls below:

```yaml
mines:
  auto-reset-threshold:
```

`MineManager` starts a regeneration task.

Blocks are restored in configurable batches:

```yaml
mines:
  regen-blocks-per-tick:
```

This helps prevent large lag spikes when regenerating big mines.

### Auto-Sell & Level Progress

When a player mines a block:

1. The block is converted into coins.
2. The player's level multiplier is applied.
3. Active coin boosters are applied.
4. Block progress toward the next level is increased.
5. Active block boosters are applied.

This allows mining, economy, boosters, and level progression to work together.

### Level-Based Perks

`PerksManager` recalculates health and damage modifiers when:

* A player joins;
* A player levels up.

### Data Persistence

Player data is stored using individual YAML files:

```text
playerdata/<uuid>.yml
```

The plugin does not require Vault or an SQL database.

## PlaceholderAPI

If PlaceholderAPI is installed and enabled, the plugin automatically registers the following placeholders:

| Placeholder               | Description                                |
| ------------------------- | ------------------------------------------ |
| `%prison_balance%`        | Formatted coin balance                     |
| `%prison_rubles%`         | Formatted ruble balance                    |
| `%prison_level%`          | Current player level (1-40)                |
| `%prison_level_max%`      | Maximum level (40)                         |
| `%prison_level_progress%` | Blocks mined / required for the next level |

The integration is implemented in:

```text
com.prison.core.placeholder.PrisonPlaceholders
```

The class is only loaded when PlaceholderAPI is detected.

Without PlaceholderAPI, the plugin simply skips the integration.

## 🔮 Possible Future Improvements

The following features are currently not implemented but could be added in future versions:

* Particle and sound effects when leveling up or upgrading equipment;
* MySQL backend instead of YAML player data for multi-server networks;
* Level-based access restrictions for mines;
* Additional Prison gameplay mechanics and progression systems.

## 📌 Project Status

This project is primarily intended as a **custom Minecraft Prison plugin and a demonstration of Java development, Bukkit/Paper API usage, GUI development, configuration management, event handling, and game-system architecture**.
