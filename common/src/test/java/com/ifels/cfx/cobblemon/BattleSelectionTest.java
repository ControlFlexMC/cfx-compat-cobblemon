package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleSelectionTest {

    @Test
    void startsOnTheFirstTile() {
        BattleSelection s = new BattleSelection();
        assertEquals(0, s.sync(new Object(), 4));
    }

    @Test
    void nextWrapsForward() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 4);
        assertEquals(1, s.move(1, 4));
        assertEquals(2, s.move(1, 4));
        assertEquals(3, s.move(1, 4));
        assertEquals(0, s.move(1, 4), "wraps past the last tile");
    }

    @Test
    void prevWrapsBackward() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4);
        assertEquals(3, s.move(-1, 4), "wraps before the first tile");
    }

    @Test
    void aNewPageResetsTheHighlight() {
        BattleSelection s = new BattleSelection();
        Object first = new Object();
        s.sync(first, 4);
        s.move(1, 4);
        s.move(1, 4);

        assertEquals(0, s.sync(new Object(), 4), "different page object starts over");
    }

    @Test
    void theSamePageKeepsTheHighlight() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 4);
        s.move(1, 4);
        assertEquals(1, s.sync(page, 4));
    }

    @Test
    void aShorterListClampsBackIntoRange() {
        BattleSelection s = new BattleSelection();
        Object page = new Object();
        s.sync(page, 6);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        s.move(1, 6);
        assertEquals(5, s.index());
        assertEquals(3, s.sync(page, 4), "two moves left, so index clamps to 3");
    }

    @Test
    void anEmptyPageDoesNotThrow() {
        BattleSelection s = new BattleSelection();
        assertEquals(0, s.sync(new Object(), 0));
        assertEquals(0, s.move(1, 0));
    }

    @Test
    void directSelectionMovesTheHighlight() {
        BattleSelection s = new BattleSelection();
        s.sync(new Object(), 4);
        s.select(2);
        assertEquals(2, s.index());
    }
}
