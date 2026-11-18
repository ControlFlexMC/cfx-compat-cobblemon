package com.ifels.cfx.cobblemon;

import java.util.List;

/**
 * Pure page model for Cobblemon 1.5.2's battle screen.
 *
 * <p>No Minecraft or Cobblemon types: the typed adapter in {@link BattleScreen} builds these
 * records, and the HUD, selection state, and tests consume them.</p>
 */
public final class BattleSlots {

    public static final String BATTLE_GUI_CLASS =
            "com.cobblemon.mod.common.client.gui.battle.BattleGUI";

    public enum PageKind {
        ROOT, MOVE, SWITCH, CONFIRM, OTHER
    }

    public record Slot(BattleAction action, float x, float y, float width, float height,
                       int row, int labelWidth) {
        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }

        public String displayName() {
            return action == null ? "(confirm button)" : action.id();
        }
    }

    public record Page(Object id, PageKind kind, List<Slot> slots) {
        public Page {
            slots = List.copyOf(slots);
        }

        public static Page empty() {
            return new Page(null, PageKind.OTHER, List.of());
        }
    }

    private BattleSlots() {
    }

    /** Root tiles are identified by icon texture: a trapped party removes the switch tile. */
    static BattleAction rootActionFor(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.endsWith("battle_menu_fight.png")) {
            return BattleAction.ROOT_FIGHT;
        }
        if (resourcePath.endsWith("battle_menu_switch.png")) {
            return BattleAction.ROOT_SWITCH;
        }
        if (resourcePath.endsWith("battle_menu_bag.png")) {
            return BattleAction.ROOT_CAPTURE;
        }
        if (resourcePath.endsWith("battle_menu_run.png")
                || resourcePath.endsWith("battle_menu_forfeit.png")) {
            return BattleAction.ROOT_RUN;
        }
        return null;
    }

    static BattleAction moveAction(int index) {
        return BattleAction.move(index + 1);
    }

    static BattleAction partyAction(int index) {
        return BattleAction.party(index + 1);
    }

    static boolean canGoBack(PageKind kind) {
        return kind == PageKind.MOVE || kind == PageKind.SWITCH || kind == PageKind.CONFIRM;
    }

    /** 1.5.2 draws its back button on all three sub-pages and never on the action menu. */
    static boolean backArrowShown(PageKind kind) {
        return canGoBack(kind);
    }
}
