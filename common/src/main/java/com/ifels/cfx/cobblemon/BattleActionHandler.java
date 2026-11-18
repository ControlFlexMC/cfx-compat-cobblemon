package com.ifels.cfx.cobblemon;

import com.ifels.cfx.cobblemon.BattleSlots.Page;
import com.ifels.cfx.cobblemon.BattleSlots.PageKind;
import com.ifels.cfx.cobblemon.BattleSlots.Slot;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.ICursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs one battle action when ControlFlex reports it fired.
 */
public final class BattleActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static final int LOG_LINES_PER_PRESS = 3;

    private BattleActionHandler() {
    }

    public static void handle(BattleAction action) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
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

        Page page = BattleScreen.page(screen);
        if (page.slots().isEmpty()) {
            return;
        }

        BattleSelection state = BattleSelection.INSTANCE;
        state.sync(page.id(), page.slots().size(), page.kind() == PageKind.ROOT);

        if (action == BattleAction.PREV || action == BattleAction.NEXT) {
            int moved = state.move(action == BattleAction.PREV ? -1 : 1, page.slots().size());
            Slot slot = page.slots().get(moved);
            warp(slot);
            LOGGER.info("[cfx-compat-cobblemon] highlight -> {} ({}/{})",
                    slot.displayName(), moved + 1, page.slots().size());
            return;
        }

        for (int i = 0; i < page.slots().size(); i++) {
            Slot slot = page.slots().get(i);
            if (slot.action() != action) {
                continue;
            }
            state.select(i);
            boolean sent = BattleScreen.click(screen, slot);
            LOGGER.info("[cfx-compat-cobblemon] {} at ({}, {}) -> sent={}",
                    slot.displayName(), (int) slot.centerX(), (int) slot.centerY(), sent);
            return;
        }
    }

    private static void warp(Slot slot) {
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
