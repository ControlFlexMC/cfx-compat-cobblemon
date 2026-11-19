package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tab stepping for the 1.5.2 summary: three pages (Info / Moves / Stats), wrapping at both ends.
 *
 * <p>Pure arithmetic — no Minecraft or Cobblemon types — so both loaders' test suites can run it
 * without the game on the classpath.</p>
 */
class SummaryTabsTest {

    @Test
    void stepWrapsForwardAtTheLastTab() {
        assertEquals(1, SummaryTabs.stepIndex(0, 3, 1));
        assertEquals(2, SummaryTabs.stepIndex(1, 3, 1));
        assertEquals(0, SummaryTabs.stepIndex(2, 3, 1));
    }

    @Test
    void stepWrapsBackwardAtTheFirstTab() {
        assertEquals(0, SummaryTabs.stepIndex(1, 3, -1));
        assertEquals(2, SummaryTabs.stepIndex(0, 3, -1));
    }

    @Test
    void stepHandlesMultiStepAndEmptyGroups() {
        assertEquals(1, SummaryTabs.stepIndex(2, 3, 2));
        assertEquals(0, SummaryTabs.stepIndex(0, 0, 1));
    }

    @Test
    void summaryClassConstantMatchesTheCobblemonScreen() {
        assertEquals("com.cobblemon.mod.common.client.gui.summary.Summary",
                SummaryTabs.GUI_CLASS);
    }
}
