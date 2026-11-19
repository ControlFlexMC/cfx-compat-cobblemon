package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.HudStateReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ControlFlex contract for the summary hints: the handler must claim the HUD (otherwise the
 * built-in {@code hud_toggle} is never routed here), apply the pushed visibility both ways, and
 * leave Escape alone so the summary still closes.
 */
class SummaryScreenHandlerTest {

    @Test
    void ownsTheHudAndAppliesThePushedVisibilityBothWays() {
        SummaryScreenHandler handler = new SummaryScreenHandler();

        assertTrue(handler.ownsHud(), "without this ControlFlex does not route hud_toggle here");
        assertFalse(handler.onBack(), "Escape must still close the summary");

        try {
            assertTrue(handler.onHudState(false, HudStateReason.USER_TOGGLE));
            assertFalse(SummaryHud.isVisible(), "the toggle must actually hide the hints");
            assertTrue(handler.onHudState(true, HudStateReason.RESTORED));
            assertTrue(SummaryHud.isVisible(), "restoring the stored state must show them again");
        } finally {
            SummaryHud.setVisible(true);
        }
    }
}
