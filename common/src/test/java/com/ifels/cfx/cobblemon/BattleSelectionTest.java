package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleSelectionTest {

    @Test
    void startsOnTheFirstTile() {
        BattleSelection s = new BattleSelection();
        assertEquals(0, s.sync(new Object(), 4, false));
    }

    @Test
    void nextWrapsForward() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 4, false);
        assertEquals(1, s.move(1, 4));
        assertEquals(2, s.move(1, 4));
        assertEquals(3, s.move(1, 4));
        assertEquals(0, s.move(1, 4), "wraps past the last tile");
    }

    @Test
    void prevWrapsBackward() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4, false);
        assertEquals(3, s.move(-1, 4), "wraps before the first tile");
    }

    @Test
    void aNewPageResetsTheHighlight() {
        BattleSelection s = new BattleSelection();
        Object first = new Object();
        s.sync(first, 4, false);
        s.move(1, 4);
        s.move(1, 4);

        assertEquals(0, s.sync(new Object(), 4, false), "different page object starts over");
    }

    @Test
    void theSamePageKeepsTheHighlight() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 4, false);
        s.move(1, 4);
        assertEquals(1, s.sync(page, 4, false));
    }

    @Test
    void aShorterListClampsBackIntoRange() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 6, false);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        assertEquals(5, s.index());
        assertEquals(3, s.sync(page, 4, false), "two moves left, so index clamps to 3");
    }

    @Test
    void anEmptyPageDoesNotThrow() {
        BattleSelection s = new BattleSelection();
        assertEquals(0, s.sync(new Object(), 0, false));
        assertEquals(0, s.move(1, 0));
    }

    @Test
    void directSelectionMovesTheHighlight() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4, false);
        s.select(2);
        assertEquals(2, s.index());
    }

    // ===== returning to the action menu =====

    @Test
    void steppingBackRestoresTheTileThatOpenedTheSubPage() {
        BattleSelection s = new BattleSelection();
        Object menu = new Object();
        s.sync(menu, 4, true);
        s.move(1, 4); // 宝可梦

        Object party = new Object();
        assertEquals(0, s.sync(party, 6, false), "a sub-page still starts on its first tile");

        s.markReturnToMenu();
        Object rebuiltMenu = new Object(); // Cobblemon builds a new instance on the way back
        assertEquals(1, s.sync(rebuiltMenu, 4, true),
                "back must land on the tile the sub-page was opened from");
    }

    @Test
    void aMenuRebuiltForANewTurnStillStartsOver() {
        BattleSelection s = new BattleSelection();
        Object menu = new Object();
        s.sync(menu, 4, true);
        s.move(1, 4);

        Object nextTurn = new Object(); // no back press: the turn advanced
        assertEquals(0, s.sync(nextTurn, 4, true),
                "a new turn is not a return, so it keeps the old reset behaviour");
    }

    @Test
    void movingOnTheMenuUpdatesWhereTheNextReturnLands() {
        BattleSelection s = new BattleSelection();
        Object menu = new Object();
        s.sync(menu, 4, true);
        s.move(1, 4); // 宝可梦
        assertEquals(2, s.move(1, 4), "抓到");

        s.markReturnToMenu();
        assertEquals(2, s.sync(new Object(), 4, true), "return lands on the latest menu position");
    }

    @Test
    void aPerTileBindingOnTheMenuIsAlsoRemembered() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4, true);
        s.select(1); // direct binding for 宝可梦, no LB/RB involved

        s.sync(new Object(), 6, false);
        s.markReturnToMenu();
        assertEquals(1, s.sync(new Object(), 4, true));
    }

    @Test
    void theMarkSurvivesAnotherSyncOfTheSamePage() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4, true);
        s.move(1, 4);

        Object party = new Object();
        s.sync(party, 6, false);

        // The back call is queued; a tick or two may still see the old page before the menu
        // replaces it. Those syncs must not consume the mark.
        s.markReturnToMenu();
        s.sync(party, 6, false);

        assertEquals(1, s.sync(new Object(), 4, true));
    }

    @Test
    void resetForgetsTheMenuPosition() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4, true);
        s.move(1, 4);

        s.reset();
        s.markReturnToMenu(); // a stale mark must not resurrect an old tile either

        assertEquals(0, s.sync(new Object(), 4, true));
    }
}
