package com.ifels.cfx.cobblemon;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection gateway to Cobblemon's summary screen, limited to its tab pages (four in 1.7.x and
 * 1.8.x).
 *
 * <p>{@code Summary} builds its tabs as ordinary {@code Button}s (one {@code SummaryTab} per page,
 * laid out left to right) and each one's own {@code onPress} switches the page. This class finds
 * those buttons in the live widget list and presses the one the bumpers asked for, so a controller
 * press is exactly a mouse click on that tab: Cobblemon still decides what switching away from a
 * page means (saving the active mark, closing the move-swap panel, rebuilding the page widget).</p>
 *
 * <p>Like {@link BattleScreen}, everything goes through reflection: the bridge has no compile
 * dependency on Cobblemon, and a future version that moves these members loses the action instead
 * of throwing into the input tick.</p>
 *
 * @since 0.10.0
 */
public final class SummaryScreen {

    public static final String GUI_CLASS = "com.cobblemon.mod.common.client.gui.summary.Summary";

    /**
     * Suffix of the tab widget class. Matched by name because the type is not on the compile
     * classpath; it is the only widget on this screen whose simple name ends with this.
     */
    private static final String TAB_CLASS_SUFFIX = "SummaryTab";

    /**
     * The tab that draws itself as selected. Cobblemon keeps it in a private {@code isActive} field
     * with no getter, and that flag — not a remembered index — is what agrees with the mouse: a tab
     * clicked by hand must be the one the next bumper press steps away from.
     */
    private static final String ACTIVE_FIELD = "isActive";

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private SummaryScreen() {
    }

    public static boolean isSummaryScreen(Screen screen) {
        return screen != null && GUI_CLASS.equals(screen.getClass().getName());
    }

    /** The tab buttons in layout order; empty when the screen is not loaded or not built yet. */
    public static List<Button> tabs(Screen screen) {
        if (!isSummaryScreen(screen)) {
            return List.of();
        }
        List<Button> out = new ArrayList<>(4);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && isTab(child)) {
                out.add(button);
            }
        }
        out.sort(Comparator.comparingInt(Button::getX));
        return out;
    }

    static boolean isTab(Object widget) {
        return widget != null && widget.getClass().getName().endsWith(TAB_CLASS_SUFFIX);
    }

    /**
     * Index of the tab currently drawn as selected, or -1 when no tab reports one.
     *
     * <p>Returning -1 (rather than 0) lets {@link #cycle} fall back to the page index Cobblemon
     * stores on the screen before assuming a fresh page, so a renamed flag does not silently turn
     * every next press into "open tab 2".</p>
     */
    static int activeIndex(List<?> tabs) {
        for (int i = 0; i < tabs.size(); i++) {
            if (isActive(tabs.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isActive(Object tab) {
        Field field = field(tab.getClass(), ACTIVE_FIELD);
        if (field == null) {
            return false;
        }
        try {
            return field.getBoolean(tab);
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
    }

    /** Where a step of {@code delta} lands, wrapping at both ends. */
    static int stepIndex(int active, int count, int delta) {
        if (count <= 0) {
            return 0;
        }
        return Math.floorMod(active + delta, count);
    }

    /** One bumper press: which tab it left, which it opened, and how many there are. */
    public record Switch(int from, int to, int count) {
    }

    /**
     * Press the tab one step away from the selected one, the same way a click would.
     *
     * @param delta -1 for the previous tab, +1 for the next
     * @return what moved, or {@code null} when this screen has no tabs to step through
     */
    public static Switch cycle(Screen screen, int delta) {
        List<Button> tabs = tabs(screen);
        if (tabs.size() < 2) {
            return null;
        }
        int from = activeIndex(tabs);
        if (from < 0) {
            from = storedIndex(screen, tabs.size());
        }
        if (from < 0) {
            from = 0;
        }
        int to = stepIndex(from, tabs.size(), delta);
        tabs.get(to).onPress();
        return new Switch(from, to, tabs.size());
    }

    /**
     * The page index Cobblemon keeps on the screen itself, or -1 when it cannot be used.
     *
     * <p>Fallback for a build that renames the tab's own flag: this is the number
     * {@code displayMainScreen} maintains and the one the tab callbacks compare against. Values
     * outside the tab range are rejected — the screen also uses this field for its party and
     * side-panel pages, which are not tabs.</p>
     */
    private static int storedIndex(Screen screen, int count) {
        Field field = field(screen.getClass(), "mainScreenIndex");
        if (field == null) {
            return -1;
        }
        try {
            int index = field.getInt(screen);
            return index >= 0 && index < count ? index : -1;
        } catch (IllegalAccessException | RuntimeException e) {
            return -1;
        }
    }

    // ===== reflection plumbing =====

    private static Field field(Class<?> type, String name) {
        String key = type.getName() + '#' + name;
        Field cached = FIELDS.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Field found = type.getDeclaredField(name);
            found.setAccessible(true);
            FIELDS.put(key, found);
            return found;
        } catch (NoSuchFieldException | RuntimeException e) {
            return null;
        }
    }
}
