# cfx-compat-cobblemon

ControlFlex bridge mod for [Cobblemon](https://modrinth.com/mod/cobblemon) battle and summary
screens on Minecraft 1.20.1, for Forge and Fabric.

Cobblemon's battle UI is pointer-only: the option tiles and move bars accept mouse clicks but are
not focusable widgets. This bridge turns "what can I do right now" into ordinary ControlFlex
actions, replays the press through Cobblemon's own click path, and draws the actually-bound
button on each tile.

- Minecraft 1.20.1
- Forge 47.4.4 or Fabric
- Cobblemon 1.5.2+1.20.1
- ControlFlex 0.9.0 or newer (API 0.9.2.2)

## Why this branch compiles against Cobblemon

Cobblemon 1.5.2 is the final Cobblemon release for 1.20.1, so this branch takes a direct
`compileOnly` / `modCompileOnly` dependency instead of the reflection gateway used by the 1.21.1
branch. That gives compile-time checked tile access and Cobblemon's own size constants. The
trade-off is deliberate: another Cobblemon build may fail at class-load or link time rather than
silently dropping actions. The 1.21.1 branch keeps the reflection approach for Cobblemon 1.8.

## Actions

The 18 battle actions are declared for `BattleGUI`, so ControlFlex lists them under
Mods → Cobblemon → Screen 适配 → `BattleGUI`.

| Group | Actions | Fires on |
| --- | --- | --- |
| Root menu | `battle_fight` / `battle_switch` / `battle_capture` / `battle_run` | action menu |
| Moves | `battle_move_1` … `battle_move_4` | move page, only selectable moves |
| Party | `battle_party_1` … `battle_party_6` | party page, including forced switches |
| Navigation | `battle_prev` / `battle_next` | any page with more than one slot |
| Log | `battle_log_up` / `battle_log_down` | any time the battle screen is open |

Defaults: `prev = LB`, `next = RB`, `log_up = right stick up`, `log_down = right stick down`.
Everything else starts unbound.

- **Root tiles are semantic, not positional.** Cobblemon drops the party tile when the active
  Pokémon is trapped, shifting the rest; the bridge maps fight / party / capture / run and forfeit
  by icon texture.
- **Per-tile actions click immediately** through `Screen.mouseClicked` at the tile centre, so
  Cobblemon's own hit testing still rejects disabled moves and illegal targets.
- **LB/RB** move the highlight and park the ControlFlex cursor on it, so the built-in `gui_press`
  (A) activates the ringed tile.
- **Back** is not an action: the bridge registers a screen handler for `BattleGUI`, so the built-in
  back press steps one page out (move/party/confirm → action menu) and only minimises at the root.
- **Confirmation page:** 1.5.2 has one `Forfeit` button. The cursor is parked on it, the HUD shows
  the `gui_press` binding, A confirms and B cancels. There is no separate confirm action.
- **Log** scrolls three rows per press.
- **Hints:** a badge with the bound button on each tile, a ring on the current tile, an LB/RB hint
  in the bottom-right and the built-in `hud_toggle` hint in the bottom-left. The toggle hint is
  hidden on the pages where Cobblemon draws its own back arrow.

The radial interaction wheel (`InteractWheelGUI`) ships a compat entry: its right stick runs in
`RADIAL_PATH`, with `cursorRadius = 49` for the 138 px wheel (button centres sit
`34.5 × √2 ≈ 48.8` px from the screen centre). Raise or lower it in the per-screen page if a skin
or GUI scale moves the sectors.

## Summary screen

`Summary` gets two actions of its own, scoped to that screen:

| Action | Default | Does |
| --- | --- | --- |
| `summary_tab_prev` / `summary_tab_next` | LB / RB | step through the three tabs (Info / Moves / Stats), wrapping at both ends |

1.5.2's tabs are ordinary buttons whose press plays the click sound and calls Cobblemon's
`displayMainScreen`; a step presses the tab itself, so the mouse and the bumpers take the same
path. The open tab is read from the tab's own private `isActive` flag, falling back to the page
index stored on the screen.

Hints are the battle screen's corner pair: `hud_toggle` bottom-left, LB/RB + "Switch tab"
bottom-right. A `Summary` screen handler makes the toggle work per screen class, and Escape still
closes the summary. The party slots, moves and buttons stay cursor clicks — only the tab row is
exposed to the bumpers.

## Building

Prerequisites: JDK 17 or 21 to run Gradle 8.8, and network access the first time.

```bash
./tools/fetch-cobblemon.sh   # downloads the two 1.5.2 jars into libs/ and checks SHA-1
./tools/build-forge.sh       # builds; deploys only if an instance path is set
./tools/build-fabric.sh      # builds; deploys only if an instance path is set
```

Both build scripts accept `--clean` (run `./gradlew clean` first) and `--release`
(also copy the jar to `RELEASE_DEPLOY_PATH` when set). The Gradle build has no
release switch, so the jar name is the same in both modes.

```bash
./tools/build-forge.sh --clean
./tools/build-fabric.sh --release
```

`libs/` is gitignored. `fetch-cobblemon.sh` accepts `COBBLEMON_FORGE_JAR` and
`COBBLEMON_FABRIC_JAR` to reuse existing copies, and `--force` to re-download.

Deploy targets:

```bash
MC_1_20_1_FORGE_PATH=/path/to/instance/minecraft ./tools/build-forge.sh
MC_1_20_1_FABRIC_PATH=/path/to/instance/minecraft ./tools/build-fabric.sh
```

With the instance variable unset, the script builds only and leaves the jar in
`forge/build/libs` or `fabric/build/libs`. When deploying, the script clears the
instance's `logs/` and `crash-reports/` so the next launch starts clean.
`RELEASE_DEPLOY_PATH` is only used by `--release`.

If `gradle` fails with `Unsupported class file major version`, point `JAVA_HOME` at a JDK 17 or 21
(for example `/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home`).

Tests:

```bash
./gradlew :forge:test :fabric:test
```

## Verification status

- Both jars build: `cfx-compat-cobblemon-0.9.0-mc1.20.1-{forge,fabric}.jar`; neither contains
  Cobblemon classes.
- 45 unit tests pass (`BattleSlots`, `SummaryTabs`, `HudLayout`, `BattleSelection`,
  `BattleActions`, `SummaryActions`, `BattleAction`).
- Forge load is verified on the instance with Cobblemon 1.5.2+1.20.1 and ControlFlex 0.9.0:
  the 0.9.0 acceptance log shows 18 registered actions, `screen handler ... registered=true`, and
  the compat config matched for `cobblemon v1.5.2+1.20.1`. The two summary actions and the
  `Summary` handler are added on top of that log and still need an in-game run.
- Forge in-game verification passed on 2026-11-18. The acceptance log covers the four-tile root
  menu and the three-tile trapped variant, Fight → move page and `battle_move_1`, the party page
  opened from Switch (5 slots), B back to the action menu, five HUD hide/show cycles, and the
  interaction wheel's `right=RADIAL_PATH` entry. No gateway warnings or exceptions were logged.
- The forfeit confirmation and log scrolling were not exercised in that acceptance log; they share
  the same page/extraction pipeline but remain the two in-game paths without captured evidence.
- Summary tab switching, the corner hints and the per-screen toggle are covered by the build and
  unit tests (tab stepping, action registration); they have not yet been captured in an in-game
  acceptance log.
- Fabric runtime is not verified: there is no Fabric test instance here, and ControlFlex's Fabric
  0.9.0 jar has not been built locally. Fabric is covered by the build and the shared unit tests.

## Differences from the 1.21.1 branch

- 20 actions instead of 25: 1.5.2 has no battle-request screen, so the five
  `battle_request_*` actions and the `BattleConfigureGUI` compat entry are gone; both branches
  carry the two summary-tab actions.
- No reflection in the battle or summary gateway; `BattleScreen` and `SummaryScreen` are typed
  adapters compiled against 1.5.2.
- 1.5.2's summary has three tabs (Info / Moves / Stats) instead of the 1.21.1 branch's four.
- HUD layout uses 1.5.2's tile sizes (root 90×26, move 92×24, party 131×40) and puts party badges
  in the bottom-right band.
- Wheel `cursorRadius` is 49 instead of 65.

## Layout

```
common/   actions, BattleSlots/SummaryTabs models, typed adapters, selection, ticker, HUDs
forge/    @Mod entry point, mods.toml, Forge event wiring
fabric/   ClientModInitializer, fabric.mod.json, Fabric event wiring
tools/    fetch-cobblemon.sh, build-forge.sh, build-fabric.sh
```

## Out of scope

- The 1.21.1 battle-request screen and its five actions.
- Dynamax/Z gimmick buttons on the move page (mouse-only).
- The spectator back button.
- The summary's party slots, moves and evolve button — still cursor clicks; only its tab row is on
  the bumpers.
- Cobblemon 1.6+ (not available for 1.20.1) and other Cobblemon UIs (party, PC, Pokédex).
- Controller cursor behaviour itself (ControlFlex owns it).
- Localization beyond `en_us`/`zh_cn`.
