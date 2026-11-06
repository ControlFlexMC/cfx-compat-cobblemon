package com.ifels.cfx.cobblemon;

/**
 * The battle-screen actions this bridge exposes to ControlFlex.
 *
 * <p>Each constant becomes a pseudo {@code KeyMapping}. ControlFlex's compat config moves
 * them into the GUI layer, so they only fire while a screen is open; the ticker additionally
 * ignores everything unless the open screen is Cobblemon's {@code BattleGUI}.</p>
 *
 * <p>Names are semantic, not positional: {@link #ROOT_SWITCH} means "open the party page",
 * never "the tile in the top-right corner". Cobblemon rebuilds the root menu with three tiles
 * when the active Pokémon is trapped, which shifts the capture/run tiles left — a positional
 * mapping would press capture where the player expected the party page.</p>
 */
public enum BattleAction {

    ROOT_FIGHT("battle_fight"),
    ROOT_SWITCH("battle_switch"),
    ROOT_CAPTURE("battle_capture"),
    /** Run in a wild battle, forfeit in a trainer/PvP battle — same tile role. */
    ROOT_RUN("battle_run"),

    MOVE_1("battle_move_1"),
    MOVE_2("battle_move_2"),
    MOVE_3("battle_move_3"),
    MOVE_4("battle_move_4"),

    PARTY_1("battle_party_1"),
    PARTY_2("battle_party_2"),
    PARTY_3("battle_party_3"),
    PARTY_4("battle_party_4"),
    PARTY_5("battle_party_5"),
    PARTY_6("battle_party_6"),

    /**
     * Move the highlight to the previous / next tile on the current sub-page, wrapping at both
     * ends, and warp the virtual cursor onto that tile — so the built-in {@code gui_press} (A)
     * activates it without a dedicated confirm action.
     */
    PREV("battle_prev"),
    NEXT("battle_next"),

    LOG_UP("battle_log_up"),
    LOG_DOWN("battle_log_down"),

    /**
     * Show or hide the key hint overlay.
     *
     * <p>Not a tile action: it is handled before the battle-screen gate, so it also works from
     * any other screen (the overlay state carries into the next battle).</p>
     */
    HUD_TOGGLE("battle_hud_toggle");

    public static final String MOD_ID = "cfx_compat_cobblemon";
    public static final String CATEGORY = "category.cfx_compat_cobblemon.battle";

    private final String key;

    BattleAction(String key) {
        this.key = key;
    }

    /** Translation key, also used as the {@code KeyMapping} name and in the compat JSON. */
    public String id() {
        return "key." + MOD_ID + "." + key;
    }

    /**
     * Ids this action may be known by to ControlFlex, most specific first.
     *
     * <p>Which one wins depends on startup order: ControlFlex scans key mappings before the
     * bridge's compat config is installed, so the action is first registered under the bridge
     * mod id and later <em>re-attributed</em> to the compat config's {@code mod_id}
     * ({@code cobblemon}). Both spellings exist in the log of a normal launch, so lookups try
     * each in turn.</p>
     */
    public String[] candidateIds() {
        return new String[]{"cobblemon:" + id(), MOD_ID + ":" + id(), id()};
    }

    public static BattleAction move(int slot) {
        return switch (slot) {
            case 1 -> MOVE_1;
            case 2 -> MOVE_2;
            case 3 -> MOVE_3;
            default -> MOVE_4;
        };
    }

    public static BattleAction party(int slot) {
        return switch (slot) {
            case 1 -> PARTY_1;
            case 2 -> PARTY_2;
            case 3 -> PARTY_3;
            case 4 -> PARTY_4;
            case 5 -> PARTY_5;
            default -> PARTY_6;
        };
    }
}
