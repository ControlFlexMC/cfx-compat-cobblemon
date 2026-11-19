package com.ifels.cfx.cobblemon;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.SummaryTab;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Typed adapter to Cobblemon 1.5.2's summary tabs.
 *
 * <p>{@code Summary} builds its tabs as ordinary {@code SummaryTab} buttons and each one's own
 * {@code onPress} switches the page. This class finds those buttons in the live widget list and
 * presses the one the bumpers asked for, so a controller press is exactly a mouse click on that
 * tab: Cobblemon still plays the click sound, closes the move-swap panel and rebuilds the page
 * widget.</p>
 *
 * <p>Only the selected flag and the stored page index are private, so they are read reflectively;
 * everything else is compile-checked against the 1.5.2 jar, like {@link BattleScreen}.</p>
 *
 * @since 0.9.1
 */
public final class SummaryScreen {

    /**
     * The tab that draws itself as selected. Cobblemon keeps it in a private {@code isActive}
     * field with no getter, and that flag — not a remembered index — is what agrees with the
     * mouse: a tab clicked by hand must be the one the next bumper press steps away from.
     */
    private static final String ACTIVE_FIELD = "isActive";

    /** The page index {@code displayMainScreen} maintains; fallback when the flag is unreadable. */
    private static final String STORED_FIELD = "mainScreenIndex";

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private SummaryScreen() {
    }

    public static boolean isSummaryScreen(Screen screen) {
        return screen instanceof Summary;
    }

    /** The tab buttons in layout order; empty when the summary is not built yet. */
    public static List<SummaryTab> tabs(Screen screen) {
        if (!(screen instanceof Summary)) {
            return List.of();
        }
        List<SummaryTab> out = new ArrayList<>(3);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof SummaryTab tab) {
                out.add(tab);
            }
        }
        out.sort(Comparator.comparingInt(SummaryTab::getX));
        return out;
    }

    /**
     * Index of the tab currently drawn as selected, or -1 when no tab reports one.
     *
     * <p>Returning -1 (rather than 0) lets {@link #cycle} fall back to the page index Cobblemon
     * stores on the screen before assuming a fresh page.</p>
     */
    static int activeIndex(List<? extends SummaryTab> tabs) {
        for (int i = 0; i < tabs.size(); i++) {
            if (isActive(tabs.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isActive(SummaryTab tab) {
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

    /**
     * The page index Cobblemon keeps on the screen itself, or -1 when it cannot be used.
     *
     * <p>Fallback for a build that renames the tab's own flag: this is the number
     * {@code displayMainScreen} maintains and the tab callbacks compare against. Out-of-range
     * values are rejected — the screen also uses this field for its side-panel pages.</p>
     */
    private static int storedIndex(Summary summary, int count) {
        Field field = field(summary.getClass(), STORED_FIELD);
        if (field == null) {
            return -1;
        }
        try {
            int index = field.getInt(summary);
            return index >= 0 && index < count ? index : -1;
        } catch (IllegalAccessException | RuntimeException e) {
            return -1;
        }
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
        if (!(screen instanceof Summary summary)) {
            return null;
        }
        List<SummaryTab> tabs = tabs(screen);
        if (tabs.size() < 2) {
            return null;
        }
        int from = activeIndex(tabs);
        if (from < 0) {
            from = storedIndex(summary, tabs.size());
        }
        if (from < 0) {
            from = 0;
        }
        int to = SummaryTabs.stepIndex(from, tabs.size(), delta);
        tabs.get(to).onPress();
        return new Switch(from, to, tabs.size());
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
