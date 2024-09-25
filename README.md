# Minecraft Server Tools Plugin

## Overview
This plugin enhances Minecraft gameplay by providing various tools, including fly mode, economy management, teleport requests (TPA), and private messaging.

## Requirements
This plugin requires the following dependencies:

- **PlaceholderAPI**: For dynamic placeholder support. Make sure to download the **Server** and **Player** expansions from the eCloud.
- **Vault**: For economy management and permissions handling.
- **LuckPerms**: For advanced permissions management.

## Features
- **Fly Mode**: Toggle flight for players.
- **Economy Management**: Handle in-game currency and transactions.
- **Teleport Requests (TPA)**: Request and accept teleportation to/from players.
- **Private Messaging**: Send and reply to direct messages.
- **Game Mode Shortener**: Quickly switch between game modes.

## Commands
| Command                     | Description                                         | Permission                                |
|----------------------------|-----------------------------------------------------|-------------------------------------------|
| `/gmc`                     | Change game mode to Creative                        | `servertools.gamemode.creative`          |
| `/gms`                     | Change game mode to Survival                        | `servertools.gamemode.survival`          |
| `/gmsp`                    | Change game mode to Spectator                       | `servertools.gamemode.spectator`         |
| `/gma`                     | Change game mode to Adventure                       | `servertools.gamemode.adventure`         |
| `/tpa <player>`            | Send a teleport request to a player                 | `servertools.tpa.send`                   |
| `/tpahere <player>`        | Request a player to teleport to you                 | `servertools.tpa.sendhere`               |
| `/tpaccept`                | Accept a teleport request                           | `servertools.tpa.accept`                 |
| `/tpdeny`                  | Deny a teleport request                             | `servertools.tpa.deny`                   |
| `/msg <player> <message>`  | Send a private message to a player                  | `servertools.message.send`               |
| `/r <message>`             | Reply to the last received private message          | `servertools.message.reply`              |
| `/pay <player> <amount>`   | Pay another player                                  | `servertools.economy.pay`                |
| `/balance` or `/bal`      | Check your balance                                   | `servertools.economy.balance`            |
| `/withdraw <amount>`       | Withdraw money from your account                    | `servertools.economy.withdraw`           |
| `/baltop`                  | Shows top 10 player balances                        | `servertools.economy.baltop`             |
| `/addmoney <player> <amount>` | Add money to a player's balance                  | `servertools.economy.addmoney`           |
| `/removemoney <player> <amount>` | Remove money from a player's balance          | `servertools.economy.removemoney`        |
| `/fly` or `/togglefly`     | Toggle flight mode                                  | `servertools.fly`                        |
| `/home`                    | Teleport to a home                                  | `servertools.home.home`                  |
| `/sethome`                 | Set a home location                                 | `servertools.home.sethome`               |
| `/delhome`                 | Delete a home                                       | `servertools.home.delhome`               |
| `/homes` or `/listhomes`   | List your homes                                     | `servertools.home.homes`                 |

## Permissions
| Permission                           | Description                                          |
|--------------------------------------|------------------------------------------------------|
| `servertools.gamemode.*`            | Allows access to all gamemode commands                |
| `servertools.gamemode.creative`     | Allows changing to creative mode                      |
| `servertools.gamemode.survival`     | Allows changing to survival mode                      |
| `servertools.gamemode.spectator`    | Allows changing to spectator mode                     |
| `servertools.gamemode.adventure`     | Allows changing to adventure mode                     |
| `servertools.tpa.*`                  | Allows access to all TPA commands                    |
| `servertools.tpa.send`               | Allows sending teleport requests                      |
| `servertools.tpa.sendhere`           | Allows sending teleport-here requests                 |
| `servertools.tpa.accept`             | Allows accepting teleport requests                    |
| `servertools.tpa.deny`               | Allows denying teleport requests                      |
| `servertools.message.*`               | Allows access to all messaging commands               |
| `servertools.message.send`            | Allows sending private messages                       |
| `servertools.message.reply`           | Allows replying to private messages                   |
| `servertools.economy.*`               | Allows access to all economy commands                 |
| `servertools.economy.pay`            | Allows paying other players                           |
| `servertools.economy.balance`        | Allows checking balance                               |
| `servertools.economy.withdraw`       | Allows withdrawing money                              |
| `servertools.economy.baltop`         | Allows viewing top balances                           |
| `servertools.economy.addmoney`       | Allows adding money to a player's balance             |
| `servertools.economy.removemoney`    | Allows removing money from a player's balance         |
| `servertools.home.*`                 | Allows access to all home commands (sethome, home, delhome, homes                           |
| `servertools.home.sethome`           | Allows viewing top balances                           |
| `servertools.home.home`              | Allows adding money to a player's balance             |
| `servertools.home.delhome`           | Allows removing money from a player's balance         |
| `servertools.home.homes`             | Allows toggling flight mode                           |


## Installation
1. Download the latest version of the plugin from the releases page.
2. Place the plugin `.jar` file in your server's `plugins` folder.
3. Restart the server.
4. Configure the plugin in the `config.yml` file as needed.

## Configuration
The plugin can be configured in the `config.yml` file. Below are the defult configurable options:

```yaml
joinandleave:
  # Customize the join message
  joinMessage: |
    [<green>+</green>]%player_name% Joined.
    [<blue>Discord</blue>] Your discord
    or more if you want

  quitMessage: |
    [<red>-</red>]%player_name% Left.

# Game mode messages
gamemode:
  gmcMessage: "<green>You are now in Creative mode."
  gmsMessage: "<green>You are now in Survival mode."
  gmspMessage: "<green>You are now in Spectator mode."
  gmaMessage: "<green>You are now in Adventure mode."

tpa:
  # TPA accept/deny symbols and messages
  tpaAcceptSymbol: " ✔ "
  tpaAcceptSymbolColor: "GREEN"
  tpaAcceptHoverText: "<green>Click to accept"
  tpaDenySymbol: " ✘ "
  tpaDenySymbolColor: "RED"
  tpaDenyHoverText: "<red>Click to deny"

  # Customize the messages sent when a teleport request is accepted or denied
  tpaAccepted: "<green>Your teleport request was accepted. Teleporting to %target%."
  tpaRequestAccepted: "<green>You accepted %requester%'s teleport request."
  tpahereAccepted: "<green>Your teleport request was accepted. %target% is teleporting to you."
  tpahereRequestAccepted: "<green>You accepted %requester%'s request to teleport to them."
  tpRequestDenied: "<red>Your teleport request was denied."
  tpDenyConfirmation: "<red>You denied the teleport request."

directmessaging:
  msgUsage: "<red>Usage: /msg <player> <message>"
  replyUsage: "<red>Usage: /r <message>"
  noOneToReplyTo: "<red>You have no one to reply to."
  msgFormatSender: "[<red>me</red> <gray>-></gray> <red>%recipient%</red>] %message%"
  msgFormatRecipient: "[<red>%sender%</red> <gray>-></gray> <red>me</red>] %message%"

economy:
  playerOnly: "<red>This command can only be used by players!"
  noPermission: "<red>You don't have permission to use this command."
  playerNotFound: "<red>Player not found or not online."
  invalidAmount: "<red>Invalid amount. Please enter a positive number."
  insufficientFunds: "<red>Insufficient funds."
  balanceUsage: "<red>Usage: /balance [player]"
  balanceMessage: "<green>%player%'s balance: $%balance%"
  payUsage: "<red>Usage: /pay <player> <amount>"
  paymentSent: "<green>You sent $%amount% to %player%."
  paymentReceived: "<green>You received $%amount% from %player%."
  balTopHeader: "<gold>===== Top 10 Balances ====="
  balTopEntry: "<yellow>%position%</yellow>. %player%: <green>$%balance%"
  addMoneyUsage: "<red>Usage: /addmoney <player> <amount>"
  moneyAdded: "<green>Added $%amount% to %player%'s balance."
  moneyReceived: "<green>$%amount% has been added to your balance."
  removeMoneyUsage: "<red>Usage: /removemoney <player> <amount>"
  moneyRemoved: "<green>Removed $%amount% from %player%'s balance."
  moneyDeducted: "<green>$%amount% has been deducted from your balance."
  withdrawUsage: "<red>Usage: /withdraw <amount>"
  withdrawSuccess: "<green>You withdrew %amount%. New balance: %balance%"
  banknote:
    name: "<gold>%currency% Bank Note"
    description: "<white>Right-click to deposit %amount% into your account."
    lore:
      - "<yellow>Amount: %amount%"
      - "<gray>Right-click to deposit"
    currency: "Dollar"
    depositSuccess: "<green>Successfully deposited %amount% into your account."
    depositFailed: "<red>Failed to deposit the bank note. Please try again."

tab-menu:
  update-interval: 20 # Update interval in ticks (20 ticks = 1 second)
  header: "<gold>Welcome to <gradient:red:blue>Your Server</gradient>!"
  footer: "<gray>Players online: <green>%server_online%</green>"
  name-format: "<group_color><group_name> <white>| "
  group-colors:
    default: "<gray>"
    vip: "<green>"
    admin: "<red>"
    owner: "<dark_red>"

fly:
  playerOnly: "<red>This command can only be used by players!"
  noPermission: "<red>You don't have permission to use this command."
  enabled: "<green>Flight mode enabled."
  disabled: "<red>Flight mode disabled."

home:
  # Maximum number of homes a player can set
  maxHomes: 3

  # Per-permission home limits (overrides maxHomes if set)
  # Permission nodes correspond to the ones defined in your permissions plugin
  homeLimits:
    servertools.home.limit.1: 1
    servertools.home.limit.5: 5
    servertools.home.limit.unlimited: -1  # Use -1 for unlimited homes

  # Messages
  playerOnly: "<red>This command can only be used by players!"
  noPermission: "<red>You don't have permission to use this command."
  setHome: "<green>Home '%home_name%' set!"
  teleportHome: "<green>Teleported to home '%home_name%'."
  noHomeSet: "<red>You have not set a home named '%home_name%' yet."
  homeDeleted: "<green>Your home '%home_name%' has been deleted."
  noHomeToDelete: "<red>You don't have a home named '%home_name%' to delete."
  homeLimitReached: "<red>You have reached the maximum number of homes."
  listHomes: "<green>Your homes: %homes%"
  noHomesSet: "<red>You have not set any homes yet."
