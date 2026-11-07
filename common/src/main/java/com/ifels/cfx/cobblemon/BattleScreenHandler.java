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
 * closing the whole screen, and applies the HUD visibility ControlFlex pushes for the built-in
 * {@code hud_toggle} action.
 *
 * <p>At the root page {@link #onBack} returns {@code false} on purpose: there is nothing to step
 * back to, so ControlFlex's default Escape runs and the battle minimises — same as before this
 * handler existed.</p>
 *
 * @since 0.13.0
 */
public final class BattleScreenHandler implements IScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean done;

    private BattleScreenHandler() {
    }

    /**
     * Register against {@code BattleGUI}. Must be called from
     * {@link CfxCobblemonPlugin#onControlFlexReady()} — not from the mod constructor: ControlFlex
     * publishes the registry during its own client init, which runs seconds after the loaders
     * have constructed this mod. Registering earlier found a {@code null} registry and silently
     * gave up, which is why B kept closing the whole battle screen.
     *
     * <p>Idempotent, so a repeated ready callback cannot produce a duplicate registration.</p>
     */
    public static void register() {
        if (done) {
            return;
        }
        try {
            IScreenHandlerRegistry registry = ControlFlexApi.getScreenHandlerRegistry();
            if (registry == null) {
                LOGGER.info("[cfx-compat-cobblemon] screen handler registry unavailable "
                        + "(ControlFlex older than 0.13.0); Escape still minimises the battle");
                return;
            }
            // One handler per screen class: putIfAbsent semantics, so false means another mod
            // already owns BattleGUI's screen hooks. Nothing to do about that, but say so.
            done = registry.register(BattleScreen.GUI_CLASS, new BattleScreenHandler());
            LOGGER.info("[cfx-compat-cobblemon] screen handler for {} registered={}",
                    BattleScreen.GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register screen handler: {}", t.toString());
        }
    }

    @Override
    public boolean onBack() {
        Screen screen = Minecraft.getInstance().screen;
        if (!BattleScreen.isBattleScreen(screen)) {
            return false;
        }
        Object selection = BattleScreen.currentSelection(screen);
        if (!BattleScreen.canGoBack(selection)) {
            // Root page (or waiting on the opponent): let the default Escape minimise.
            return false;
        }
        boolean switched = BattleScreen.back(screen, selection);
        if (switched) {
            // Cobblemon rebuilds the menu from scratch after this, so tell the highlight which
            // tile it should land on instead of letting a fresh page object reset it to "Fight".
            BattleSelection.INSTANCE.markReturnToMenu();
        }
        LOGGER.info("[cfx-compat-cobblemon] B from {} -> {}",
                selection.getClass().getSimpleName(), switched ? "action menu" : "no handler");
        return switched;
    }

    /** BattleGUI draws its hints through {@link BattleHud}; ControlFlex owns the toggle. */
    @Override
    public boolean ownsHud() {
        return true;
    }

    @Override
    public boolean onHudState(boolean visible, HudStateReason reason) {
        // The toggle is its own feedback: badges, ring and page hint disappear, and the dimmed
        // bottom-left key hint stays as the way back. No extra toast.
        BattleHud.setVisible(visible);
        return true;
    }
}
