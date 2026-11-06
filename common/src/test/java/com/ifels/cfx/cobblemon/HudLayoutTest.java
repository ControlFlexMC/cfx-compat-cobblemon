package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final BattleScreen.PageKind ROOT = BattleScreen.PageKind.ROOT;
    private static final BattleScreen.PageKind MOVE = BattleScreen.PageKind.MOVE;

    private static BattleScreen.Slot slot(float x, float y, float w, float h, int row) {
        return new BattleScreen.Slot(BattleAction.ROOT_FIGHT, x, y, w, h, row);
    }

    @Test
    void rootTopRowHangsAbove() {
        HudLayout.Anchor a = HudLayout.anchorFor(slot(12f, 400f, 90f, 26f, 0), ROOT);
        assertEquals(12, a.x(), "badge is flush with the tile's left edge");
        assertEquals(400 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
    }

    @Test
    void rootSecondRowHangsBelow() {
        HudLayout.Anchor a = HudLayout.anchorFor(slot(12f, 429f, 90f, 26f, 1), ROOT);
        assertEquals(12, a.x());
        assertEquals(429 + 26 + HudLayout.GAP, a.y());
    }

    @Test
    void rootRowsAlternateSoConsecutiveBadgesDoNotShareAGap() {
        HudLayout.Anchor row1 = HudLayout.anchorFor(slot(12f, 429f, 90f, 26f, 1), ROOT);
        HudLayout.Anchor row2 = HudLayout.anchorFor(slot(12f, 458f, 90f, 26f, 2), ROOT);
        assertTrue(row2.y() + HudLayout.GLYPH_SIZE <= row1.y());
    }

    @Test
    void moveBadgeSitsInsideTheTileBottomLeft() {
        // Move tiles are 92x24; the badge must stay within the tile's bounds.
        HudLayout.Anchor a = HudLayout.anchorFor(slot(20f, 500f, 92f, 24f, 0), MOVE);
        assertEquals(20 + HudLayout.INSET, a.x());
        assertEquals(500 + 24 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
        assertTrue(a.y() >= 500, "inside the tile vertically");
        assertTrue(a.y() + HudLayout.GLYPH_SIZE <= 500 + 24, "inside the tile vertically");
        assertTrue(a.x() >= 20, "inside the tile horizontally");
    }

    @Test
    void moveBadgeIgnoresRowParity() {
        // Second move row must not be pushed outside the tile just because its row is odd.
        HudLayout.Anchor a = HudLayout.anchorFor(slot(20f, 529f, 92f, 24f, 1), MOVE);
        assertTrue(a.y() >= 529 && a.y() + HudLayout.GLYPH_SIZE <= 529 + 24);
    }
}
