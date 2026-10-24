<img src="src/main/resources/assets/qtiers/icon.png" width="96" align="right" alt="QTiers logo">

# QTiers

QTiers is a Fabric client mod for **Minecraft 1.21.11**. It shows a player's PvP tier-list ranks right next to their name, in nametags and in the tab list. It covers five tier lists at once:

- **PvPTiers** (pvptiers.com)
- **SubTiers** (subtiers.net)
- **MCPVP** (mcpvp.com)
- **PVPHQ** (pvphq.com)
- **TierMC** (tiermc.com)

It works like TierTagger, but for all five sites at once, and each site keeps its own icons.

![Profile screen for qdran](profile-screen.png)

## Features

### Tiers next to names
- Every site can sit on the **left** or **right** of the name, or be turned **off**.
- Each site shows the gamemode you pick, or the player's **highest** tier. If a player isn't ranked in your gamemode, QTiers can fall back to their highest tier, like TierTagger (*Never*, *If not ranked in mode*, *Always*).
- Every tier list uses **its own gamemode icons** from its own website, so you can tell at a glance which site a tier is from.
- Tiers are **saved between sessions** (in `.minecraft/qtiers/cache.json`), so they show the moment you join a server and refresh in the background.
- Tier colors come from **MCPVP**: High/Mid/Low tiers 1–6 (HT1 gold, HT2 silver, HT3 orange, HT4 green, HT5 blue, HT6 brown). Retired tiers show as `RHT1` in light blue.

### Your own name in F5
In third person you see your own nametag, with your tiers, like the WhoAmI mod. Turn it off with **Own name in F5** in the settings.

![Own nametag with tiers in F5](docs/f5-own-name.png)

### Player profiles
`/qtiers <player>` opens a profile screen. It shows a full-body skin render and every tier, peak, point total, rank and region from all five sites. It also has a button to open the player's NameMC page. Press **H** to open the profile of the player nearest to you.

### Player search
To look anyone up, type their name in the search box and press Enter. The box is at the bottom left of the profile screen and at the top of the settings screen.

### Settings
Open the settings with `/qtiers`, a keybind, or the Configure button in [Mod Menu](https://modrinth.com/mod/modmenu). The **Tiers** button opens a page where you choose each site's position and gamemode.

<p>
  <img src="docs/settings.png" width="49%" alt="QTiers settings">
  <img src="docs/tiers.png" width="49%" alt="Tiers page: position and gamemode per site">
</p>

## Commands
| Command | What it does |
|---|---|
| `/qtiers <player>` | Opens that player's profile. Online names tab-complete. |
| `/qtiers` or `/qtiers settings` | Opens the settings |
| `/qtiers reload` | Reloads the config and fetches tiers again |

## Keybinds
Set them under Options → Controls → QTiers.

| Action | Default |
|---|---|
| Open nearest player's profile | H |
| Cycle the PvPTiers / SubTiers / MCPVP / PVPHQ / TierMC gamemode | unbound |
| Toggle QTiers | unbound |
| Open settings | unbound |

## Install
1. Install [Fabric Loader](https://fabricmc.net/use/) 0.16 or newer for Minecraft 1.21.11.
2. Put `qtiers-1.4.0.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) in `.minecraft/mods`.
3. Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to open the settings from the mod list.

Download the jar from the [releases page](https://github.com/Adrian1337afk/qtiers/releases).

## Build
```
gradlew build
```
The jar ends up in `build/libs/`.

## Credits
- **Tier data:** the [PvPTiers](https://pvptiers.com), [SubTiers](https://subtiers.net), [PVPHQ](https://pvphq.com) and [TierMC](https://tiermc.com/api.html) APIs, and public [MCPVP](https://www.mcpvp.com) profile pages (MCPVP has no API).
- **Gamemode icons:** each tier list's own website. They belong to those sites.
- **Tier colors:** mcpvp.com.
- **Skin renders:** [Visage](https://visage.surgeplay.com), with [mc-heads](https://mc-heads.net) as the fallback.
- **Ideas:** the layout and profile screen come from [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers), and the own-name-in-F5 idea from [WhoAmI](https://modrinth.com/mod/whoami). QTiers uses no code or textures from either mod.

## License
QTiers is licensed under the [GNU GPL v3.0](LICENSE). The gamemode icons belong to their tier-list websites and are not covered by this license.
