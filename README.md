# cfx-compat-cobblemon

ControlFlex bridge mod for [Cobblemon](https://modrinth.com/mod/cobblemon) battle screens.

Cobblemon's battle UI is pointer-only: the option tiles are not focusable widgets and only accept
`mousePrimaryClicked(x, y)`, so a controller has to drag a cursor across the screen for every turn.
This bridge turns "what can I do right now" into ordinary ControlFlex actions, replays the press as
a click on the right tile, and draws the actually-bound button on each tile.

MC 1.21.1, Fabric and NeoForge, requires ControlFlex API 0.12.0 or newer.

## Actions

All 18 actions live in `category.cfx_compat_cobblemon.battle` and are unbound by default; bind them
under ControlFlex → Mods → Cobblemon → Battle.

| Group | Actions | Fires on |
| --- | --- | --- |
| Root menu | `battle_fight` / `battle_switch` / `battle_capture` / `battle_run` | action menu |
| Moves | `battle_move_1` … `battle_move_4` | move page, only the moves the Pokémon actually has |
| Party | `battle_party_1` … `battle_party_6` | party page, including forced switches |
| Navigation | `battle_prev` / `battle_next` | any sub-page: walks the highlight and parks the cursor |
| Log | `battle_log_up` / `battle_log_down` | any time the battle screen is open |

Back is not an action either: the bridge registers a screen handler for `BattleGUI`, so the built-in
back button steps one page out (move/party → action menu) and only minimises at the root.

Showing or hiding the hints uses ControlFlex's own built-in `hud_toggle` action (0.13.0), not an
action of ours — one binding works for every bridge. Its state is stored by ControlFlex per screen
class, so the choice survives leaving the battle and restarting the game.

## How it works

- **No compile dependency on Cobblemon.** Fabric production jars are intermediary, NeoForge's are
  official, so one artifact cannot compile against both. Cobblemon's own class and method names are
  not remapped, so everything goes through reflection (`BattleScreen`) — a future Cobblemon that
  moves these members loses the actions instead of throwing.
- **Semantic, not positional.** Root tiles are identified by icon texture; `trapped` drops the party
  tile and shifts the rest left, so a positional mapping would press capture where the player
  expected the party page.
- **Clicks go through Cobblemon's own hit testing** (`mousePrimaryClicked`), so disabled moves and
  illegal targets are still rejected by Cobblemon.
- **Hints are drawn by this mod**, not by ControlFlex: `IActionBindingProvider` resolves the bound
  button, its glyph and the controller skin in one call, so a rebind or a controller swap shows up
  on the next frame.

## Building

```bash
./tools/build-fabric.sh    # MC_1_21_1_FABRIC_PATH=<instance>/minecraft to deploy
./tools/build-neoforge.sh  # MC_1_21_1_NEOFORGE_PATH=<instance>/minecraft to deploy
```

The deploy step also clears the instance's `logs/`, so the next launch starts with a clean log.

## Layout

```
common/    actions, reflection gateway, ticker, HUD  (loader-neutral)
fabric/    entry point + fabric.mod.json
neoforge/  entry point + neoforge.mods.toml
```
