# Changelog

## 1.5.2 (2026-10-27)

### Added
- **More Minecraft versions.** QTiers now has a jar for each of these versions:
  - 1.21.4
  - 1.21.6 – 1.21.8
  - 1.21.11
  - 26.1 – 26.1.2
  - 26.2
  - 26.3

  Download the one for your version: the file name ends in the Minecraft version, e.g. `qtiers-1.5.2+26.3.jar`.
- **Movable opponent HUD**: press **Move** next to "Opponent HUD" in the settings and drag the box where you want it. **Reset** puts it back in the top-left corner. The position stays the same on any resolution or GUI scale.

## 1.5.1 (2026-10-27)

### Fixed
- **Tiers now show on servers with custom nametags and tab lists, like mcpvp.club.** These servers hide the normal nametag and show names with floating text above the player, and their tab list rows are fake entries. QTiers now adds tiers to the floating name and finds the real player behind each tab row.
- The online players list no longer shows a server's fake tab rows.

### Changed
- Player nametags sit a little higher.

### Added
- `/qtiers debug` writes what the server sends for names and the tab list to the log, to help track down servers where tiers don't show.

## 1.5.0 (2026-10-25)

### Added
- **Tiers load instantly**: QTiers now saves tiers between sessions (in `.minecraft/qtiers/cache.json`), so they show the moment you join a server and refresh in the background. Saved tiers are kept for up to 7 days.
- **Opponent HUD**: when you hit a player, a small box in the top-left corner shows their tiers on every site for 10 seconds. Turn it off with **Opponent HUD** in the settings.
- **Online players**: `/qtiers list` (or a keybind) shows everyone on the server, strongest first, with each site's tier in its own column. Click a name to open their profile.
- **Leaderboards**: `/qtiers top` (or a keybind) shows the top players of PvPTiers, SubTiers or TierMC, overall or per gamemode. Click a name to open their profile.
- **Compare players**: `/qtiers compare <player1> <player2>` shows two players' tiers side by side, site by site, with the better tier marked ▲.
- **Clickable chat names**: open chat and click an online player's name to see their tiers. Turn it off with **Clickable chat names** in the settings.

### Changed
- The settings screen has new **Leaderboards** and **Online players** buttons. **Show highest tier** moved to the **Tiers** page.
- **Refresh tiers** keeps showing the old tiers until the new ones have loaded.
- MCPVP's short label (opponent HUD and site labels) is now **MC**.
- Done on a profile opened from the online players list or a leaderboard takes you back to that list.

## 1.4.0 (2026-10-24)

### Added
- **TierMC** tiers (tiermc.com), from its public API: all 8 gamemodes (Vanilla, Sword, Mace, UHC, Potion, Nethpot, Axe, SMP), plus each player's overall position, points and region. Retired TierMC tiers are shown with the R prefix. TierMC uses its own 8 gamemode icons from tiermc.com, and has its own keybind to cycle its gamemode.
- **Your own name in F5**: in third person you now see your own nametag, with your tiers, like the WhoAmI mod. Turn it off with **Own name in F5** in the settings.
- **Player search**: a search box at the bottom left of the profile screen and at the top of the settings screen. Type a name and press Enter to open that player's tiers.
- **Mod Menu support**: the Configure button in Mod Menu opens the QTiers settings. Mod Menu is optional.

### Changed
- **Tidier icons**: every gamemode icon is now about the same size, centred, and lined up with the tier text, with even spacing after it.
- **Simpler settings screen**: the per-site options (left/right/off and gamemode) moved to their own page behind the **Tiers** button, next to the search box.
- The profile screen shows the five sites in 3 columns. If a stats line is too long for its panel, the points total is left out instead of running into the next panel.

## 1.3.0 (2026-10-08)

### Added
- **MCPVP** tiers (mcpvp.com): all 16 kits, including Shield, Early Game, Late Game, End Game and Spear, plus each player's overall tier and points. MCPVP has no public API, so QTiers reads its public profile pages.
- **PVPHQ** tiers (pvphq.com), from its official API. Inactive PVPHQ tiers are shown like retired tiers (R prefix).
- **Each tier list has its own icons**, taken from its own website, so you can tell at a glance which site a tier is from:
  - PvPTiers: 8 icons from pvptiers.com
  - SubTiers: 12 icons from subtiers.net
  - MCPVP: 16 icons from mcpvp.com, the same ones its profile pages show
  - PVPHQ: 11 icons from pvphq.com
- Keybinds to cycle the MCPVP and PVPHQ gamemodes.

### Changed
- **Tier colors now come from MCPVP**, and tiers now go from 1 to 6 with High, Mid and Low positions (e.g. MT3).
- The profile screen shows the four sites in a 2×2 grid, and each site's panel appears as soon as that site answers. If a panel has more tiers than fit, it ends with "+N more".
- The settings screen fits all four sites on small windows.

### Removed
- The icon style setting (MCTiers / PvPTiers / mcpvp.club). Each tier list now always uses its own icons.

## 1.2.0 (2026-10-08)

### Removed
- **MCTiers is gone.** MCTiers has shut down permanently and its website and API are offline, so QTiers no longer shows MCTiers ranks or tries to contact it. QTiers now shows **PvPTiers** and **SubTiers**.
- The "Cycle MCTiers gamemode" keybind, and the gamemodes only MCTiers ranked (Neth OP, Vanilla).

### Changed
- New default layout: PvPTiers on the left of the name, SubTiers on the right. If you already use QTiers, your own layout is kept.
- Updating from an older version works: your settings load normally, and the old MCTiers entry is dropped automatically.

## 1.1.0 (2026-10-04)

### Added
- **Icon styles**, switchable in `/qtiers` → **Icons**: MCTiers (default), PvPTiers and mcpvp.club (classic). The button previews each style.

### Changed
- QTiers is now licensed under **GPL-3.0**, because the icon styles come from [PvPTiers/Tiers](https://github.com/PvPTiers/Tiers).
- The QTiers icon set from 1.0.1 was replaced by the icon styles above.

### Fixed
- If a site fails to respond, the lookup is retried once. A site that still can't be reached shows **"Couldn't load"** instead of "Not ranked".
- The points and rank line on the profile screen no longer runs into the next column on narrow screens.
- The settings screen now fits small windows. The Done button no longer covers the last row.

## 1.0.1 (2026-10-04)

### Changed
- New gamemode icons drawn for QTiers.

### Fixed
- Long rows on the profile screen no longer overlap the next column on narrow screens.

## 1.0.0 (2026-09-29)

First release.

- Shows **MCTiers**, **PvPTiers** and **SubTiers** ranks next to player names in nametags and the tab list.
- Each site can go left or right of the name, or be turned off.
- Show a specific gamemode per site, or each player's highest tier.
- `/qtiers <player>` opens a profile screen with the player's skin and every tier, peak, rank and point total, plus a NameMC button.
- **H** opens the nearest player's profile.
- Gamemode icons, MCTiers tier colors, and retired tiers shown as `RHT1`.
- All settings in-game with `/qtiers`.
