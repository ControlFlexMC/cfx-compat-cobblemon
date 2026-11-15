package com.ifels.cfx.cobblemon;

/**
 * The battle request screen's actions.
 *
 * <p>Separate from {@link BattleAction} on purpose: these three only exist while
 * {@code BattleConfigureGUI} is open, their meaning ("next format") is not the battle screen's
 * ("next option"), and each one may reasonably be bound to a different button than its battle
 * counterpart. Sharing ids would also force one default per button across two screens.</p>
 *
 * @since 0.2.0
 */
public enum BattleRequestAction {

    /** Step to the previous format (single/double/triple/multi, as the server offered them). */
    PREV("battle_request_prev"),

    /** Step to the next format. */
    NEXT("battle_request_next"),

    /** Step to the previous level rule ("no restriction" / "set level" …). */
    LEVEL_PREV("battle_request_level_prev"),

    /** Step to the next level rule. */
    LEVEL_NEXT("battle_request_level_next"),

    /** Press the screen's primary button: challenge, accept, or invite. */
    CONFIRM("battle_request_confirm");

    private final String key;

    BattleRequestAction(String key) {
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
