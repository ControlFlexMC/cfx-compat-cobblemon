package com.ifels.cfx.cobblemon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs one summary tab action when ControlFlex reports it fired.
 *
 * <p>Goes through {@link SummaryScreen}, i.e. presses the tab button itself — the same path a
 * mouse click takes — so leaving a page still runs Cobblemon's own bookkeeping (click sound,
 * closing the move-swap panel) and the bumper can never select a tab the screen does not have.</p>
 *
 * @since 0.9.1
 */
public final class SummaryActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private SummaryActionHandler() {
    }

    public static void handle(SummaryAction action) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc == null ? null : mc.screen;
        if (!SummaryScreen.isSummaryScreen(screen)) {
            return;
        }
        SummaryScreen.Switch moved = SummaryScreen.cycle(
                screen, action == SummaryAction.PREV ? -1 : 1);
        if (moved == null) {
            LOGGER.info("[cfx-compat-cobblemon] summary: no tab row to switch (screen={})",
                    screen.getClass().getSimpleName());
            return;
        }
        LOGGER.info("[cfx-compat-cobblemon] summary tab {} -> {} ({}/{})",
                moved.from() + 1, moved.to() + 1, moved.to() + 1, moved.count());
    }
}
