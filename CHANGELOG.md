# Changelog

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
