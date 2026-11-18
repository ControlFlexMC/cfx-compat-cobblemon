package com.ifels.cfx.cobblemon;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure placement rules for the HUD badges and the selection ring.
 *
 * <p>Placement is per page: the 90x26 action tiles carry a label and artwork, the 92x24 move
 * bars leave the bottom-left corner free, the 131x40 party tiles leave the bottom band free, and
 * the single confirmation button has room underneath.</p>
 */
public final class HudLayout {

    public static final int GLYPH_SIZE = 10;
    public static final int GAP = 2;

    /** Move tiles draw their type icon at x-9; the ring has to reach its left edge. */
    public static final int MOVE_ICON_OVERHANG = 9;
    public static final int INSET = 12;

    public static final int ROOT_TEXT_INSET = 6;
    public static final int ROOT_LABEL_Y = 8;
    public static final int ROOT_ICON_RESERVE = 20;
    public static final int ROOT_LABEL_GAP = 4;

    public static final int CONFIRM_BADGE_DROP = 10;
    public static final int SWITCH_INSET = 2;

    private HudLayout() {
    }

    public record Anchor(int x, int y) {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    public static List<Anchor> anchorsFor(List<BattleSlots.Slot> slots,
                                          BattleSlots.PageKind kind,
                                          List<Integer> badgeWidths) {
        List<Anchor> anchors = new ArrayList<>(slots.size());
        boolean inside = kind == BattleSlots.PageKind.ROOT && allRootBadgesFit(slots, badgeWidths);
        for (int i = 0; i < slots.size(); i++) {
            anchors.add(anchorFor(slots.get(i), kind, width(badgeWidths, i), inside));
        }
        return anchors;
    }

    private static boolean allRootBadgesFit(List<BattleSlots.Slot> slots,
                                            List<Integer> badgeWidths) {
        for (int i = 0; i < slots.size(); i++) {
            if (!rootFits(slots.get(i), width(badgeWidths, i))) {
                return false;
            }
        }
        return true;
    }

    private static int width(List<Integer> badgeWidths, int index) {
        return index < badgeWidths.size() ? badgeWidths.get(index) : GLYPH_SIZE;
    }

    private static Anchor anchorFor(BattleSlots.Slot slot, BattleSlots.PageKind kind,
                                    int badgeWidth, boolean rootInsideAllowed) {
        if (kind == BattleSlots.PageKind.MOVE) {
            int x = (int) slot.x() + INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - GAP;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.SWITCH) {
            int x = (int) (slot.x() + slot.width()) - badgeWidth - SWITCH_INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - SWITCH_INSET;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.CONFIRM) {
            int x = (int) slot.x() + ((int) slot.width() - badgeWidth) / 2;
            int y = (int) (slot.y() + slot.height()) + CONFIRM_BADGE_DROP;
            return new Anchor(x, y);
        }
        if (kind == BattleSlots.PageKind.ROOT && rootInsideAllowed) {
            return new Anchor(rootInsideX(slot), (int) slot.y() + ROOT_LABEL_Y);
        }
        int x = (int) slot.x();
        int y = slot.row() % 2 == 0
                ? (int) slot.y() - GLYPH_SIZE - GAP
                : (int) (slot.y() + slot.height()) + GAP;
        return new Anchor(x, y);
    }

    private static boolean rootFits(BattleSlots.Slot slot, int badgeWidth) {
        if (slot.labelWidth() <= 0) {
            return false;
        }
        return rootInsideX(slot) + badgeWidth
                <= (int) slot.x() + (int) slot.width() - ROOT_ICON_RESERVE;
    }

    private static int rootInsideX(BattleSlots.Slot slot) {
        return (int) slot.x() + ROOT_TEXT_INSET + slot.labelWidth() + ROOT_LABEL_GAP;
    }

    /** The visible extent of a tile: the move page's type icon sticks out to the left. */
    public static Rect highlightFor(BattleSlots.Slot slot, BattleSlots.PageKind kind) {
        int x = (int) slot.x();
        int y = (int) slot.y();
        int width = (int) slot.width();
        int height = (int) slot.height();
        if (kind == BattleSlots.PageKind.MOVE) {
            x -= MOVE_ICON_OVERHANG;
            width += MOVE_ICON_OVERHANG;
        }
        return new Rect(x, y, width, height);
    }
}
