package com.ifels.cfx.cobblemon;

/**
 * The summary screen's actions.
 *
 * <p>Only the tab row is exposed: Cobblemon's summary already lets the cursor click its party
 * slots, moves and buttons, so the one thing a controller cannot do there is the shoulder-button
 * page turn every other tabbed screen offers. Keeping these separate from {@link BattleAction} and
 * {@link BattleRequestAction} means the same LB/RB can mean "next tab" here without inheriting the
 * battle screen's binding.</p>
 *
 * @since 0.10.0
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
