# cfx-compat-cobblemon

ControlFlex bridge mod for [Cobblemon](https://modrinth.com/mod/cobblemon) battle screens.

Cobblemon's battle UI is pointer-only: the option tiles are not focusable widgets and only accept
`mousePrimaryClicked(x, y)`, so a controller has to drag a cursor across the screen for every turn.
This bridge turns "what can I do right now" into ordinary ControlFlex actions, replays the press as
a click on the right tile, and draws the actually-bound button on each tile.

MC 1.21.1, Fabric and NeoForge, requires ControlFlex API 0.9.2.2 or newer (native action
registration and the per-screen handler registry).

## Actions

All 18 battle actions are declared for `BattleGUI` only, so ControlFlex lists them under
Mods → Cobblemon → Screen 适配 → `BattleGUI` — that page is where they are bound, reset (back to
the four defaults) or cleared. The same card's 按键绑定 tab lists every action of the mod.

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

The radial interaction wheel (`InteractWheelGUI`) ships a compat entry instead of actions: its right
stick runs in `RADIAL_PATH`, so a direction parks the cursor on that sector's button rather than
dragging across the ring. `cursorRadius` 65 sits between the wheel's dead centre (41 GUI px) and its
outer ring (85 GUI px, `SIZE = 170`) — move it up or down in the per-screen page if a skin or a GUI
scale puts the sectors somewhere else.

## Battle request screen

`BattleConfigureGUI` gets five actions of its own, scoped to that screen:

| Action | Default | Does |
| --- | --- | --- |
| `battle_request_prev` / `battle_request_next` | LB / RB | step through the formats the server offered (single / double / triple / multi …) |
| `battle_request_level_prev` / `battle_request_level_next` | D-pad ← / → | step through the level rule row ("no restriction", "set level" …) |
| `battle_request_confirm` | Y | presses the primary button — challenge, accept, or invite, whatever it currently reads |

Stepping calls the same `setCurrentPage` the on-screen arrows call (wrap-around included) and
refreshes the button label, so the format the panel shows is always the format that gets sent. The
level row mirrors that, without the label refresh — it only changes what the challenge carries. The
press is refused while the button reports itself disabled.

Hints are drawn on the controls themselves: one glyph outside each arrow on both rows, no caption,
and the press glyph above the button it presses. The toggle hint stays bottom-left, like the battle
screen's.

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
