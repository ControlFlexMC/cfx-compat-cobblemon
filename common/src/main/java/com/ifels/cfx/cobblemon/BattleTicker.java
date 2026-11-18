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
 * Keeps the highlight and the ControlFlex cursor in sync with the page Cobblemon is showing.
 */
public final class BattleTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    /** Page object the cursor was last parked on. Identity, not equality. */
    private static Object parkedOn;

    private BattleTicker() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;

        if (!BattleScreen.isBattleScreen(screen)) {
            BattleSelection.INSTANCE.reset();
            parkedOn = null;
            return;
        }

        Page page = BattleScreen.page(screen);
        if (page.id() == null) {
            parkedOn = null;
            return;
        }
        if (page.slots().isEmpty()) {
            BattleSelection.INSTANCE.sync(page.id(), 0, page.kind() == PageKind.ROOT);
            parkedOn = null;
            return;
        }

        int index = BattleSelection.INSTANCE.sync(page.id(), page.slots().size(),
                page.kind() == PageKind.ROOT);

        if (page.id() != parkedOn) {
            parkedOn = page.id();
            warp(page.slots().get(index));
            LOGGER.info("[cfx-compat-cobblemon] page opened: cursor parked on {} ({}/{})",
                    page.slots().get(index).displayName(), index + 1, page.slots().size());
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
