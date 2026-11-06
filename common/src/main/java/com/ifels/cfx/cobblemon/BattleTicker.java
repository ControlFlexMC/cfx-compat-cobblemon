package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Samples the pseudo keys once per client tick and replays them on Cobblemon's battle screen.
 *
 * <p>Everything is gated on the open screen being {@code BattleGUI}. Presses made anywhere
 * else are drained, so a stray controller press in the inventory does not fire when a battle
 * opens later.</p>
 */
public final class BattleTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static final int LOG_LINES_PER_PRESS = 3;

    /**
     * The sub-page object the cursor was last parked on.
     *
     * <p>Identity, not equality: Cobblemon builds a new sub-page object for every turn and every
     * page switch, and that is exactly the moment the highlight snaps back to tile 0 — so the
     * cursor has to snap with it, or the visible highlight and the real pointer disagree and the
     * built-in {@code gui_press} fires on the wrong tile.</p>
     */
    private static Object parkedOn;

    private BattleTicker() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (screen == null || BattleKeys.isEmpty()) {
            BattleKeys.drainAll();
            return;
        }

        // Handled outside the battle-screen gate: the overlay can be switched off (or back on)
        // from any screen, and the choice carries into the next battle.
        if (consume(BattleAction.HUD_TOGGLE)) {
            BattleHud.toggle();
        }

        if (!BattleScreen.isBattleScreen(screen)) {
            BattleKeys.drainAll();
            return;
        }

        // The log is scrollable even while the opponent is thinking (no sub-page mounted).
        consume(BattleAction.LOG_UP, () -> BattleScreen.scrollLog(screen, -LOG_LINES_PER_PRESS));
        consume(BattleAction.LOG_DOWN, () -> BattleScreen.scrollLog(screen, LOG_LINES_PER_PRESS));

        Object selection = BattleScreen.currentSelection(screen);
        if (selection == null) {
            // No page mounted (opponent is thinking). Drop the marker so the next page — which
            // may well be the same class, freshly built — parks the cursor again.
            parkedOn = null;
            BattleKeys.drainAll();
            return;
        }

        List<BattleScreen.Slot> slots = BattleScreen.slots(selection);
        if (slots.isEmpty()) {
            BattleSelection.INSTANCE.sync(selection, 0);
            parkedOn = null;
            BattleKeys.drainAll();
            return;
        }

        // LB/RB walk the highlight; confirm activates whatever it is on. Direct per-tile bindings
        // still work and drag the highlight along, so the two input styles stay consistent.
        BattleSelection selectionState = BattleSelection.INSTANCE;
        selectionState.sync(selection, slots.size());

        // A page that just appeared starts with the highlight on tile 0, so the pointer starts
        // there too — one tick after the page mounts, before the player can press anything.
        if (selection != parkedOn) {
            parkedOn = selection;
            BattleScreen.Slot first = slots.get(selectionState.index());
            warpCursor(first);
            LOGGER.info("[cfx-compat-cobblemon] page opened: cursor parked on {} ({}/{})",
                    first.action().id(), selectionState.index() + 1, slots.size());
        }

        int delta = 0;
        if (consume(BattleAction.PREV)) {
            delta -= 1;
        }
        if (consume(BattleAction.NEXT)) {
            delta += 1;
        }
        if (delta != 0) {
            int moved = selectionState.move(delta, slots.size());
            BattleScreen.Slot slot = slots.get(moved);
            // Put the real cursor on the highlighted tile so the built-in gui_press (A) hits it.
            warpCursor(slot);
            LOGGER.info("[cfx-compat-cobblemon] highlight -> {} ({}/{})",
                    slot.action().id(), moved + 1, slots.size());
        }

        for (int i = 0; i < slots.size(); i++) {
            BattleScreen.Slot slot = slots.get(i);
            KeyMapping key = BattleKeys.get(slot.action());
            if (key == null) {
                continue;
            }
            while (key.consumeClick()) {
                selectionState.select(i);
                boolean accepted = BattleScreen.click(selection, slot);
                LOGGER.info("[cfx-compat-cobblemon] {} at ({}, {}) -> {}",
                        slot.action().id(), (int) slot.centerX(), (int) slot.centerY(),
                        accepted ? "accepted" : "rejected by screen");
            }
        }

        // Anything not backed by a tile right now (move 4 on a two-move Pokémon, party 5/6 in a
        // small team) is dropped rather than saved for later.
        BattleKeys.drainAll();
    }

    /**
     * Warp the virtual cursor onto a tile centre.
     *
     * <p>This is what replaces a dedicated confirm action: ControlFlex's built-in {@code gui_press}
     * clicks wherever the cursor is, so moving the cursor and moving the highlight are the same
     * gesture.</p>
     *
     * <p>Wrapped because {@code getCursorController()} does not exist in API versions before
     * 0.11.0 — there the highlight still moves, only the cursor stays put.</p>
     */
    private static void warpCursor(BattleScreen.Slot slot) {
        try {
            ICursorController cursor = ControlFlexApi.getCursorController();
            if (cursor != null) {
                cursor.warpToGui(slot.centerX(), slot.centerY());
            }
        } catch (Throwable ignored) {
            // older ControlFlex: no cursor API
        }
    }

    private static boolean consume(BattleAction action) {
        KeyMapping key = BattleKeys.get(action);
        if (key == null) {
            return false;
        }
        boolean fired = false;
        while (key.consumeClick()) {
            fired = true;
        }
        return fired;
    }

    private static void consume(BattleAction action, Runnable onFire) {
        if (consume(action)) {
            onFire.run();
        }
    }
}
