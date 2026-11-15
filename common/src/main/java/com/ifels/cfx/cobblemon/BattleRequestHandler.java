package com.ifels.cfx.cobblemon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a battle request action when ControlFlex reports it fired.
 *
 * <p>All three actions go through {@link BattleRequestScreen}, i.e. through the same methods
 * and the same button the screen's own widgets use — so a format the server did not offer is
 * never stepped to, and a challenge the server would refuse is never sent.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private BattleRequestHandler() {
    }

    public static void handle(BattleRequestAction action) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc == null ? null : mc.screen;
        if (!BattleRequestScreen.isConfigureScreen(screen)) {
            return;
        }

        if (action == BattleRequestAction.CONFIRM) {
            boolean pressed = BattleRequestScreen.pressPrimary();
            LOGGER.info("[cfx-compat-cobblemon] request screen: primary button -> {}",
                    pressed ? "pressed" : "disabled or absent");
            return;
        }

        int step = action == BattleRequestAction.PREV ? -1 : 1;
        String before = BattleRequestScreen.pageName(screen);
        boolean cycled = BattleRequestScreen.cycle(screen, step);
        if (cycled) {
            LOGGER.info("[cfx-compat-cobblemon] request format {} -> {} ({}/{})",
                    before, BattleRequestScreen.pageName(screen),
                    BattleRequestScreen.page(screen) + 1, BattleRequestScreen.pageCount());
        }
    }
}
