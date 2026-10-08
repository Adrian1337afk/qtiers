<img src="src/main/resources/assets/qtiers/icon.png" width="96" align="right" alt="QTiers logo">

# QTiers

A Fabric client mod for **Minecraft 1.21.11**. It shows **PvPTiers**, **SubTiers**, **MCPVP** and **PVPHQ** rankings together next to player names. It works like TierTagger, but for all four sites at once.

```
⚔HT3 | Steve | ⛏HT4 ⚔MT3 🪓LT2
PvPTiers        SubTiers MCPVP PVPHQ
```

![Profile screen](profile-screen.png)

## Features
- **All 4 sites at once** in nametags and the tab list. Put each site on the **left** or **right** of the name, or turn it **off**.
- **Player profile screen** (`/qtiers <player>`): a full-body skin render, plus every tier, peak, points, rank and region from every site. It has a button to open the player's NameMC page.
- **A separate gamemode for each site**, or **Highest**. Fallback rules match TierTagger: *Never*, *If not ranked in mode*, or *Always*.
- **Each tier list has its own icons**, taken from its own website. Tiers use **MCPVP's colors** for High/Mid/Low tiers 1–6 (HT1 gold, HT2 silver, HT3 orange, HT4 green, HT5 blue, HT6 brown). Retired tiers show as `RHT1` in light blue.
- **Open the nearest player's profile** with **H**.
- **In-game settings**: `/qtiers`.

## Commands
- `/qtiers <player>`: open that player's profile screen. Online names tab-complete.
- `/qtiers` or `/qtiers settings`: open settings
- `/qtiers reload`: reload the config and refetch tiers

## Keybinds (Options → Controls → QTiers)
| Action | Default |
|---|---|
| Open nearest player's tiers | H |
| Cycle PvPTiers / SubTiers / MCPVP / PVPHQ gamemode | unbound |
| Toggle QTiers | unbound |
| Open settings | unbound |

## Install
Put `qtiers-1.3.0.jar` and **Fabric API** in `.minecraft/mods`. You need Fabric Loader 0.16 or newer.

## Build
```
gradlew build
```
The jar is written to `build/libs/`.

## Credits
- Gamemode icons: from each tier list's own website ([PvPTiers](https://pvptiers.com), [SubTiers](https://subtiers.net), [MCPVP](https://www.mcpvp.com), [PVPHQ](https://pvphq.com)). They belong to those sites.
- Tier colors: from mcpvp.com.
- Tier data: [PvPTiers](https://pvptiers.com) and [SubTiers](https://subtiers.net) public APIs, the [PVPHQ](https://pvphq.com) API, and public [MCPVP](https://www.mcpvp.com) profile pages (MCPVP has no API).
- Skin renders: [Visage](https://visage.surgeplay.com), with [mc-heads](https://mc-heads.net) as the fallback.
- The layout idea and profile screen come from [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers). No Tiers code or textures are used.

## License
QTiers is licensed under the [GNU GPL v3.0](LICENSE). The gamemode icons belong to their tier-list websites and are not covered by this license.
