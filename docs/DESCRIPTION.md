# Control Flex × Cobblemon

A compatibility bridge that puts **Cobblemon's battle on your controller** — Fight, a move, a switch, the log, all one press instead of dragging a cursor across the screen every turn.

---

## What This Does

Without this mod, a controller can **point at** Cobblemon's battle, but it cannot **play** it.

Cobblemon's battle UI is pointer-only by construction. The option tiles are not focusable widgets — they are painted by the page, and the page's only input entry point is `mousePrimaryClicked(x, y)`. There is no focus chain to walk, no widget to tab through, and no key binding that picks a move. ControlFlex can already move its virtual cursor onto a tile and press it, but every turn becomes: drag to Fight, press, drag to the move you want, press — and the same again next turn.

This bridge turns "what can I do right now" into ordinary ControlFlex actions. Each one finds the tile that stands for it and replays the click through Cobblemon's own hit test.

**The result:** Fight on one button, each of your four moves on another, party slots 1–6 on six more, and LB/RB to walk the highlight when you would rather browse. Cobblemon still decides what is legal — the bridge only presses.

---

## Features

- 🎮 **The whole turn on the pad** — Fight · Party · Capture · Run on actions of their own.
- 🗡️ **One button per move** — Move 1 to Move 4, tied to the move slots, not to where they happen to sit on screen.
- 👥 **Party slots 1–6** — including a forced switch, where the page opens straight onto your team.
- ↔️ **Highlight navigation** — LB/RB wraps through the tiles of the current page and parks the cursor on the highlighted one, so the built-in confirm presses exactly what you see.
- 📜 **Battle log** — scroll up and down on the stick without leaving the buttons you are using.
- ↩️ **Back that steps one level** — from the move or party page, back returns to the action menu; only at the menu does it minimise the battle, exactly like Cobblemon's own arrow.
- 🏷️ **Hints on the tiles** — every tile shows the button actually bound to it, drawn with your controller skin's glyph; a rebind or a controller swap shows up on the next frame.
- 👁️ **Hints you can turn off** — ControlFlex's own `hud_toggle` binding hides them, remembers the choice per screen, and leaves a dimmed hint in the corner as the way back.
- ⚔️ **Challenge screen covered too** — step the format with LB/RB, the level rule with the d-pad, and press Challenge / Accept / Invite with Y.
- 📑 **Summary tabs on the bumpers** — LB/RB flips Info / Moves / Stats / Marks on Cobblemon's summary screen, wrapping at both ends, with the same corner hints as the battle screen.
- 🕹️ **Interaction wheel** — the right stick becomes radial over Cobblemon's wheel: a direction parks the cursor on that sector instead of dragging across the ring.
- 🎯 **Clicks go through Cobblemon** — a disabled move or an illegal target is still rejected by Cobblemon, never overridden by the bridge.
- 🎛️ **Fully rebindable** — all 25 actions appear under ControlFlex's Cobblemon card with the same trigger modes a built-in key gets, and reset returns to the shipped defaults.
- 🧩 **No compile dependency on Cobblemon** — everything is read by reflection, so a Cobblemon update that moves these members loses the actions instead of throwing into the render or tick loop.

---

## Dependencies (Required)

| Mod | Version |
|-----|---------|
| [ControlFlex](https://www.curseforge.com/minecraft/mc-mods/controlflex) | ≥ 0.9.0 |
| [Cobblemon](https://www.curseforge.com/minecraft/mc-mods/cobblemon) | ≥ 1.7.0 |

Minecraft **1.21.1**, Fabric or NeoForge — one jar per loader. ControlFlex 0.9.0 ships API 0.9.2.2, which is what native action registration and the per-screen hooks need.

Both dependencies are mandatory. This is a **client-side** mod, so they only need to be installed on your own client.

---

## The Actions

| Group | Actions | Default |
|---|---|---|
| Action menu | Fight · Party · Capture · Run / Forfeit | — |
| Moves | Move 1 · Move 2 · Move 3 · Move 4 | — |
| Party | Party Slot 1 … Party Slot 6 | — |
| Navigate | Previous Option · Next Option | LB / RB |
| Log | Log Scroll Up · Log Scroll Down | RS ↑ / RS ↓ |
| Challenge panel | Previous Format · Next Format | LB / RB |
| Challenge panel | Previous Level Rule · Next Level Rule | D-pad ← / → |
| Challenge panel | Challenge / Accept | Y |
| Summary | Previous Tab · Next Tab | LB / RB |

Eighteen battle actions, five challenge-screen actions and two summary-tab actions. Of the battle set only prev/next (LB/RB) and log up/down (RS ↑/↓) ship bound — the rest start empty on purpose, so they never collide with the template you already run. The five challenge-panel actions and the two summary-tab actions all ship bound.

The battle actions are declared for `BattleGUI` alone and the summary actions for `Summary`, so each set is listed on its own screen page in ControlFlex's Cobblemon card: that is where they are bound, reset to the defaults, or cleared. The card's binding tab lists every action of the mod in one place.

---

## Compatibility

- ✅ Works in singleplayer and multiplayer — client-side only, nothing is needed on the server.
- ✅ One source, identical behaviour on Fabric and NeoForge; the two jars differ only in their metadata.
- ✅ Coexists with the cursor — you can still drag and click anything. The actions are shortcuts, not a mode you have to enter or leave.
- ✅ Coexists with Cobblemon's own mouse handling — the bridge never takes over the screen's input, it only replays clicks into it.
- 📌 Battle actions fire only while Cobblemon's battle screen is open; the five request actions only on the challenge panel, and the two tab actions only on the summary.
- 📌 Actions are bound in ControlFlex. This mod adds no key settings of its own.

---

## How It Works

```
a bound controller button
            ↓
ControlFlex fires cfx_compat_cobblemon:battle_move_2
            ↓
cfx-compat-cobblemon finds the tile that stands for "move 2"
            ↓
BattleGUI's own hit test   mousePrimaryClicked(cx, cy)
            ↓
Cobblemon behaves exactly as if that tile had been clicked
```

The actions are native ControlFlex actions: declared with the SCREEN layer and their own screen class as the target, so ControlFlex only fires them while that screen is open. No `KeyMapping` is created, nothing is written into Cobblemon's input, and the vanilla controls screen is untouched.

Tiles are identified **semantically, not by position**. The four menu tiles are matched by icon texture; moves and party slots by slot index. Cobblemon drops the party tile when your active Pokémon is trapped and shifts the rest left — a positional mapping would press Capture where you expected the party page.

The cursor is kept honest. When a new sub-page mounts, the bridge parks the virtual cursor on the tile that page starts on; LB/RB then warp it along with the highlight. The ring you see and the tile the built-in confirm would press are always the same one, which is what makes "navigate, then press A" work at all.

Back is a screen handler rather than an action: it calls the same page switch Cobblemon's back arrow calls, and returns `false` at the root so Escape minimises the battle exactly as before.

Summary tabs are not reimplemented either. LB/RB find the four `SummaryTab` buttons in the live widget list, read the one currently flagged `isActive`, and press its neighbour — the same `onPress` a mouse click runs, so switching away still saves the active mark, closes the move-swap panel and rebuilds the page content. Because the starting point is read back from the button each time, a tab clicked with the mouse is where the next bumper press steps from.

Hints come from ControlFlex's action binding provider, so the glyph on a tile is whatever button is bound right now, on whichever controller is connected. They are drawn by this mod rather than by Cobblemon, which is why they follow a rebind immediately.

---

## Not Supported

- **Out-of-battle Cobblemon screens** — the party screen, PC and Pokédex are untouched. This bridge speaks for `BattleGUI`, the challenge panel and the summary's tab row, plus one compat entry for the interaction wheel; the summary's party slots, moves and buttons stay cursor clicks.
- **The forfeit confirmation's two buttons have no actions of their own.** The ring plus the built-in confirm press covers them — that is the badge those two buttons advertise.
- **A move that cannot be picked right now** (no PP, disabled by an effect) has no tile, so its action does nothing that turn. Cobblemon would reject the click anyway.
- **Keyboard bindings.** Cobblemon's battle screen has none to mirror; this bridge adds controller actions, it does not add keyboard ones.
- **Choosing a target in a double battle.** If the battle then asks you to pick one, that pick is still a cursor click — the move action only takes the move.

---

## Notes

- This mod is **client-side only** — it does not need to be installed on servers, and it will not affect anything server-side.
- If a move action seems dead, check that the move page is actually open and that the move is usable — Cobblemon would ignore the same click from a mouse.
- Hints can be hidden with ControlFlex's `hud_toggle` binding; the choice is stored per screen and survives leaving the battle and restarting the game.
- Install the JAR that matches your loader (Fabric or NeoForge). There is one Minecraft version on this branch: 1.21.1.
