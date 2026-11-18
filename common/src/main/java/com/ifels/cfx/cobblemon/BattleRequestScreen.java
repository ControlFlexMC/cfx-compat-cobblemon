package com.ifels.cfx.cobblemon;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection gateway to Cobblemon's battle request screen.
 *
 * <p>{@code BattleConfigureGUI} is pointer-only in the same way the battle screen is: the format
 * arrows and the challenge button are widgets, so a controller would have to walk a cursor onto
 * them. This class exposes the three things a controller actually needs — step the format, read
 * the current format, press the primary button — without a compile dependency on Cobblemon.</p>
 *
 * <p>Everything here mirrors what the screen's own widgets do: {@link #cycle} calls the same
 * {@code setCurrentPage} the arrows call (including its wrap-around) and then refreshes the
 * button label, and {@link #pressPrimary} only presses a button that reports itself active, so a
 * request the server would refuse stays refused.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestScreen {

    public static final String GUI_CLASS =
            "com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleConfigureGUI";

    /** The small arrow buttons on both rows; matched by name so no class is loaded eagerly. */
    private static final String ARROW_CLASS =
            "com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleRequestNavigationButton";

    /** Field and method handles, keyed by {@code declaringClass#member}. */
    private static final Map<String, Object> MEMBERS = new ConcurrentHashMap<>();

    private static Class<?> guiType;

    private BattleRequestScreen() {
    }

    public static boolean isConfigureScreen(Screen screen) {
        return screen != null && GUI_CLASS.equals(screen.getClass().getName());
    }

    /** The options the server offered for this request; empty when the class is not loaded. */
    public static List<?> options() {
        Object value = staticValue("options");
        return value instanceof List<?> list ? list : Collections.emptyList();
    }

    public static int pageCount() {
        return options().size();
    }

    /** Index of the format currently shown. */
    public static int page(Screen screen) {
        Object value = fieldValue(screen, "currentPage");
        return value instanceof Number number ? number.intValue() : 0;
    }

    /** Log-friendly name of the shown format ({@code SINGLE_BATTLE}, …). */
    public static String pageName(Screen screen) {
        List<?> options = options();
        int page = page(screen);
        if (page < 0 || page >= options.size()) {
            return String.valueOf(page);
        }
        return String.valueOf(options.get(page));
    }

    /**
     * Step one format forward or back, exactly like the on-screen arrows.
     *
     * <p>{@code setCurrentPage} already wraps: a value past the end becomes 0 and a negative one
     * becomes the last page, so the caller only has to pass {@code current + step}.</p>
     *
     * @param step -1 for the previous format, +1 for the next
     * @return whether the screen accepted the change
     */
    public static boolean cycle(Screen screen, int step) {
        if (!isConfigureScreen(screen) || pageCount() == 0) {
            return false;
        }
        if (!invokeInt(screen, "setCurrentPage", page(screen) + step)) {
            return false;
        }
        // The button's label ("Challenge"/"Accept"/"Invite") is derived from the page, so it has
        // to be refreshed by hand — the arrows do the same in their own callbacks.
        invokeNoArg(screen, "updateRequestButtonText");
        return true;
    }

    /** Index of the level rule shown on the second row. */
    public static int levelIndex(Screen screen) {
        Object value = fieldValue(screen, "levelRulesetOptionIndex");
        return value instanceof Number number ? number.intValue() : 0;
    }

    /** How many level rules the screen offers; 0 when this request has no level row. */
    public static int levelCount() {
        Object value = staticValue("levelRulesetOption");
        return value instanceof List<?> list ? list.size() : 0;
    }

    /** Log-friendly name of the shown level rule. */
    public static String levelName(Screen screen) {
        Object value = staticValue("levelRulesetOption");
        int index = levelIndex(screen);
        if (!(value instanceof List<?> list) || index < 0 || index >= list.size()) {
            return String.valueOf(index);
        }
        return String.valueOf(list.get(index));
    }

    /**
     * Width of the panel's own artwork, in GUI pixels.
     *
     * <p>Cobblemon's {@code SIZE} is the same number the background texture is drawn at and the
     * same one its layout maths centres on, so it doubles as the panel edge for anything that has
     * to sit outside it.</p>
     */
    public static int panelWidth() {
        Object value = staticValue("SIZE");
        return value instanceof Number number ? number.intValue() : 0;
    }

    /** Left edge of the panel; it is centred on the screen. */
    public static int panelLeft(Screen screen) {
        int width = panelWidth();
        if (screen == null || width == 0) {
            return 0;
        }
        return (screen.width - width) / 2;
    }

    /**
     * Step one level rule forward or back, like the arrows under the format title.
     *
     * <p>No button-label refresh: the level rule only changes what the challenge packet carries,
     * and the screen's own arrows do not touch the label either.</p>
     */
    public static boolean cycleLevel(Screen screen, int step) {
        if (!isConfigureScreen(screen) || levelCount() == 0 || !levelRowShown(screen)) {
            return false;
        }
        return invokeInt(screen, "setLevelRulesetOptionIndex", levelIndex(screen) + step);
    }

    /**
     * Whether the level-rule row is on screen for the current page.
     *
     * <p>Cobblemon builds the level arrows once, when the screen opens on a format that has
     * levels, and does not remove them when the player cycles to the team page — its render only
     * clears their {@code active} flag. So an arrow being present says the screen <em>can</em>
     * show the row; both arrows being enabled says this page <em>does</em>.</p>
     */
    public static boolean levelRowShown(Screen screen) {
        return levelRowShown(levelArrows(screen));
    }

    /** The same test for a list already read off the screen. */
    public static boolean levelRowShown(List<Button> levelArrows) {
        return levelArrows.size() >= 2 && levelArrows.get(0).active && levelArrows.get(1).active;
    }

    /**
     * The format arrows, left to right; empty when the screen has none.
     *
     * <p>Read from the live widget list rather than the layout constants: the arrows move with
     * the panel, and a future Cobblemon that re-lays them out keeps working.</p>
     */
    public static List<Button> formatArrows(Screen screen) {
        List<Button> remaining = new ArrayList<>(arrows(screen));
        remaining.removeAll(levelArrows(screen));
        if (remaining.isEmpty()) {
            return List.of();
        }
        int topY = remaining.get(0).getY();
        List<Button> out = new ArrayList<>();
        for (Button arrow : remaining) {
            if (arrow.getY() == topY) {
                out.add(arrow);
            }
        }
        out.sort(Comparator.comparingInt(Button::getX));
        return out;
    }

    /**
     * The level-rule arrows, left to right; empty when the screen has no level row (a team
     * request does not offer one).
     */
    public static List<Button> levelArrows(Screen screen) {
        List<Button> all = arrows(screen);
        List<Button> out = new ArrayList<>();
        for (String field : new String[]{"autoLevelNavButtonLeft", "autoLevelNavButtonRight"}) {
            Object value = staticValue(field);
            if (value instanceof Button arrow && all.contains(arrow)) {
                out.add(arrow);
            }
        }
        out.sort(Comparator.comparingInt(Button::getX));
        return out;
    }

    /** Every {@code BattleRequestNavigationButton} currently on the screen, top-left first. */
    private static List<Button> arrows(Screen screen) {
        if (!isConfigureScreen(screen)) {
            return List.of();
        }
        List<Button> out = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && ARROW_CLASS.equals(child.getClass().getName())) {
                out.add(button);
            }
        }
        out.sort(Comparator.comparingInt(Button::getY).thenComparingInt(Button::getX));
        return out;
    }

    /** The screen's primary button, or null while the screen is still building itself. */
    public static Button primaryButton() {
        Object value = staticValue("requestButton");
        return value instanceof Button button ? button : null;
    }

    /**
     * The label the primary button is drawing right now; empty when there is no button.
     *
     * <p>{@code BattleRequestButton} keeps the visible text in its own mutable {@code text} field
     * and rewrites it when the format changes, while the inherited message keeps the value the
     * button was built with. Read the field first so a glyph placed after the label follows
     * "Challenge"/"Invite"/… instead of the stale message.</p>
     */
    public static Component primaryLabel() {
        Button button = primaryButton();
        if (button == null) {
            return Component.empty();
        }
        Object value = fieldValue(button, "text");
        return value instanceof Component label ? label : button.getMessage();
    }

    /**
     * Press the primary button the way a click would, minus the cursor.
     *
     * @return whether a press happened; a disabled button means "not allowed right now"
     */
    public static boolean pressPrimary() {
        Button button = primaryButton();
        if (button == null || !button.active || !button.visible) {
            return false;
        }
        button.onPress();
        return true;
    }

    // ===== reflection plumbing =====

    private static Object staticValue(String name) {
        try {
            Class<?> type = guiType();
            Field field = field(type, name);
            return field == null ? null : field.get(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Object fieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = field(target.getClass(), name);
            return field == null ? null : field.get(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static boolean invokeInt(Object target, String name, int value) {
        Method method = method(target.getClass(), name, int.class);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(target, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void invokeNoArg(Object target, String name) {
        Method method = method(target.getClass(), name);
        if (method == null) {
            return;
        }
        try {
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // the label refresh is cosmetic; a missing method must not break the key press
        }
    }

    private static Class<?> guiType() throws ClassNotFoundException {
        Class<?> cached = guiType;
        if (cached != null) {
            return cached;
        }
        Class<?> loaded = Class.forName(GUI_CLASS);
        guiType = loaded;
        return loaded;
    }

    private static Field field(Class<?> type, String name) {
        String key = type.getName() + '#' + name;
        Object cached = MEMBERS.get(key);
        if (cached instanceof Field found) {
            return found;
        }
        try {
            Field found = type.getDeclaredField(name);
            found.setAccessible(true);
            MEMBERS.put(key, found);
            return found;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... types) {
        String key = type.getName() + '#' + name + java.util.Arrays.toString(types);
        Object cached = MEMBERS.get(key);
        if (cached instanceof Method found) {
            return found;
        }
        try {
            Method found = type.getDeclaredMethod(name, types);
            found.setAccessible(true);
            MEMBERS.put(key, found);
            return found;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
