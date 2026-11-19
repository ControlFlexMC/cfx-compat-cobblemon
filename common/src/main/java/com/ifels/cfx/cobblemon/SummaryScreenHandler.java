package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.HudStateReason;
import com.ifels.controlflex.api.IScreenHandler;
import com.ifels.controlflex.api.IScreenHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Speaks for {@code Summary}: owns the tab hints so ControlFlex's {@code hud_toggle} can hide
 * them, and leaves Escape alone.
 *
 * <p>Back is deliberately not intercepted. The summary has no inner page of this bridge's own —
 * LB/RB switch tabs, Escape closes the screen — so a back press should do what it always did.
 * Returning {@code false} lets ControlFlex fall through to its default.</p>
 *
 * @since 0.10.0
 */
public final class SummaryScreenHandler implements IScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean done;

    /** Package-private so the handler contract (ownership, pushes, back) is unit-testable. */
    SummaryScreenHandler() {
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
                        + "(ControlFlex older than 0.13.0); the summary hints stay fixed");
                return;
            }
            done = registry.register(SummaryScreen.GUI_CLASS, new SummaryScreenHandler());
            LOGGER.info("[cfx-compat-cobblemon] screen handler for {} registered={}",
                    SummaryScreen.GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register summary screen handler: {}",
                    t.toString());
        }
    }

    /** No sub-pages of ours to step out of: Escape should close the summary as before. */
    @Override
    public boolean onBack() {
        return false;
    }

    /** The tab hints are drawn by this bridge, so ControlFlex must route the toggle here. */
    @Override
    public boolean ownsHud() {
        return true;
    }

    @Override
    public boolean onHudState(boolean visible, HudStateReason reason) {
        SummaryHud.setVisible(visible);
        return true;
    }
}
