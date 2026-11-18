# cfx-compat-cobblemon 1.20.1 — design

- Status: approved in chat on 2026-11-18, including the revision from a reflection
  gateway to a direct Cobblemon 1.5.2 compile dependency.
- Branch: `1.20.1` (the existing `1.21.1` branch is not touched).
- Supersedes for this branch: the reflection strategy described in the 1.21.1 README.

## 1. Context

The repository already contains a working 1.21.1 bridge (Fabric + NeoForge). That bridge targets
Cobblemon 1.8's battle GUI and reaches it entirely through reflection.

Minecraft 1.20.1 has a different situation:

- Cobblemon 1.5.2 (`1.5.2+1.20.1`, 2024-05-27) is the last Cobblemon release for 1.20.1. The
  1.20.1 line will not receive another battle-GUI rewrite.
- The 1.5.2 battle screen is the old one: `BattleGUI` plus four selection classes, with clicks
  dispatched by `Screen` to child `Element`s. It does not have `mousePrimaryClicked`,
  `BattleConfigureGUI`, or the 1.8 request screen.
- ControlFlex 1.20.1 `0.9.0.603` already ships API `0.9.2.2`: native action registration
  (`onRegisterActions`), per-screen handlers (`IScreenHandlerRegistry`), HUD state via
  `hud_toggle`, and `warpToGui`.

Because 1.5.2 is final for 1.20.1, a compile dependency is acceptable and preferable: it removes
reflection plumbing, gives compile-time checked tile access, and lets the adapter read Cobblemon's
own size constants. The cost is deliberate: a different Cobblemon build will fail at class-load or
link time instead of silently dropping actions.

## 2. Scope and decisions

| Decision | Choice |
| --- | --- |
| Minecraft | 1.20.1 |
| Loaders | Forge (`47.4.4`) and Fabric, both built from one `common/` source set |
| Cobblemon | `1.5.2+1.20.1`, direct `compileOnly` / `modCompileOnly` dependency |
| ControlFlex | `>=0.9.0`, API `0.9.2.2` |
| Java | 17 |
| Reflection | none in the battle gateway |
| Battle-request actions | removed (1.5.2 has no request screen) |
| Battle actions | 18, same ids and defaults as 1.21.1 where they still make sense |
| Interaction wheel | ported compat entry, `cursorRadius` recalculated for the 138 px wheel |
| HUD toggle | ControlFlex's built-in `hud_toggle`, state pushed to the bridge |
| Confirmation page | one forfeit button, activated through `gui_press` on the parked cursor |

Goals:

- Controller access to every actionable control on Cobblemon's battle screen.
- Per-tile bindings plus a highlight/cursor pair for `gui_press`.
- Hint badges that show the actually bound button.
- One shared implementation for Forge and Fabric; Forge behaviour is verified in-game, Fabric is
  verified by build and the shared unit suite.

Non-goals: see §16.

## 3. Version matrix and artifacts

| Property | Value |
| --- | --- |
| `minecraft_version` | `1.20.1` |
| `minecraft_version_range` | `[1.20.1,1.20.2)` |
| `forge_version` | `47.4.4` |
| `forge_version_range` | `[47,)` |
| `fabric_loader_version` | `0.15.0` |
| `fabric_api_version` | `0.83.0+1.20.1` |
| `java_version` | `17` |
| `controlflex_version_range` (Fabric) | `>=0.9.0` |
| `controlflex_version_range_forge` | `[0.9.0,)` |
| `cobblemon_version_range` (Fabric) | `>=1.5.2` |
| `cobblemon_version_range_forge` | `[1.5.2,)` |
| `controlflex_api_version` | `0.9.2.2` |
| `mod_version` | `0.1.0` |

Forge's range matches the actual Cobblemon version: Maven `ComparableVersion` ordering puts
`1.5.2+1.20.1` above `1.5.2`, checked locally.

Artifacts:

- `forge/build/libs/cfx-compat-cobblemon-0.1.0-mc1.20.1-forge.jar`
- `fabric/build/libs/cfx-compat-cobblemon-0.1.0-mc1.20.1-fabric.jar`

## 4. Architecture and modules

```
common/   actions, BattleSlots model, BattleScreen adapter, selection, ticker, HUD
forge/    @Mod entry point, mods.toml, Forge event wiring
fabric/   ClientModInitializer, fabric.mod.json, Fabric event wiring
tools/    fetch-cobblemon.sh, build-forge.sh, build-fabric.sh
```

`common/` is shared by both loader modules through the existing sourceSet include. There are no
mixins.

Boundaries:

- `BattleSlots` — pure Java. The page kind, slots, resource-to-action mapping, move/party index
  mapping, and the "can go back / back arrow shown" decisions. No Minecraft or Cobblemon imports.
- `BattleScreen` — the only class that imports Cobblemon. Converts the live `BattleGUI` into a
  `BattleSlots.Page` and performs clicks, back, and log scrolling.
- `BattleSelection` — pure highlight state.
- `BattleTicker` — parks the ControlFlex cursor on the highlighted slot.
- `BattleActionHandler` — maps a ControlFlex action id to a slot and clicks it.
- `BattleScreenHandler` — `IScreenHandler` for `BattleGUI` (back + HUD state).
- `BattleHud` / `HudLayout` — pure layout decisions plus drawing.
- `CfxCobblemonPlugin` — ControlFlex SPI entry point.

This boundary keeps all Minecraft-free logic unit-testable and confines the compile dependency to
one adapter class.

## 5. Build and Cobblemon dependency acquisition

### 5.1 Local jars

Both production jars are fetched into `libs/` (gitignored):

| File | Source | SHA-1 |
| --- | --- | --- |
| `Cobblemon-forge-1.5.2+1.20.1.jar` | `https://cdn.modrinth.com/data/MdwFAVRL/versions/vm5zUZAg/Cobblemon-forge-1.5.2%2B1.20.1.jar` | `02601f79f0c442b75463538da7a3c7f260fda42e` |
| `Cobblemon-fabric-1.5.2+1.20.1.jar` | `https://cdn.modrinth.com/data/MdwFAVRL/versions/EVozVxCq/Cobblemon-fabric-1.5.2%2B1.20.1.jar` | `2465e767f2659240f1c16549e602cb6e8b9145db` |

`tools/fetch-cobblemon.sh`:

- Creates `libs/`.
- Skips a download when the file exists with the expected SHA-1.
- Supports `COBBLEMON_FORGE_JAR` and `COBBLEMON_FABRIC_JAR` as existing local sources; copies them
  into `libs/` after verifying the hash.
- Re-downloads with `--force`.
- Fails loudly on hash mismatch or download failure.

The build scripts call it before Gradle; a missing jar in Gradle also produces an actionable
`GradleException` naming the script.

`libs/` is added to `.gitignore` explicitly (the repository's existing `*.jar` rule already covers
the files, but the directory should be obvious to contributors).

### 5.2 Forge module

`forge/build.gradle` uses `net.neoforged.moddev.legacyforge` 2.0.141, exactly like
control-flex-1.20.1:

```groovy
legacyForge {
    version = "${minecraft_version}-${forge_version}"
    runs { client { client() } }
    mods { "${mod_id}" { sourceSet sourceSets.main } }
}

dependencies {
    compileOnly files(rootProject.file("libs/Cobblemon-forge-1.5.2+1.20.1.jar"))
    compileOnly "com.github.ControlFlexMC:control-flex-api:${controlflex_api_version}"
    compileOnly "org.jetbrains.kotlin:kotlin-stdlib:1.9.24"
}
```

The Cobblemon Forge jar is a production jar using Mojang official member names (`m_...`). ModDev
legacyforge compiles against the official mappings and reobfuscates the output, so direct calls to
Minecraft methods and Cobblemon methods both link.

The ControlFlex API (`com.github.ControlFlexMC:control-flex-api:0.9.2.2`) is resolved from
`mavenLocal()`. The machine used for this work already has it; a clean machine publishes it with
`tools/publish_maven_local.sh` in the `control-flex-api` checkout before building.

Build tooling: Gradle 8.8 with a Java 17 toolchain. `settings.gradle` keeps the plugin-management
repositories required by the two plugins: FabricMC Loom for Fabric and MinecraftForge plus
SpongePowered for `net.neoforged.moddev.legacyforge`.

Both loader modules expand `mods.toml` / `fabric.mod.json` through `processResources` and bundle
`LICENSE`. The Forge jar also carries `pack.mcmeta` with `pack_format = 15`.

### 5.3 Fabric module

`fabric/build.gradle` uses Loom 1.6 with `loom.officialMojangMappings()` and a mod dependency, so
Loom remaps the intermediary production jar to Mojang names in the dev classpath:

```groovy
modCompileOnly files(rootProject.file("libs/Cobblemon-fabric-1.5.2+1.20.1.jar"))
```

If Loom does not remap a file-based mod dependency, the fallback is a tiny local Maven repository
(`libs/maven`) containing the same jar and a minimal POM, consumed as
`modCompileOnly "cfx.local:cobblemon-fabric:1.5.2"`, which Loom definitely remaps as a Maven mod.
The compile spike in §17 decides which form is committed.

### 5.4 Compile spike (first implementation step)

Before any bridge code, both modules compile a throwaway class that exercises the parts a
remapping mistake would break:

- Import `com.cobblemon.mod.common.client.gui.battle.BattleGUI`, cast a `Screen`, call
  `getCurrentActionSelection()`.
- Call `getTiles()` on a `BattleGeneralActionSelection`, read a `BattleOptionTile`, and assign
  `tile.getResource()` to `net.minecraft.resources.ResourceLocation` and `tile.getText()` to
  `net.minecraft.network.chat.Component`. This is the part that fails if Loom does not remap the
  Fabric jar from intermediary to Mojang names.
- Call `screen.mouseClicked(0, 0, 0)` and cast a `BattleMessagePane` child to
  `AbstractSelectionList<?>` to prove the two Minecraft-owned call sites compile.

`:forge:compileJava` and `:fabric:compileJava` must pass. This is the gate for the whole
direct-dependency approach; if either fails, adjust the dependency wiring (§5.3 fallback, Kotlin
stdlib, or Forge mappings) before proceeding. The throwaway class is deleted immediately after
the gate.

### 5.5 Metadata

Forge `META-INF/mods.toml`:

- `modLoader = "javafml"`, `loaderVersion = "[47,)"`.
- Dependencies: `forge [47,)`, `minecraft [1.20.1,1.20.2)`, `controlflex [0.9.0,)` `AFTER`,
  `cobblemon [1.5.2,)` `AFTER`; all `side = "CLIENT"`.
- `displayTest = "IGNORE_ALL_VERSION"`.
- `[[mods]] side = "CLIENT"`.

Fabric `fabric.mod.json`:

- `environment: client`.
- `depends`: `fabricloader >=0.15.0`, `fabric-api >=0.83.0+1.20.1`, `minecraft 1.20.1`,
  `java >=17`, `controlflex >=0.9.0`, `cobblemon >=1.5.2`.

Both depend on ControlFlex and Cobblemon being present at runtime; Cobblemon is not embedded.

### 5.6 Entry points

Forge (`forge/.../CfxCobblemonForge.java`):

- `@Mod("cfx_compat_cobblemon")`, client dist.
- `@Mod.EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT)`.
- `TickEvent.ClientTickEvent` with `phase == END` calls `BattleTicker.tick()`.
- `ScreenEvent.Render.Post` calls `BattleHud.render(event.getGuiGraphics(), event.getScreen())`.

Fabric (`fabric/.../CfxCobblemonFabric.java`):

- `ClientModInitializer`.
- `ClientTickEvents.END_CLIENT_TICK` calls `BattleTicker.tick()`.
- `ScreenEvents.AFTER_INIT` registers `afterRender` calling `BattleHud.render(...)`.

## 6. Action catalog and ControlFlex integration

18 actions, ids unchanged from 1.21.1 so bindings remain conceptually stable:

| Group | Actions | Fires on |
| --- | --- | --- |
| Root | `battle_fight`, `battle_switch`, `battle_capture`, `battle_run` | action menu |
| Moves | `battle_move_1` … `battle_move_4` | move page, only selectable moves |
| Party | `battle_party_1` … `battle_party_6` | party page, including forced switches |
| Navigation | `battle_prev`, `battle_next` | any page with more than one slot |
| Log | `battle_log_up`, `battle_log_down` | while the battle screen is open |

Registration:

- `ActionLayer.SCREEN`, `targetClass(BattleSlots.BATTLE_GUI_CLASS)`, `groupModId("cobblemon")`.
- Defaults: `prev = LB`, `next = RB`, `log_up = RS up`, `log_down = RS down`; all others unbound.
- No pseudo `KeyMapping`; declarations go through `IActionRegistrar`.

Compat config `assets/cfx_compat_cobblemon/compat/cfx_compat_cobblemon.json`:

- `cobblemon` / `BattleGUI` entry so the screen appears under Mods → Cobblemon → Screen 适配.
- `InteractWheelGUI` entry: `rightStick` `RADIAL_PATH`, `threshold 0.5`, `cursorRadius 49`.
  The 1.5.2 wheel is 138 px; its four buttons are 69 px quadrants, so their centres are
  `34.5 × √2 ≈ 48.8` px from the screen centre. `49` lands on the centre. The 1.21.1 value of 65
  belongs to the 170 px wheel.
- No `BattleConfigureGUI` entry.

Screen handler registration happens in `CfxCobblemonPlugin.onControlFlexReady()` (the registry is
not published earlier), is idempotent, and is registered for the exact `BattleGUI` class name.

## 7. Battle gateway

### 7.1 `BattleSlots` (pure model)

```java
public final class BattleSlots {
    public static final String BATTLE_GUI_CLASS =
            "com.cobblemon.mod.common.client.gui.battle.BattleGUI";

    public enum PageKind { ROOT, MOVE, SWITCH, CONFIRM, OTHER }

    public record Slot(BattleAction action, float x, float y, float width, float height,
                       int row, int labelWidth) { ... }

    public record Page(Object id, PageKind kind, List<Slot> slots) { ... }

    static BattleAction rootActionFor(String resourcePath);
    static BattleAction moveAction(int index);
    static BattleAction partyAction(int index);
    static boolean canGoBack(PageKind kind);
    static boolean backArrowShown(PageKind kind);
}
```

- `Slot.centerX()/centerY()` are the click point; `displayName()` returns the action id or
  `"(confirm button)"`.
- `Page` is immutable; `Page.empty()` has a `null` id, `PageKind.OTHER`, and no slots.
- `rootActionFor` matches resource **path suffixes**: fight, switch, bag → capture, run/forfeit →
  run. Position is never used: the trapped-party case removes the switch tile and shifts the rest.
- `canGoBack` is true for `MOVE`, `SWITCH`, `CONFIRM`; false for `ROOT` and `OTHER`.
- `backArrowShown` equals `canGoBack`: 1.5.2 draws its back button on all three sub-pages and never
  on the action menu.

### 7.2 `BattleScreen` (typed adapter)

Public API, no Cobblemon types:

```java
public static boolean isBattleScreen(Screen screen);
public static BattleSlots.Page page(Screen screen);
public static boolean click(Screen screen, BattleSlots.Slot slot);
public static boolean canGoBack(Screen screen);
public static boolean back(Screen screen);
public static void scrollLog(Screen screen, int lines);
```

Internals:

- `screen instanceof BattleGUI`; `gui.getCurrentActionSelection()`.
- `instanceof` on `BattleGeneralActionSelection`, `BattleMoveSelection`,
  `BattleSwitchPokemonSelection`, `ForfeitConfirmationSelection` chooses the page.
- Root slots: `getTiles()`, `getResource().getPath()`, `getX()`, `getY()`, width/height from
  `BattleOptionTile.OPTION_WIDTH/OPTION_HEIGHT`, row `i / 2`, label width from
  `Minecraft.getInstance().font.width(tile.getText())`.
- Move slots: `getMoveTiles()`, skip `!tile.getSelectable()`, action `moveAction(i)`, size
  `BattleMoveSelection.MOVE_WIDTH/MOVE_HEIGHT`, row `i / 2`. A disabled move is skipped but the
  remaining moves keep their original numbers.
- Switch slots: `getTiles()`, action `partyAction(i)`, size `BattleOverlay.TILE_WIDTH/TILE_HEIGHT`,
  row `i / 2`, at most 6.
- Confirm slot: `getForfeitButton()`, action `null`, size
  `BattleOptionTile.OPTION_WIDTH/OPTION_HEIGHT`, row 0.
- Click: `screen.mouseClicked(slot.centerX(), slot.centerY(), 0)`, with `screen` typed as
  `Screen`. This is exactly what a real mouse click does: `BattleGUI.mouseClicked` calls
  `super.mouseClicked`, which dispatches to the mounted selection, which hit-tests its tiles. Two
  consequences matter:
  - The call site's owner is a Minecraft class (`Screen`), so Forge reobfuscation and Loom
    remapping translate it reliably. Calling the inherited method through a Cobblemon-typed
    reference would put a Cobblemon class in the `invokevirtual` owner and rely on the remapper
    resolving an inherited Minecraft member across a third-party class; that is the fragile case
    this design avoids.
  - Disabled moves and illegal targets are still rejected by Cobblemon's own hit test.
  `Screen.mouseClicked` returns whether a child consumed the event. `BattleMoveSelection`
  returns `false` even after accepting a move, so the return value is logged as "sent", never as a
  success/failure verdict.
- Back: re-check `BattleSlots.canGoBack(page.kind())`, then `gui.changeActionSelection(null)`,
  mirroring the page's own back button.
- Scroll: find the child `instanceof BattleMessagePane`, cast it to
  `net.minecraft.client.gui.components.AbstractSelectionList<?>`, then
  `list.setScrollAmount(list.getScrollAmount() + lines * BattleMessagePane.LINE_HEIGHT)`. The
  cast keeps the `invokevirtual` owner on a Minecraft class for the same remapping reason as the
  click.
- Every method catches `RuntimeException`, logs once with the action being attempted, and returns
  the empty/false result. A Cobblemon state race must not break the client tick.

The `BattleGUI.changeActionSelection` call is queued by Cobblemon and applied on the next render;
the gateway therefore returns `true` once the request is issued, not once the page has changed.

## 8. Page model

| Page | Cobblemon type | Slots | Slot action | Size | Notes |
| --- | --- | --- | --- | --- | --- |
| Root | `BattleGeneralActionSelection` | up to 4 | fight / switch / capture / run | 90×26 | semantic by icon resource |
| Move | `BattleMoveSelection` | up to 4 | `moveAction(i)` | 92×24 | skip non-selectable |
| Party | `BattleSwitchPokemonSelection` | up to 6 | `partyAction(i)` | 131×40 | includes forced switch |
| Confirm | `ForfeitConfirmationSelection` | 1 | `null` | 90×26 | `gui_press` activates |
| Other | anything else | 0 | — | — | no actions |

Root options in 1.5.2:

- Wild single battle: Fight, Switch (unless trapped), Capture (bag icon), Run. Capture/Run minimise
  the battle and print Cobblemon's chat prompt; the bridge only replays the tile click.
- Trainer/PvP: Fight, Switch (unless trapped), Forfeit (run icon). Forfeit opens the confirmation
  page.

The confirmation page has one `Forfeit` button and a back button. The slot has no bridge action:
the ticker parks the ControlFlex cursor on it, the HUD advertises the `gui_press` binding, A
activates it, and B cancels through the screen handler. No dedicated confirm action is added,
because a second binding that can immediately forfeit is a footgun.

## 9. Selection, ticker, and action handler

`BattleSelection` is unchanged from 1.21.1:

- Identity-keyed page changes reset the highlight to tile 0.
- `markReturnToMenu()` restores the root tile after a sub-page back.
- Pure state; unit-testable.

`BattleTicker` (client tick):

1. Not a battle screen → `reset()`, clear the parked page.
2. `BattleScreen.page(screen)`; if `id == null` → clear parked page.
3. Empty slots → sync with count 0, clear parked page.
4. Otherwise sync the index and, when `page.id()` differs from the last parked page, warp the
   ControlFlex cursor to `slots[index]`.

`BattleActionHandler`:

1. Reject when the open screen is not `BattleGUI`.
2. Log actions scroll ±3 rows and return.
3. Build the page; empty slots return.
4. Sync `BattleSelection`.
5. `PREV`/`NEXT`: move the highlight (wrap), warp the cursor, log the new target.
6. Per-tile action: `select(i)`, `BattleScreen.click(screen, slot)`, log `sent=<boolean>`.
   `BattleMoveSelection.mouseClicked` returns `false` even after accepting a move, so the log must
   not treat `false` as a rejection.

## 10. Screen handler and HUD toggle

`BattleScreenHandler implements IScreenHandler`:

- `onBack()`: battle screen and `BattleScreen.canGoBack(screen)` required; then
  `BattleScreen.back(screen)`; on success `BattleSelection.INSTANCE.markReturnToMenu()` and
  return `true`. Root returns `false`, letting ControlFlex's Escape minimise the battle.
- `ownsHud()` returns `true`.
- `onHudState(visible, reason)` calls `BattleHud.setVisible(visible)` and returns `true`.

Registration in `onControlFlexReady()`:

- `ControlFlexApi.getScreenHandlerRegistry()`; `null` → log and skip (still functional, no toggle).
- `registry.register(BattleSlots.BATTLE_GUI_CLASS, handler)`; idempotent; log the result.

`hud_toggle` remains ControlFlex's built-in action; the bridge does not declare one. The state is
persisted per screen class by ControlFlex.

## 11. HUD layout

Shared glyph constants: 10 px glyph, 2 px gap, colors and font handling from the 1.21.1 HUD.

| Page | Badge placement | Highlight |
| --- | --- | --- |
| Root | inside on the label line when every root badge fits before the icon; otherwise outside above/below alternating by row | 90×26 tile rect |
| Move | inside bottom-left, `INSET = 12`; clears the type icon | rect extended 9 px left (`MOVE_ICON_OVERHANG`) |
| Party | inside bottom-right, `SWITCH_INSET = 2`; below the HP bar | 131×40 tile rect |
| Confirm | centred under the forfeit button, `CONFIRM_BADGE_DROP = 10` | 90×26 button rect |

Root fitting rule: label starts at `x + 6`, the artwork reserves the right 20 px, so a badge fits
when `x + 6 + labelWidth + 4 + badgeWidth <= x + width - 20`. If any root badge does not fit, the
whole page uses the outside rule; a mixed row reads as a bug.

Party tiles: the 131×40 tile leaves the bottom band below the HP bar (bar at `y+22`, height 4,
HP text at `y+22`) free. Outside placement is not viable: the row gap is 5 px and a 10 px glyph
would overlap the neighbouring tile.

Corner hints:

- Page navigation (LB/RB) bottom-right when the page has more than one slot.
- HUD toggle bottom-left, drawn dimmed while the HUD is hidden.
- The toggle hint is suppressed on `MOVE`, `SWITCH`, and `CONFIRM`, because Cobblemon draws its own
  back button in that corner on those pages. It is shown on `ROOT`.
- The confirm page shows the `gui_press` binding on its single button instead of a page-nav hint.

HUD rendering is gated on `IActionBindingProvider`; no provider → nothing drawn. The highlight,
badges, and hints all resolve bindings live, so a rebind or controller swap shows on the next
frame.

Porting note: 1.20.1's `ResourceLocation` constructor is deprecated for removal; glyph textures use
`ResourceLocation.tryParse(glyph.getTextureId())` with a null guard. `GuiGraphics`, `graphics.pose()`, and the `blit` /
`drawString` overloads used by the 1.21.1 HUD exist unchanged in 1.20.1.

## 12. Error handling and logging

- Missing Cobblemon jar at build time → `GradleException` naming `tools/fetch-cobblemon.sh`.
- `BattleScreen` catches `RuntimeException` and logs once per failure site; it never crashes the
  tick or render pass.
- No screen handler registry → log once, continue without the HUD toggle and back interception.
- No cursor controller → per-tile actions still work; `PREV`/`NEXT` moves the highlight but
  `gui_press` cannot follow it. Log once.
- One log line per page open, per action, and per HUD state change. No per-frame logging.

All logging uses the `[cfx-compat-cobblemon]` prefix and the `cfx_compat_cobblemon` logger.

## 13. Testing

### 13.1 Unit tests (`common/src/test`, run by both modules)

| Test | Covers |
| --- | --- |
| `BattleSlotsTest` | root resource → action; move/party index mapping; `canGoBack`; `backArrowShown`; slot centre; `Page.empty` |
| `HudLayoutTest` | root inside/outside rule; move inside + 9 px highlight overhang; party bottom-right; confirm centred drop |
| `BattleSelectionTest` | identity reset, wrap, root restore, select, reset |
| `BattleActionsTest` | 18 registrations, target class, group, layer, defaults, id round-trip |
| `BattleActionTest` | ids/translation keys unique; compat JSON has no `screenKeys` |

Deleted: `BattleRequestActionsTest`, `BattleScreenBackArrowTest`, `BattleScreenConfirmTest`
(the last two encoded 1.8-only semantics: a root back arrow and two confirm buttons).

Commands:

```bash
./tools/fetch-cobblemon.sh
./gradlew :forge:test :fabric:test
```

Tests must not load `BattleScreen` or any Cobblemon class; the Cobblemon jar is compile-only and
absent from the test runtime. This is why `BattleSlots`, `HudLayout`, and `BattleSelection` are
pure.

### 13.2 Compile/build verification

```bash
./gradlew :forge:build :fabric:build
for jar in forge/build/libs/*-forge.jar fabric/build/libs/*-fabric.jar; do
  unzip -l "$jar" | grep -q 'com/cobblemon/' && echo "LEAKED: $jar" && exit 1
done
```

Both jars must exist and neither must contain Cobblemon classes (compile-only).

### 13.3 Forge in-game acceptance

Instance: `/Users/ifels/.prism/instances/cobblemon-1.20.1-forge/minecraft`
(Cobblemon 1.5.2, ControlFlex 0.9.0.603, KotlinForForge 4.12.0).

1. Wild battle: root actions Fight/Switch/Capture/Run; A on the highlighted tile.
2. Move page: four move actions, disabled move skipped, B returns to root, LB/RB moves the ring.
3. Trainer battle: Forfeit opens confirmation; A forfeits; B cancels.
4. Forced switch: party actions select the correct Pokémon.
5. Log scroll up/down.
6. `hud_toggle` hides/shows badges, ring, and hints; state survives a new battle.
7. Interaction wheel: right stick parks on all four sectors.
8. No exceptions in `logs/latest.log`; the plugin logs 18 declared actions.

Deploy:

```bash
./tools/build-forge.sh
```

The script builds, copies the jar into `mods/`, and clears `logs/`.

### 13.4 Fabric verification

`:fabric:build` and `:fabric:test` must pass. There is no Fabric test instance, and the
ControlFlex 0.9.0 Fabric jar is not built locally yet, so Fabric runtime behaviour is not claimed.
If a Fabric instance is added later, build ControlFlex's `:fabric:build` and repeat §13.3.

## 14. Migration and cleanup

- Replace `neoforge/` with `forge/` (entry point, `mods.toml`, build script); delete
  `tools/build-neoforge.sh`.
- Delete all `BattleRequest*` classes, their tests, lang keys, and the `BattleConfigureGUI` compat
  entry: 1.5.2 has no battle-request screen.
- Rewrite `BattleScreen` as the typed adapter; add `BattleSlots`.
- Update `BattleHud`, `HudLayout`, `BattleTicker`, `BattleActionHandler`, `BattleScreenHandler`,
  `BattleActions`, `CfxCobblemonPlugin` to the new page model.
- Rewrite README for 1.20.1: direct compile dependency, 18 actions, no request screen, wheel radius
  49, build instructions, out-of-scope list.
- Keep the `1.21.1` branch untouched.

Commit sequence:

1. `chore(1.20.1): retarget the build to Forge and pin Cobblemon 1.5.2`
2. `refactor(bridge): replace the reflection gateway with a typed BattleGUI adapter`
3. `feat(hud): lay out badges for the 1.5.2 battle pages`
4. `chore: drop the battle-request screen and refresh the docs`

## 15. Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Forge production jar + official mappings do not compile as expected | §5.4 compile spike before bridge code; fall back to a locally published Maven jar if needed |
| Loom does not remap a file-based Cobblemon dependency | §5.3 local Maven repository fallback; decided in the spike |
| Kotlin types in Cobblemon signatures need stdlib | `compileOnly kotlin-stdlib:1.9.24`; add only if javac needs it |
| A different Cobblemon build breaks linking | Accepted: 1.5.2 is final for 1.20.1; README states the pin |
| `screen.mouseClicked` returns false after a successful move | Log `sent`, never "rejected"; acceptance is visual |
| Scroll method names differ after remapping | Direct compiled calls, not reflection; verified by the compile spike and in-game log scroll |
| Cursor API absent or cursor not controller-driven | Per-tile actions still work; HUD and highlight still render; logged once |
| `changeActionSelection` is queued | Back reports issued, not applied; `markReturnToMenu` handles the rebuilt root |
| Party badge collision with chords | Right-aligned inside the free bottom band; visual acceptance covers a two-glyph binding |
| Fabric runtime untested | Explicitly documented; shared adapter and identical class names reduce risk |

## 16. Out of scope

- The 1.21.1 battle-request screen and its five actions.
- Dynamax/Z gimmick buttons on the move page (mouse-only).
- The spectator back button.
- Cobblemon 1.6+ (not available for 1.20.1) and any other Cobblemon UI.
- Controller cursor behaviour itself (ControlFlex owns it).
- Localization beyond the existing `en_us`/`zh_cn` files.

## 17. Implementation sequence

1. Compile spike and build wiring (§5): fetch script, Forge/Fabric modules, `libs` gitignore,
   Cobblemon compile dependency, Gradle configuration. Gate: both `compileJava` tasks pass the
   §5.4 spike.
2. `BattleSlots` and its unit tests.
3. `BattleScreen` typed adapter; update `BattleTicker`, `BattleActionHandler`,
   `BattleScreenHandler`.
4. `BattleHud`/`HudLayout` for 1.5.2 sizes and page rules; rewrite `HudLayoutTest`.
5. Actions, plugin, compat JSON, lang; delete request code; loader entry points and metadata.
6. Forge deploy and in-game acceptance (§13.3); fix layout or behaviour issues found.
7. Fabric build/tests; README; final diff review.

## 18. Acceptance criteria

- `./gradlew :forge:test :fabric:test` passes.
- `./gradlew :forge:build :fabric:build` produces both named jars without Cobblemon classes.
- The Forge instance loads the mod with Cobblemon 1.5.2 and ControlFlex 0.9.0.603 and logs 18
  declared actions.
- All §13.3 scenarios pass, with no exceptions in the log.
- The binding UI lists exactly 18 actions under Cobblemon → BattleGUI and no request actions.
- The interaction wheel's right stick parks on all four sectors.
- `1.21.1` remains unchanged on its branch.
