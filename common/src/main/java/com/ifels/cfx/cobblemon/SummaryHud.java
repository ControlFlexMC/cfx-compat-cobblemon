package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionBindingInfo;
import com.ifels.controlflex.api.IActionBindingProvider;
import com.ifels.controlflex.api.IControllerGlyph;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Key hints for the summary screen: the toggle in the bottom-left and the tab row's LB/RB in the
 * bottom-right, both on the same baseline and margins as the battle screen's corner hints.
 *
 * <p>Only the tab row is hinted. Everything else on the summary — party slots, moves, the evolve
 * button — is a normal clickable widget the virtual cursor can already reach, so a badge per
 * widget would advertise buttons the player does not need to learn. The two corners are the
 * controls that have no pointer equivalent.</p>
 *
 * <p>Bindings come from ControlFlex's {@code IActionBindingProvider}, so a rebind or a controller
 * swap shows up on the next frame — the same contract {@link BattleHud} works under.</p>
 *
 * @since 0.9.1
 */
public final class SummaryHud {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    private static boolean visible = true;

    private SummaryHud() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        if (!SummaryScreen.isSummaryScreen(screen)) {
            return;
        }
        IActionBindingProvider provider = provider();
        if (provider == null) {
            return;
        }
        // Drawn before the gate on purpose: it is the way back when the hints are off.
        BattleHud.drawToggleHint(graphics, screen, provider, visible);
        if (!visible) {
            return;
        }
        drawTabHint(graphics, screen, provider);
    }

    /** Same contract as {@link BattleHud#setVisible}: ControlFlex owns the toggle, we only draw. */
    public static void setVisible(boolean shown) {
        if (visible == shown) {
            return;
        }
        visible = shown;
        LOGGER.info("[cfx-compat-cobblemon] summary key hints {}", visible ? "shown" : "hidden");
    }

    public static boolean isVisible() {
        return visible;
    }

    /**
     * The LB/RB row in the bottom-right corner, labelled with what it switches.
     *
     * <p>One hint for the whole tab row rather than a glyph on each tab: the two buttons act on
     * the group, and repeating them on all three tabs would read as three separate bindings.</p>
     */
    private static void drawTabHint(GuiGraphics graphics, Screen screen,
                                    IActionBindingProvider provider) {
        if (SummaryScreen.tabs(screen).size() < 2) {
            return;
        }
        List<IControllerGlyph> prev = glyphsOf(provider, SummaryAction.PREV.actionId());
        List<IControllerGlyph> next = glyphsOf(provider, SummaryAction.NEXT.actionId());
        String label = Component.translatable("cfx_compat_cobblemon.hud.tab_switch").getString();
        BattleHud.drawPrevNextHint(graphics, screen, prev, next, label);
    }

    private static List<IControllerGlyph> glyphsOf(IActionBindingProvider provider, String actionId) {
        IActionBindingInfo info = provider.getActiveBinding(actionId);
        if (info == null || !info.isBound()) {
            return List.of();
        }
        return info.getGlyphs();
    }

    private static IActionBindingProvider provider() {
        try {
            return ControlFlexApi.getActionBindingProvider();
        } catch (Throwable t) {
            return null;
        }
    }
}
