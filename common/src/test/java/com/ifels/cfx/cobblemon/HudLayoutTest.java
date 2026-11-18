package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final BattleSlots.PageKind ROOT = BattleSlots.PageKind.ROOT;
    private static final BattleSlots.PageKind MOVE = BattleSlots.PageKind.MOVE;
    private static final BattleSlots.PageKind SWITCH = BattleSlots.PageKind.SWITCH;
    private static final BattleSlots.PageKind CONFIRM = BattleSlots.PageKind.CONFIRM;

    private static BattleSlots.Slot slot(float x, float y, float w, float h, int row) {
        return slot(x, y, w, h, row, 0);
    }

    private static BattleSlots.Slot slot(float x, float y, float w, float h, int row,
                                         int labelWidth) {
        return new BattleSlots.Slot(BattleAction.ROOT_FIGHT, x, y, w, h, row, labelWidth);
    }

    private static HudLayout.Anchor single(BattleSlots.Slot slot, BattleSlots.PageKind kind,
                                           int badgeWidth) {
        return HudLayout.anchorsFor(List.of(slot), kind, List.of(badgeWidth)).get(0);
    }

    private static List<HudLayout.Anchor> page(List<BattleSlots.Slot> slots,
                                               BattleSlots.PageKind kind, int badgeWidth) {
        return HudLayout.anchorsFor(slots, kind, Collections.nCopies(slots.size(), badgeWidth));
    }

    private static boolean isOutside(HudLayout.Anchor a, float top, float height) {
        return a.y() < top || a.y() >= top + height;
    }

    @Test
    void rootBadgeSitsInsideAfterAShortLabel() {
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 26), ROOT, 10);
        assertEquals(12 + HudLayout.ROOT_TEXT_INSET + 26 + HudLayout.ROOT_LABEL_GAP, a.x());
        assertEquals(400 + HudLayout.ROOT_LABEL_Y, a.y());
        assertTrue(a.x() + 10 <= 12 + 90 - HudLayout.ROOT_ICON_RESERVE);
    }

    @Test
    void oneLongLabelSendsTheWholePageOutside() {
        List<BattleSlots.Slot> slots = List.of(
                slot(12f, 400f, 90f, 26f, 0, 26),
                slot(105f, 400f, 90f, 26f, 0, 62),
                slot(12f, 429f, 90f, 26f, 1, 18),
                slot(105f, 429f, 90f, 26f, 1, 26));
        List<HudLayout.Anchor> anchors = page(slots, ROOT, 26);
        for (int i = 0; i < slots.size(); i++) {
            assertTrue(isOutside(anchors.get(i), slots.get(i).y(), slots.get(i).height()),
                    "slot " + i + " must fall back with the rest of the page");
        }
    }

    @Test
    void rootRowsAlternateForTheOutsideRule() {
        assertEquals(400 - 10 - 2, single(slot(12f, 400f, 90f, 26f, 0), ROOT, 10).y());
        assertEquals(429 + 26 + 2, single(slot(12f, 429f, 90f, 26f, 1), ROOT, 10).y());
    }

    @Test
    void moveBadgeSitsInsideTheBottomLeft() {
        HudLayout.Anchor a = single(slot(20f, 500f, 92f, 24f, 0), MOVE, 10);
        assertEquals(20 + HudLayout.INSET, a.x());
        assertEquals(500 + 24 - 10 - 2, a.y());
        assertTrue(a.x() >= 20 + HudLayout.MOVE_ICON_OVERHANG);
        assertTrue(a.y() + 10 <= 524);
    }

    @Test
    void switchBadgeSitsInsideTheBottomRight() {
        HudLayout.Anchor a = single(slot(12f, 100f, 131f, 40f, 0), SWITCH, 10);
        assertEquals(12 + 131 - 10 - HudLayout.SWITCH_INSET, a.x());
        assertEquals(100 + 40 - 10 - HudLayout.SWITCH_INSET, a.y());
        assertTrue(a.x() + 10 <= 12 + 131);
        assertTrue(a.y() + 10 <= 140);
    }

    @Test
    void confirmBadgeCentresUnderTheSingleButton() {
        BattleSlots.Slot button = new BattleSlots.Slot(null, 115f, 150f, 90f, 26f, 0, 0);
        HudLayout.Anchor a = single(button, CONFIRM, 10);
        assertEquals(115 + (90 - 10) / 2, a.x());
        assertEquals(150 + 26 + HudLayout.CONFIRM_BADGE_DROP, a.y());
    }

    @Test
    void moveHighlightIncludesTheIconThatSticksOutLeft() {
        HudLayout.Rect r = HudLayout.highlightFor(slot(20f, 500f, 92f, 24f, 0), MOVE);
        assertEquals(20 - HudLayout.MOVE_ICON_OVERHANG, r.x());
        assertEquals(20 + 92, r.right());
        assertEquals(92 + HudLayout.MOVE_ICON_OVERHANG, r.width());
    }

    @Test
    void rootAndSwitchHighlightsAreExactlyTheTileRect() {
        HudLayout.Rect root = HudLayout.highlightFor(slot(12f, 400f, 90f, 26f, 0), ROOT);
        assertEquals(12, root.x());
        assertEquals(90, root.width());
        HudLayout.Rect swap = HudLayout.highlightFor(slot(12f, 100f, 131f, 40f, 0), SWITCH);
        assertEquals(131, swap.width());
        assertEquals(40, swap.height());
    }
}
