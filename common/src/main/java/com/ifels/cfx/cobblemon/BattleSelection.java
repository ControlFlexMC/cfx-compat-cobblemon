package com.ifels.cfx.cobblemon;

/**
 * Which tile the LB/RB highlight is on, for the sub-page that is open right now.
 *
 * <p>Deliberately not stored on the screen: Cobblemon rebuilds the sub-page object whenever the
 * turn advances, so the highlight has to reset with it. The page object's identity is the reset
 * key — a new page means index 0, and a shorter list clamps back into range.</p>
 *
 * <p>Pure state, no Minecraft types, unit-testable.</p>
 */
public final class BattleSelection {

    /** Shared because the ticker moves it and the HUD draws it. Client thread only. */
    public static final BattleSelection INSTANCE = new BattleSelection();

    private Object page;
    private int index;

    BattleSelection() {
    }

    /**
     * Bind to the current page and return the index to draw.
     *
     * @param currentPage identity of the sub-page object (used only for change detection)
     * @param slotCount   number of tiles on that page
     */
    public int sync(Object currentPage, int slotCount) {
        if (slotCount <= 0) {
            page = currentPage;
            index = 0;
            return 0;
        }
        if (currentPage != page) {
            page = currentPage;
            index = 0;
        }
        if (index >= slotCount) {
            index = slotCount - 1;
        }
        return index;
    }

    public int index() {
        return index;
    }

    /** Move by {@code delta} (normally ±1), wrapping at both ends. */
    public int move(int delta, int slotCount) {
        if (slotCount <= 0) {
            index = 0;
            return 0;
        }
        index = Math.floorMod(index + delta, slotCount);
        return index;
    }

    /** Point the highlight at a specific tile (used when a per-tile binding fires). */
    public void select(int target) {
        index = Math.max(0, target);
    }

    public void reset() {
        page = null;
        index = 0;
    }
}
