package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSlotsTest {

    private static final String FIGHT = "textures/gui/battle/battle_menu_fight.png";
    private static final String SWITCH = "textures/gui/battle/battle_menu_switch.png";
    private static final String BAG = "textures/gui/battle/battle_menu_bag.png";
    private static final String RUN = "textures/gui/battle/battle_menu_run.png";

    @Test
    void rootTilesAreIdentifiedByIconNotPosition() {
        assertEquals(BattleAction.ROOT_FIGHT, BattleSlots.rootActionFor(FIGHT));
        assertEquals(BattleAction.ROOT_SWITCH, BattleSlots.rootActionFor(SWITCH));
        assertEquals(BattleAction.ROOT_CAPTURE, BattleSlots.rootActionFor(BAG));
        assertEquals(BattleAction.ROOT_RUN, BattleSlots.rootActionFor(RUN));
        assertEquals(BattleAction.ROOT_RUN,
                BattleSlots.rootActionFor("textures/gui/battle/battle_menu_forfeit.png"));
        assertNull(BattleSlots.rootActionFor("textures/gui/battle/battle_menu_unknown.png"));
        assertNull(BattleSlots.rootActionFor(null));
    }

    @Test
    void trappedMenuKeepsSemanticMeaningsWhenPositionsShift() {
        // Cobblemon drops the switch tile when trapped; the remaining tiles shift left.
        List<String> tiles = List.of(FIGHT, BAG, RUN);
        assertEquals(List.of(BattleAction.ROOT_FIGHT, BattleAction.ROOT_CAPTURE,
                        BattleAction.ROOT_RUN),
                tiles.stream().map(BattleSlots::rootActionFor).toList());
    }

    @Test
    void disabledMoveKeepsItsOriginalNumber() {
        assertEquals(BattleAction.MOVE_1, BattleSlots.moveAction(0));
        assertEquals(BattleAction.MOVE_2, BattleSlots.moveAction(1));
        assertEquals(BattleAction.MOVE_3, BattleSlots.moveAction(2));
        assertEquals(BattleAction.MOVE_4, BattleSlots.moveAction(3));
    }

    @Test
    void partySlotsAreNumberedInOrder() {
        assertEquals(BattleAction.PARTY_1, BattleSlots.partyAction(0));
        assertEquals(BattleAction.PARTY_6, BattleSlots.partyAction(5));
    }

    @Test
    void backIsAvailableOnEverySubPageButNotTheMenu() {
        assertFalse(BattleSlots.canGoBack(BattleSlots.PageKind.ROOT));
        assertFalse(BattleSlots.canGoBack(BattleSlots.PageKind.OTHER));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.MOVE));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.SWITCH));
        assertTrue(BattleSlots.canGoBack(BattleSlots.PageKind.CONFIRM));
    }

    @Test
    void backArrowIsDrawnWhereCobblemonDrawsItsBackButton() {
        assertFalse(BattleSlots.backArrowShown(BattleSlots.PageKind.ROOT));
        assertFalse(BattleSlots.backArrowShown(BattleSlots.PageKind.OTHER));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.MOVE));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.SWITCH));
        assertTrue(BattleSlots.backArrowShown(BattleSlots.PageKind.CONFIRM));
    }

    @Test
    void confirmSlotHasNoActionOfItsOwn() {
        BattleSlots.Slot slot = new BattleSlots.Slot(null, 115f, 150f, 90f, 26f, 0, 0);
        assertEquals("(confirm button)", slot.displayName());
        assertEquals(160.0, slot.centerX());
        assertEquals(163.0, slot.centerY());
    }

    @Test
    void emptyPageHasNoIdAndNoSlots() {
        BattleSlots.Page page = BattleSlots.Page.empty();
        assertNull(page.id());
        assertEquals(BattleSlots.PageKind.OTHER, page.kind());
        assertTrue(page.slots().isEmpty());
    }

    @Test
    void slotCentreIsTheClickPoint() {
        BattleSlots.Slot slot = new BattleSlots.Slot(BattleAction.MOVE_2, 105f, 500f, 92f, 24f, 0, 0);
        assertEquals(151.0, slot.centerX());
        assertEquals(512.0, slot.centerY());
        assertEquals(BattleAction.MOVE_2.id(), slot.displayName());
    }
}
