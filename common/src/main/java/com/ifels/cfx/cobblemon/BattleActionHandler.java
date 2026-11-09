package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runs one battle action when ControlFlex reports it fired.
 *
 * <p>Replaces the old {@code KeyMapping.consumeClick} loop: the trigger arrives as a callback, so
 * there is no key state to poll and no queued click to drain. Everything still goes through
 * Cobblemon's own hit testing ({@code mousePrimaryClicked}) and the virtual cursor, so a
 * controller press means the same thing as a mouse click.</p>
 *
 * @since 0.1.0
 */
public final class BattleActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static final int LOG_LINES_PER_PRESS = 3;

    private BattleActionHandler() {
    }

    public static void handle(BattleAction action) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc == null ? null : mc.screen;
        if (!BattleScreen.isBattleScreen(screen)) {
            return;
        }

        if (action == BattleAction.LOG_UP) {
            BattleScreen.scrollLog(screen, -LOG_LINES_PER_PRESS);
            return;
        }
        if (action == BattleAction.LOG_DOWN) {
            BattleScreen.scrollLog(screen, LOG_LINES_PER_PRESS);
            return;
        }

        Object selection = BattleScreen.currentSelection(screen);
        if (selection == null) {
            return;
        }
        List<BattleScreen.Slot> slots = BattleScreen.slots(selection);
        if (slots.isEmpty()) {
            return;
        }

        BattleSelection state = BattleSelection.INSTANCE;
        boolean rootMenu = BattleScreen.kindOf(selection) == BattleScreen.PageKind.ROOT;
        state.sync(selection, slots.size(), rootMenu);

        if (action == BattleAction.PREV || action == BattleAction.NEXT) {
            int moved = state.move(action == BattleAction.PREV ? -1 : 1, slots.size());
            BattleScreen.Slot slot = slots.get(moved);
            warpCursor(slot);
            // Confirm-page buttons carry no action of their own; name them by role instead.
            String name = slot.action() == null ? "(confirm button)" : slot.action().id();
            LOGGER.info("[cfx-compat-cobblemon] highlight -> {} ({}/{})",
                    name, moved + 1, slots.size());
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            BattleScreen.Slot slot = slots.get(i);
            if (slot.action() != action) {
                continue;
            }
            state.select(i);
            boolean accepted = BattleScreen.click(selection, slot);
            LOGGER.info("[cfx-compat-cobblemon] {} at ({}, {}) -> {}",
                    slot.action().id(), (int) slot.centerX(), (int) slot.centerY(),
                    accepted ? "accepted" : "rejected by screen");
            return;
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
