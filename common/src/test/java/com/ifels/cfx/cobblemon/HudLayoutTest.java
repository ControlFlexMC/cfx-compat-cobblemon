package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final BattleScreen.PageKind ROOT = BattleScreen.PageKind.ROOT;
    private static final BattleScreen.PageKind MOVE = BattleScreen.PageKind.MOVE;

    private static BattleScreen.Slot slot(float x, float y, float w, float h, int row) {
        return slot(x, y, w, h, row, 0);
    }

    /** {@code labelWidth} = 0 means "unknown", which must always fall back to the outside rule. */
    private static BattleScreen.Slot slot(float x, float y, float w, float h, int row,
                                          int labelWidth) {
        return new BattleScreen.Slot(BattleAction.ROOT_FIGHT, x, y, w, h, row, labelWidth);
    }

    /** Anchor for a page holding exactly one badge. */
    private static HudLayout.Anchor single(BattleScreen.Slot slot, BattleScreen.PageKind kind,
                                           int badgeWidth) {
        return HudLayout.anchorsFor(List.of(slot), kind, List.of(badgeWidth)).get(0);
    }

    /** Anchors for a page whose badges all have the same width. */
    private static List<HudLayout.Anchor> page(List<BattleScreen.Slot> slots,
                                               BattleScreen.PageKind kind, int badgeWidth) {
        return HudLayout.anchorsFor(slots, kind,
                Collections.nCopies(slots.size(), badgeWidth));
    }

    private static boolean isOutside(HudLayout.Anchor a, float tileTop, float tileHeight) {
        return a.y() < tileTop || a.y() >= tileTop + tileHeight;
    }

    // ===== action menu: outside rule =====

    @Test
    void rootTopRowHangsAbove() {
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0), ROOT, HudLayout.GLYPH_SIZE);
        assertEquals(12, a.x(), "badge is flush with the tile's left edge");
        assertEquals(400 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
    }

    @Test
    void rootSecondRowHangsBelow() {
        HudLayout.Anchor a = single(slot(12f, 429f, 90f, 26f, 1), ROOT, HudLayout.GLYPH_SIZE);
        assertEquals(12, a.x());
        assertEquals(429 + 26 + HudLayout.GAP, a.y());
    }

    @Test
    void rootRowsAlternateSoConsecutiveBadgesDoNotShareAGap() {
        HudLayout.Anchor row1 = single(slot(12f, 429f, 90f, 26f, 1), ROOT, HudLayout.GLYPH_SIZE);
        HudLayout.Anchor row2 = single(slot(12f, 458f, 90f, 26f, 2), ROOT, HudLayout.GLYPH_SIZE);
        assertTrue(row2.y() + HudLayout.GLYPH_SIZE <= row1.y());
    }

    // ===== action menu: inside when the whole page fits =====

    @Test
    void rootBadgeSitsInsideAfterAShortLabel() {
        // "Fight" is ~26px; a single glyph therefore fits between label and artwork.
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 26), ROOT, HudLayout.GLYPH_SIZE);
        assertEquals(12 + HudLayout.ROOT_TEXT_INSET + 26 + HudLayout.ROOT_LABEL_GAP, a.x());
        assertEquals(400 + HudLayout.ROOT_LABEL_Y, a.y());
        assertTrue(a.x() + HudLayout.GLYPH_SIZE <= 12 + 90 - HudLayout.ROOT_ICON_RESERVE,
                "must stop before the artwork");
    }

    @Test
    void rootChordAlsoFitsOnAZhLabel() {
        // zh_cn "战斗" is ~18px, and even a two-glyph chord (26px) still fits.
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 18), ROOT, 26);
        assertEquals(12 + 6 + 18 + 4, a.x());
        assertTrue(a.x() + 26 <= 12 + 90 - HudLayout.ROOT_ICON_RESERVE);
    }

    @Test
    void rootBadgeFallsOutsideWhenTheLabelIsLong() {
        // de_de "Auswechseln" ~62px leaves no room for a chord.
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 62), ROOT, 26);
        assertEquals(12, a.x(), "outside: flush with the tile's left edge");
        assertEquals(400 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
    }

    @Test
    void rootBadgeFallsOutsideWhenTheLabelWidthIsUnknown() {
        HudLayout.Anchor a = single(slot(12f, 400f, 90f, 26f, 0, 0), ROOT, 26);
        assertEquals(400 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
    }

    // ===== action menu: placement is one decision for the page =====

    @Test
    void oneLongLabelSendsTheWholePageOutside() {
        // de_de "Auswechseln" sits next to "Fängen" — a half-inside row would look broken, so
        // the long one drags every badge out.
        List<BattleScreen.Slot> slots = List.of(
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
    void allShortLabelsKeepTheWholePageInside() {
        List<BattleScreen.Slot> slots = List.of(
                slot(12f, 400f, 90f, 26f, 0, 26),
                slot(105f, 400f, 90f, 26f, 0, 20),
                slot(12f, 429f, 90f, 26f, 1, 18),
                slot(105f, 429f, 90f, 26f, 1, 26));

        List<HudLayout.Anchor> anchors = page(slots, ROOT, 26);
        for (int i = 0; i < slots.size(); i++) {
            assertEquals(slots.get(i).y() + HudLayout.ROOT_LABEL_Y, anchors.get(i).y(),
                    "slot " + i + " should sit on its label's line");
        }
    }

    @Test
    void insideRootBadgesNeverReachTheArtwork() {
        int tileLeft = 12;
        int tileRight = tileLeft + 90;
        for (int label = 1; label <= 70; label++) {
            for (int badgeWidth : new int[]{10, 16, 26, 34}) {
                HudLayout.Anchor a = single(slot(tileLeft, 400f, 90f, 26f, 0, label), ROOT,
                        badgeWidth);
                if (isOutside(a, 400f, 26f)) {
                    continue;
                }
                assertTrue(a.x() >= tileLeft + HudLayout.ROOT_TEXT_INSET,
                        "label=" + label + " badge=" + badgeWidth + ": overlaps the text start");
                assertTrue(a.x() + badgeWidth <= tileRight - HudLayout.ROOT_ICON_RESERVE,
                        "label=" + label + " badge=" + badgeWidth + ": reaches the artwork");
            }
        }
    }

    // ===== move list =====

    @Test
    void moveBadgeSitsInsideTheTileBottomLeft() {
        // Move tiles are 92x24; the badge must stay within the tile's bounds.
        HudLayout.Anchor a = single(slot(20f, 500f, 92f, 24f, 0), MOVE, HudLayout.GLYPH_SIZE);
        assertEquals(20 + HudLayout.INSET, a.x());
        assertEquals(500 + 24 - HudLayout.GLYPH_SIZE - HudLayout.GAP, a.y());
        assertTrue(a.y() >= 500, "inside the tile vertically");
        assertTrue(a.y() + HudLayout.GLYPH_SIZE <= 500 + 24, "inside the tile vertically");
        assertTrue(a.x() >= 20, "inside the tile horizontally");
    }

    @Test
    void moveBadgeIgnoresRowParity() {
        // Second move row must not be pushed outside the tile just because its row is odd.
        HudLayout.Anchor a = single(slot(20f, 529f, 92f, 24f, 1), MOVE, HudLayout.GLYPH_SIZE);
        assertTrue(a.y() >= 529 && a.y() + HudLayout.GLYPH_SIZE <= 529 + 24);
    }

    @Test
    void moveBadgeStartsAfterTheTypeIcon() {
        // TypeIcon is drawn at x-9 and is 18 wide, so it covers the tile's first 9 pixels too.
        HudLayout.Anchor a = single(slot(20f, 500f, 92f, 24f, 0), MOVE, HudLayout.GLYPH_SIZE);
        assertTrue(a.x() >= 20 + HudLayout.MOVE_ICON_OVERHANG,
                "badge must clear the icon instead of sitting on it");
    }

    @Test
    void movePageIgnoresTheRootOnlyLabelRule() {
        // Move tiles have no labelWidth; the root rule must not leak into this page.
        HudLayout.Anchor a = single(slot(20f, 500f, 92f, 24f, 0, 0), MOVE, 26);
        assertEquals(20 + HudLayout.INSET, a.x());
    }

    // ===== forfeit confirmation =====

    private static final BattleScreen.PageKind CONFIRM = BattleScreen.PageKind.CONFIRM;

    /**
     * Confirm buttons carry no action of their own — their badge comes from the built-in confirm
     * press — which the layout code sees as a slot with a null action.
     */
    private static BattleScreen.Slot confirmButton(float x, float y) {
        return new BattleScreen.Slot(null, x, y, 34f, 19f, 0, 0);
    }

    @Test
    void confirmBadgeCentresUnderItsButton() {
        HudLayout.Anchor a = single(confirmButton(500f, 300f), CONFIRM, HudLayout.GLYPH_SIZE);
        assertEquals((int) (500f + (34f - HudLayout.GLYPH_SIZE) / 2f), a.x());
        assertEquals(300 + 19 + HudLayout.CONFIRM_BADGE_DROP, a.y());
    }

    @Test
    void confirmBadgesClearThePanelEdgeAndStaySymmetric() {
        // Buttons sit 8px above the panel's bottom edge; the drop must clear the edge, and equal
        // badge widths must centre both badges identically within their buttons.
        List<BattleScreen.Slot> buttons = List.of(
                confirmButton(500f, 300f), confirmButton(535f, 300f));
        List<HudLayout.Anchor> anchors = HudLayout.anchorsFor(buttons, CONFIRM,
                Collections.nCopies(2, HudLayout.GLYPH_SIZE));
        for (HudLayout.Anchor a : anchors) {
            assertTrue(a.y() >= 300 + 19 + 8, "badge must start below the panel edge");
        }
        assertEquals(anchors.get(0).x() - 500, anchors.get(1).x() - 535,
                "same badge width: identical offset within each button");
    }

    @Test
    void confirmHighlightIsExactlyTheButtonRect() {
        HudLayout.Rect r = HudLayout.highlightFor(confirmButton(500f, 300f), CONFIRM);
        assertEquals(500, r.x());
        assertEquals(300, r.y());
        assertEquals(34, r.width());
        assertEquals(19, r.height());
    }

    // ===== highlight ring =====

    @Test
    void moveHighlightIncludesTheIconThatSticksOutLeft() {
        HudLayout.Rect r = HudLayout.highlightFor(slot(20f, 500f, 92f, 24f, 0), MOVE);
        assertEquals(20 - HudLayout.MOVE_ICON_OVERHANG, r.x(), "ring reaches the icon's left edge");
        assertEquals(20 + 92, r.right(), "ring still ends on the tile's right edge");
        assertEquals(92 + HudLayout.MOVE_ICON_OVERHANG, r.width());
        assertEquals(500, r.y());
        assertEquals(24, r.height());
    }

    @Test
    void rootHighlightIsExactlyTheTileRect() {
        HudLayout.Rect r = HudLayout.highlightFor(slot(12f, 400f, 90f, 26f, 0), ROOT);
        assertEquals(12, r.x());
        assertEquals(400, r.y());
        assertEquals(90, r.width());
        assertEquals(26, r.height());
    }

    @Test
    void pageWithNoBadgesProducesNoAnchors() {
        assertTrue(HudLayout.anchorsFor(new ArrayList<>(), ROOT, new ArrayList<>()).isEmpty());
    }
}
