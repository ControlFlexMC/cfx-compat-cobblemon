package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which widget is a summary tab, where the highlight currently sits, and how LB/RB steps it.
 *
 * <p>Cobblemon keeps the four tab pages as plain {@code Button}s and flags the open one with a
 * private {@code isActive} field — there is no public getter. Reading that field is what lets a
 * wrap from the last tab land on the first instead of always stepping out of tab 0, and makes a
 * tab clicked with the mouse agree with what the bumpers do next.</p>
 */
class SummaryTabsTest {

    @Test
    void onlySummaryTabWidgetsAreTabs() {
        assertTrue(SummaryScreen.isTab(new SummaryTab(false)));
        assertFalse(SummaryScreen.isTab(new Object()));
        assertFalse(SummaryScreen.isTab(new ExitButton()));
    }

    @Test
    void activeIndexReadsTheTabThatReportsItselfActive() {
        assertEquals(2, SummaryScreen.activeIndex(List.of(
                new SummaryTab(false), new SummaryTab(false), new SummaryTab(true))));
        assertEquals(3, SummaryScreen.activeIndex(List.of(
                new SummaryTab(false), new SummaryTab(false), new SummaryTab(false),
                new SummaryTab(true))));
    }

    @Test
    void activeIndexReportsNoTabWhenNothingReportsActive() {
        assertEquals(-1, SummaryScreen.activeIndex(
                List.of(new SummaryTab(false), new SummaryTab(false))),
                "callers must be able to tell 'tab 0 is open' from 'nothing readable'");
        assertEquals(-1, SummaryScreen.activeIndex(List.of()));
    }

    @Test
    void stepWrapsAtBothEnds() {
        assertEquals(1, SummaryScreen.stepIndex(0, 4, 1), "RB on the first tab opens the second");
        assertEquals(3, SummaryScreen.stepIndex(0, 4, -1), "LB on the first tab wraps to the last");
        assertEquals(0, SummaryScreen.stepIndex(3, 4, 1), "RB on the last tab wraps to the first");
        assertEquals(2, SummaryScreen.stepIndex(3, 4, -1));
    }

    @Test
    void stepHandlesMultiStepAndEmptyGroups() {
        assertEquals(1, SummaryScreen.stepIndex(3, 4, 2));
        assertEquals(0, SummaryScreen.stepIndex(0, 0, 1), "no tabs: stay on a safe index");
    }

    /** The class-name suffix is what {@link SummaryScreen#isTab} matches on. */
    public static final class SummaryTab {
        private boolean isActive;

        SummaryTab(boolean isActive) {
            this.isActive = isActive;
        }
    }

    public static final class ExitButton {
    }
}
