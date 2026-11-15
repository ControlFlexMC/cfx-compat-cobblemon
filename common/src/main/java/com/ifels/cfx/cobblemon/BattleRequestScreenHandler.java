package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.HudStateReason;
import com.ifels.controlflex.api.IScreenHandler;
import com.ifels.controlflex.api.IScreenHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speaks for {@code BattleConfigureGUI}: owns the hints so the built-in {@code hud_toggle} can
 * hide them, and leaves Escape alone.
 *
 * <p>Back is deliberately not intercepted. The request screen has no inner pages — the format
 * arrows are actions, not a sub-page — so a back press should do what it always did: close the
 * panel. Returning {@code false} lets ControlFlex fall through to its default.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestScreenHandler implements IScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean done;

    private BattleRequestScreenHandler() {
    }

    /** Idempotent; must run from the ready callback, after ControlFlex published its registry. */
    public static void register() {
        if (done) {
            return;
        }
        try {
            IScreenHandlerRegistry registry = ControlFlexApi.getScreenHandlerRegistry();
            if (registry == null) {
                LOGGER.info("[cfx-compat-cobblemon] screen handler registry unavailable "
                        + "(ControlFlex older than 0.13.0); the request hints stay fixed");
                return;
            }
            done = registry.register(BattleRequestScreen.GUI_CLASS, new BattleRequestScreenHandler());
            LOGGER.info("[cfx-compat-cobblemon] screen handler for {} registered={}",
                    BattleRequestScreen.GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register request screen handler: {}",
                    t.toString());
        }
    }

    /** No sub-pages to step out of: Escape should close the panel exactly as before. */
    @Override
    public boolean onBack() {
        return false;
    }

    /** The hints are drawn by this bridge, so ControlFlex must route the toggle here. */
    @Override
    public boolean ownsHud() {
        return true;
    }

    @Override
    public boolean onHudState(boolean visible, HudStateReason reason) {
        BattleRequestHud.setVisible(visible);
        return true;
    }
}
