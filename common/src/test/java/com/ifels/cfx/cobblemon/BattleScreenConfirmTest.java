package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The forfeit confirmation's slot contract, tested through the public {@code slots()} path.
 *
 * <p>The fake page is named {@code ForfeitConfirmationSelection} on purpose: the bridge matches
 * sub-pages by class-name suffix, so that name is what routes it to the confirm branch.</p>
 */
class BattleScreenConfirmTest {

    @Test
    void confirmPageIsRecognisedAsSuch() {
        assertEquals(BattleScreen.PageKind.CONFIRM,
                BattleScreen.kindOf(new ForfeitConfirmationSelection()));
    }

    @Test
    void slotsFollowVisualOrderSoIndexZeroIsTheLeftButton() {
        List<BattleScreen.Slot> slots = BattleScreen.slots(new ForfeitConfirmationSelection());

        assertEquals(2, slots.size());
        assertEquals(122f, slots.get(0).x(), "index 0 must be the accept button, drawn on the left");
        assertEquals(157f, slots.get(1).x(), "index 1 is decline, on the right");
        assertNull(slots.get(0).action(), "neither button carries a bridge action of its own");
        assertNull(slots.get(1).action());
        assertEquals(34f, slots.get(0).width());
        assertEquals(19f, slots.get(0).height());
        assertEquals("(confirm button)", slots.get(0).displayName(),
                "log name must not dereference the missing action");
    }

    /** Only the class-name suffix and the two getters are used; the rest can be minimal. */
    static final class ForfeitConfirmationSelection {
        private final Button accept = new Button(122, 218);
        private final Button decline = new Button(157, 218);

        public Button getAcceptButton() {
            return accept;
        }

        public Button getDeclineButton() {
            return decline;
        }
    }

    /** Stands in for Cobblemon's {@code BattleResponseButton} (a vanilla Button, 34×19). */
    static final class Button {
        private final int x;
        private final int y;

        Button(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return 34;
        }

        public int getHeight() {
            return 19;
        }
    }
}
