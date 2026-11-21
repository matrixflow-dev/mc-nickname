# Nickname

A Paper plugin for Minecraft 26.3 that lets players choose the name shown in chat, in join and leave messages, and in the player list.

The name above a player's head stays the account name. Bedrock players who join through Geyser and Floodgate can use it too. Players need nothing on their side.

## Commands

| Command | Who | What it does |
|---|---|---|
| `/nick` | everyone | Shows your current nickname |
| `/nick <name>` | everyone | Sets your nickname |
| `/nick off` | everyone | Goes back to your account name |
| `/nick <player> <name\|off>` | ops (`nickname.others`) | Sets or removes another player's nickname. The player is named by account name and may be offline. Works from the console |

A nickname:

- has 1 to 16 characters (`max-length`). Japanese characters count as one each.
- may use letters of any script, digits, and `_ - ー ・`. Spaces, color codes and emoji are refused.
- may not be anyone else's account name or nickname, or a name in `reserved`. Case and the `.` Floodgate puts in front of Bedrock names do not matter.

Setting your own account name as your nickname removes the nickname.

## Settings

`plugins/Nickname/config.yml`:

| Key | Default | Meaning |
|---|---|---|
| `max-length` | 16 | Longest nickname, in characters |
| `reserved` | `[アイ]` | Names nobody may take, such as an NPC's name |

Nicknames are kept in `plugins/Nickname/nicknames.yml`, by player UUID.

## For other plugins

The nickname is the player's display name (`Player#displayName()`) and player list name. Plugins that use the display name see it with no extra code. [mc-ai-staff](https://github.com/matrixflow-dev/mc-ai-staff) 0.3.1 calls players by it and finds them by either name.

## Build

```bash
./gradlew build    # build/libs/nickname-<version>.jar
```

It needs JDK 25.

## License

MIT
