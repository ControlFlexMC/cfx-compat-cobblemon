package com.ifels.cfx.cobblemon;

/**
 * Pure tab model for Cobblemon 1.5.2's summary screen.
 *
 * <p>1.5.2 has three tab pages (Info / Moves / Stats). No Minecraft or Cobblemon types live here:
 * the typed adapter in {@link SummaryScreen} finds the widgets, and the tests exercise the stepping
 * rule without the game on the classpath.</p>
 *
 * @since 0.9.1
 */
public final class SummaryTabs {

    public static final String GUI_CLASS =
            "com.cobblemon.mod.common.client.gui.summary.Summary";

    private SummaryTabs() {
    }

    /** Where a step of {@code delta} lands, wrapping at both ends. */
    static int stepIndex(int active, int count, int delta) {
        if (count <= 0) {
            return 0;
        }
        return Math.floorMod(active + delta, count);
    }
}
