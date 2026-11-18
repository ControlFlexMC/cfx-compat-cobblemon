package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.HudStateReason;
import com.ifels.controlflex.api.IScreenHandler;
import com.ifels.controlflex.api.IScreenHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speaks for {@code BattleGUI}: absorbs the back press so it steps out one sub-page instead of
 * closing the whole screen, and applies the HUD state ControlFlex pushes.
 */
public final class BattleScreenHandler implements IScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean done;

    private BattleScreenHandler() {
    }

    public static void register() {
        if (done) {
            return;
        }
        try {
            IScreenHandlerRegistry registry = ControlFlexApi.getScreenHandlerRegistry();
            if (registry == null) {
                LOGGER.info("[cfx-compat-cobblemon] screen handler registry unavailable "
                        + "(ControlFlex older than 0.9.2.1)");
                return;
            }
            done = registry.register(BattleSlots.BATTLE_GUI_CLASS, new BattleScreenHandler());
            LOGGER.info("[cfx-compat-cobblemon] screen handler for {} registered={}",
                    BattleSlots.BATTLE_GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register screen handler: {}", t.toString());
        }
    }

    @Override
    public boolean onBack() {
        Screen screen = Minecraft.getInstance().screen;
        if (!BattleScreen.isBattleScreen(screen) || !BattleScreen.canGoBack(screen)) {
            return false;
        }
        boolean switched = BattleScreen.back(screen);
        if (switched) {
            BattleSelection.INSTANCE.markReturnToMenu();
        }
        LOGGER.info("[cfx-compat-cobblemon] B -> {}", switched ? "previous page" : "no change");
        return switched;
    }

    @Override
    public boolean ownsHud() {
        return true;
    }

    @Override
    public boolean onHudState(boolean visible, HudStateReason reason) {
        BattleHud.setVisible(visible);
        return true;
    }
}
