package com.ifels.cfx.cobblemon;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a key hint badge goes relative to its tile, and how far the selection ring has to reach.
 *
 * <p>Placement is per page, because the tiles differ in what is already painted inside them:</p>
 *
 * <ul>
 *   <li><b>Action menu</b> (4 square tiles, 26px): nothing free inside — name on the left, icon on
 *       the right, and a 2–5px row gap. Badges hang outside, flush with the tile's left edge,
 *       alternating above (row 0, 2, 4 …) and below (row 1, 3, 5 …). Alternating rather than
 *       "always above" means two consecutive badges never share the same gap.
 *
 *       <p>The label starts at {@code x+6} and the icon at about {@code x+70}, leaving ~64px for
 *       text. That is not much slack: the longest of Cobblemon's 28 locales is German
 *       {@code "Auswechseln"} at roughly 62px, so a badge inside would collide. Wrapping the label
 *       to two lines would fit geometrically, but the label is drawn by {@code BattleOptionTile}
 *       (single line, {@code x+6,y+8}, following the overlay opacity and hover texture) — taking
 *       that over would mean re-implementing Cobblemon's text rendering, or a mixin. Outside
 *       placement stays the only language-proof option.</p></li>
 *   <li><b>Move list</b> (4 wide bars, 24px): the bottom-left corner under the move name is empty
 *       — the name sits on the top line, PP on the bottom right, the category icon in the middle.
 *       Badges go <em>inside</em> that corner, past the type icon (see {@link #MOVE_ICON_OVERHANG}).</li>
 *   <li><b>Party list</b> (2×3, 29px): level, name and HP bar all start at the left edge, so the
 *       badging falls back to the outside rule.</li>
 *   <li><b>Forfeit confirmation</b> (2 buttons, 34×19, in a 113×45 panel): both buttons share one
 *       badge (the confirm binding), centred under each button and dropped below the panel —
 *       inside there is no room that would not cover the thumb artwork.</li>
 * </ul>
 *
 * <p>Pure geometry, no Minecraft types, so it is unit-testable.</p>
 */
public final class HudLayout {

    public static final int GLYPH_SIZE = 10;
    public static final int GAP = 2;

    /**
     * How far a move tile's artwork reaches left of its own rect.
     *
     * <p>{@code BattleMoveSelection.MoveTile} draws {@code TypeIcon(x - 9, y + 2)}: an 18×18 icon
     * at 0.5 scale, centred on the tile's left edge. So the visible tile starts 9px outside the
     * rect, and a highlight drawn on the rect alone leaves the icon hanging out to the left.</p>
     */
    public static final int MOVE_ICON_OVERHANG = 9;

    /** Inset from the tile's left edge when the badge sits inside the tile (clears the icon). */
    public static final int INSET = 12;

    /** Where {@code BattleOptionTile} starts drawing its label. */
    public static final int ROOT_TEXT_INSET = 6;
    /** Baseline row of that label — the badge shares the line when it sits inside. */
    public static final int ROOT_LABEL_Y = 8;
    /** Width the option's artwork occupies on the right of a root tile. */
    public static final int ROOT_ICON_RESERVE = 20;
    /** Gap between the label and an inside badge. */
    public static final int ROOT_LABEL_GAP = 4;

    /**
     * How far a confirm badge sits below its button.
     *
     * <p>The confirmation panel leaves 8px under the buttons (45px panel, buttons at y+18 and
     * 19px tall); ten puts the badge fully clear of the panel edge with a pixel of air.</p>
     */
    public static final int CONFIRM_BADGE_DROP = 10;

    private HudLayout() {
    }

    public record Anchor(int x, int y) {
    }

    /** A highlight rectangle in GUI coordinates. */
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    /**
     * Anchors for every badge on the page, in slot order.
     *
     * <p>Placement is decided <b>per page, not per tile</b>: either every badge sits inside its
     * tile or every badge hangs outside. A half-inside, half-outside row looks like a bug even
     * when each individual choice is correct, and the mixed case is exactly what a language with
     * one long label produces (de_de "Auswechseln" next to "Fängen").</p>
     *
     * @param badgeWidths width of each slot's badge, same order as {@code slots}
     */
    public static List<Anchor> anchorsFor(List<BattleScreen.Slot> slots,
                                          BattleScreen.PageKind kind,
                                          List<Integer> badgeWidths) {
        List<Anchor> anchors = new ArrayList<>(slots.size());
        boolean inside = kind == BattleScreen.PageKind.ROOT && allRootBadgesFit(slots, badgeWidths);
        for (int i = 0; i < slots.size(); i++) {
            anchors.add(anchorFor(slots.get(i), kind, width(badgeWidths, i), inside));
        }
        return anchors;
    }

    /** Inside only when every badge on the page has room between its label and the artwork. */
    private static boolean allRootBadgesFit(List<BattleScreen.Slot> slots,
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

    private static Anchor anchorFor(BattleScreen.Slot slot, BattleScreen.PageKind kind,
                                    int badgeWidth, boolean rootInsideAllowed) {
        if (kind == BattleScreen.PageKind.MOVE) {
            // Inside, bottom-left: above the tile's bottom edge, past the type icon.
            int x = (int) slot.x() + INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - GAP;
            return new Anchor(x, y);
        }
        if (kind == BattleScreen.PageKind.CONFIRM) {
            // Centred under each button, clear of the panel. The two buttons carry the same
            // confirm badge, so symmetric centring reads as "press this on the ringed one" —
            // inside a 34x19 button there is no room that would not cover the thumb artwork.
            int x = (int) slot.x() + ((int) slot.width() - badgeWidth) / 2;
            int y = (int) (slot.y() + slot.height()) + CONFIRM_BADGE_DROP;
            return new Anchor(x, y);
        }
        if (kind == BattleScreen.PageKind.ROOT && rootInsideAllowed) {
            return new Anchor(rootInsideX(slot), (int) slot.y() + ROOT_LABEL_Y);
        }
        int x = (int) slot.x();
        int y = slot.row() % 2 == 0
                ? (int) slot.y() - GLYPH_SIZE - GAP
                : (int) (slot.y() + slot.height()) + GAP;
        return new Anchor(x, y);
    }

    /**
     * Whether one badge has room between its label and the tile's artwork.
     *
     * <p>Measured, not assumed: the label starts at {@code x+6} and the artwork takes the right
     * {@link #ROOT_ICON_RESERVE} pixels, so this depends on the language. A single-button binding
     * (~10px) fits almost everywhere; de_de "Auswechseln" (~62px) leaves room for nothing.</p>
     */
    private static boolean rootFits(BattleScreen.Slot slot, int badgeWidth) {
        if (slot.labelWidth() <= 0) {
            // Label length unknown (reflection failed): do not guess, use the safe rule.
            return false;
        }
        return rootInsideX(slot) + badgeWidth
                <= (int) slot.x() + (int) slot.width() - ROOT_ICON_RESERVE;
    }

    /** x of an inside badge: right after the label, on the label's own line. */
    private static int rootInsideX(BattleScreen.Slot slot) {
        return (int) slot.x() + ROOT_TEXT_INSET + slot.labelWidth() + ROOT_LABEL_GAP;
    }

    /**
     * The area the selection ring has to enclose — the tile's <em>visible</em> extent, which is not
     * always its rect: on the move page the type icon sticks out to the left.
     */
    public static Rect highlightFor(BattleScreen.Slot slot, BattleScreen.PageKind kind) {
        int x = (int) slot.x();
        int y = (int) slot.y();
        int width = (int) slot.width();
        int height = (int) slot.height();
        if (kind == BattleScreen.PageKind.MOVE) {
            x -= MOVE_ICON_OVERHANG;
            width += MOVE_ICON_OVERHANG;
        }
        return new Rect(x, y, width, height);
    }
}
