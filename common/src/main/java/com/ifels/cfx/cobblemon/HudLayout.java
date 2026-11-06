package com.ifels.cfx.cobblemon;

/**
 * Where a key hint badge goes relative to its tile.
 *
 * <p>Placement is per page, because the tiles differ in what is already painted inside them:</p>
 *
 * <ul>
 *   <li><b>Action menu</b> (4 square tiles, 26px): nothing free inside — name on the left, icon on
 *       the right, and a 2–5px row gap. Badges hang outside, flush with the tile's left edge,
 *       alternating above (row 0, 2, 4 …) and below (row 1, 3, 5 …). Alternating rather than
 *       "always above" means two consecutive badges never share the same gap.</li>
 *   <li><b>Move list</b> (4 wide bars, 24px): the bottom-left corner under the move name is empty
 *       — the name sits on the top line, PP on the bottom right, the category icon in the middle.
 *       Badges go <em>inside</em> that corner.</li>
 *   <li><b>Party list</b> (2×3, 29px): level, name and HP bar all start at the left edge, so the
 *       badging falls back to the outside rule.</li>
 * </ul>
 *
 * <p>Pure geometry, no Minecraft types, so it is unit-testable.</p>
 */
public final class HudLayout {

    public static final int GLYPH_SIZE = 10;
    public static final int GAP = 2;
    /** Inset from the tile edge when the badge sits inside the tile. */
    public static final int INSET = 3;

    private HudLayout() {
    }

    public record Anchor(int x, int y) {
    }

    public static Anchor anchorFor(BattleScreen.Slot slot, BattleScreen.PageKind kind) {
        if (kind == BattleScreen.PageKind.MOVE) {
            // Inside, bottom-left: above the tile's bottom edge, past its left edge.
            int x = (int) slot.x() + INSET;
            int y = (int) (slot.y() + slot.height()) - GLYPH_SIZE - GAP;
            return new Anchor(x, y);
        }
        int x = (int) slot.x();
        int y = slot.row() % 2 == 0
                ? (int) slot.y() - GLYPH_SIZE - GAP
                : (int) (slot.y() + slot.height()) + GAP;
        return new Anchor(x, y);
    }
}
