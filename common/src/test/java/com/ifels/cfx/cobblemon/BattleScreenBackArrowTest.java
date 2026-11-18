package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When Cobblemon draws its own back arrow in the bottom-left corner.
 *
 * <p>The arrow is a plain field on each sub-page, not a widget, and it is not always drawn even
 * though the field exists: the action menu only shows it while the player can take back the choice
 * they just made, and a forced switch has no way back. The toggle hint shares that corner, so it
 * has to follow what is actually on screen, not the field's presence.</p>
 */
class BattleScreenBackArrowTest {

    @Test
    void actionMenuShowsTheArrowOnlyWhileAChoiceCanBeTakenBack() {
        assertFalse(BattleScreen.backArrowShown(new BattleGeneralActionSelection(null)),
                "nothing to undo: the action menu has no back arrow");
        assertTrue(BattleScreen.backArrowShown(new BattleGeneralActionSelection(new Object())),
                "an answered request can be taken back, and the arrow is drawn then");
    }

    @Test
    void partyPageHidesTheArrowForAForcedSwitch() {
        assertTrue(BattleScreen.backArrowShown(new BattleSwitchPokemonSelection(request(false))),
                "a normal switch can be cancelled, so the arrow is drawn");
        assertFalse(BattleScreen.backArrowShown(new BattleSwitchPokemonSelection(request(true))),
                "a forced switch has no way back and draws no arrow");
    }

    @Test
    void movePageAlwaysShowsTheArrow() {
        assertTrue(BattleScreen.backArrowShown(new BattleMoveSelection()));
    }

    @Test
    void noSelectionMeansNoArrow() {
        assertFalse(BattleScreen.backArrowShown(null));
    }

    private static Object request(boolean forceSwitch) {
        return new ForcedSwitch(forceSwitch);
    }

    /** Same getter name as Cobblemon's {@code SingleActionRequest}. */
    public static final class ForcedSwitch {
        private final boolean forceSwitch;

        ForcedSwitch(boolean forceSwitch) {
            this.forceSwitch = forceSwitch;
        }

        public boolean getForceSwitch() {
            return forceSwitch;
        }
    }

    /** The class-name suffix is what {@link BattleScreen#kindOf} matches on. */
    public static final class BattleGeneralActionSelection {
        private final Object lastAnwseredRequest;

        BattleGeneralActionSelection(Object lastAnwseredRequest) {
            this.lastAnwseredRequest = lastAnwseredRequest;
        }

        public Object getLastAnwseredRequest() {
            return lastAnwseredRequest;
        }
    }

    public static final class BattleSwitchPokemonSelection {
        private final Object request;

        BattleSwitchPokemonSelection(Object request) {
            this.request = request;
        }

        public Object getRequest() {
            return request;
        }
    }

    public static final class BattleMoveSelection {
    }
}
