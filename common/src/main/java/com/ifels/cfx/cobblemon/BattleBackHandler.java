package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IGuiBackHandler;
import com.ifels.controlflex.api.IGuiBackHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Absorbs the back press on the battle screen so it steps out one sub-page instead of closing the
 * whole screen.
 *
 * <p>Registered against {@code BattleGUI} alone. At the root page it returns {@code false} on
 * purpose: there is nothing to step back to, so ControlFlex's default Escape runs and the battle
 * minimises — same as before this handler existed.</p>
 *
 * @since 0.12.0
 */
public final class BattleBackHandler implements IGuiBackHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean done;

    private BattleBackHandler() {
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
            IGuiBackHandlerRegistry registry = ControlFlexApi.getGuiBackHandlerRegistry();
            if (registry == null) {
                LOGGER.info("[cfx-compat-cobblemon] back router unavailable (ControlFlex older than 0.12.0); "
                        + "Escape still minimises the battle");
                return;
            }
            // One handler per screen class: putIfAbsent semantics, so false means another mod
            // already owns BattleGUI's back key. Nothing to do about that, but say so.
            done = registry.register(BattleScreen.GUI_CLASS, new BattleBackHandler());
            LOGGER.info("[cfx-compat-cobblemon] back handler for {} registered={}",
                    BattleScreen.GUI_CLASS, done);
        } catch (Throwable t) {
            LOGGER.warn("[cfx-compat-cobblemon] could not register back handler: {}", t.toString());
        }
    }

    @Override
    public boolean onGuiBack() {
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
        LOGGER.info("[cfx-compat-cobblemon] B from {} -> {}",
                selection.getClass().getSimpleName(), switched ? "action menu" : "no handler");
        return switched;
    }
}
