package com.ifels.cfx.cobblemon;

/**
 * The summary screen's actions.
 *
 * <p>Only the tab row is exposed: the summary's party slots, moves and buttons are already
 * pointer-clickable, and the shoulder buttons are the one control with no mouse equivalent there.
 * Separate from {@link BattleAction} so the same LB/RB can mean "next tab" without inheriting the
 * battle screen's binding.</p>
 *
 * @since 0.9.1
 */
public enum SummaryAction {

    /** Step to the previous tab (wrap-around at the first). */
    PREV("summary_tab_prev"),

    /** Step to the next tab (wrap-around at the last). */
    NEXT("summary_tab_next");

    private final String key;

    SummaryAction(String key) {
        this.key = key;
    }

    /** Translation key, also used as the KeyMapping-style name. */
    public String id() {
        return "key." + BattleAction.MOD_ID + "." + key;
    }

    /** Stable ControlFlex action id, written into the user's binding file. */
    public String actionId() {
        return BattleAction.MOD_ID + ":" + key;
    }
}
