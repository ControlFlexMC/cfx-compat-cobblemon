package com.ifels.cfx.cobblemon;

/**
 * Which tile the LB/RB highlight is on, for the sub-page that is open right now.
 *
 * <p>Deliberately not stored on the screen: Cobblemon rebuilds the sub-page object whenever the
 * turn advances, so the highlight has to reset with it. The page object's identity is the reset
 * key — a new page means index 0, and a shorter list clamps back into range.</p>
 *
 * <p>Stepping back to the action menu is the one exception. Cobblemon does not keep that instance
 * around: the back press removes the sub-page and the next frame builds a fresh
 * {@code BattleGeneralActionSelection}, so identity alone would drop the player on "Fight" every
 * time. {@link #markReturnToMenu()} records that the player asked to go back, and the menu that
 * appears next restores the tile the sub-page was opened from. A menu rebuilt for a new turn has
 * no such mark, so it still starts over at 0.</p>
 *
 * <p>Pure state, no Minecraft types, unit-testable.</p>
 */
public final class BattleSelection {

    /** Shared because the ticker moves it and the HUD draws it. Client thread only. */
    public static final BattleSelection INSTANCE = new BattleSelection();

    private Object page;
    private int index;
    /** Where the highlight sat on the action menu; restored when a sub-page steps back to it. */
    private int rootIndex;
    private boolean onRootMenu;
    /** One-shot: the back key stepped out of a sub-page and a menu is expected next. */
    private boolean returningToMenu;

    BattleSelection() {
    }

    /**
     * Bind to the current page and return the index to draw.
     *
     * @param currentPage identity of the sub-page object (used only for change detection)
     * @param slotCount   number of tiles on that page
     * @param rootMenu    whether this page is the four-tile action menu
     */
    public int sync(Object currentPage, int slotCount, boolean rootMenu) {
        if (currentPage != page) {
            boolean restore = returningToMenu && rootMenu;
            returningToMenu = false;
            page = currentPage;
            onRootMenu = rootMenu;
            index = restore ? rootIndex : 0;
        }

        if (slotCount <= 0) {
            index = 0;
            return 0;
        }
        if (index >= slotCount) {
            index = slotCount - 1;
        }
        if (rootMenu) {
            // Moving around the menu updates the position a sub-page will come back to.
            rootIndex = index;
        }
        return index;
    }

    /** The back key stepped out of a sub-page: the menu that lands next keeps its tile. */
    public void markReturnToMenu() {
        returningToMenu = true;
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
        if (onRootMenu) {
            rootIndex = index;
        }
        return index;
    }

    /** Point the highlight at a specific tile (used when a per-tile binding fires). */
    public void select(int target) {
        index = Math.max(0, target);
        if (onRootMenu) {
            rootIndex = index;
        }
    }

    public void reset() {
        page = null;
        index = 0;
        rootIndex = 0;
        onRootMenu = false;
        returningToMenu = false;
    }
}
