package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Keeps the highlight and the virtual cursor in sync with the page Cobblemon is showing.
 *
 * <p>Actions themselves no longer arrive here: ControlFlex declares them and calls back on the
 * activation edge. What is left is positional state — a page that just appeared starts on tile 0,
 * so the pointer has to start there too, one tick after it mounts.</p>
 *
 * @since 0.1.0
 */
public final class BattleTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

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

        if (screen == null || !BattleScreen.isBattleScreen(screen)) {
            BattleSelection.INSTANCE.reset();
            parkedOn = null;
            return;
        }

        Object selection = BattleScreen.currentSelection(screen);
        if (selection == null) {
            // No page mounted (opponent is thinking). Drop the marker so the next page — which
            // may well be the same class, freshly built — parks the cursor again.
            parkedOn = null;
            return;
        }

        List<BattleScreen.Slot> slots = BattleScreen.slots(selection);
        boolean rootMenu = BattleScreen.kindOf(selection) == BattleScreen.PageKind.ROOT;
        if (slots.isEmpty()) {
            BattleSelection.INSTANCE.sync(selection, 0, rootMenu);
            parkedOn = null;
            return;
        }

        BattleSelection.INSTANCE.sync(selection, slots.size(), rootMenu);

        if (selection != parkedOn) {
            parkedOn = selection;
            BattleScreen.Slot current = slots.get(BattleSelection.INSTANCE.index());
            warpCursor(current);
            LOGGER.info("[cfx-compat-cobblemon] page opened: cursor parked on {} ({}/{})",
                    current.action().id(), BattleSelection.INSTANCE.index() + 1, slots.size());
        }
    }

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
}
