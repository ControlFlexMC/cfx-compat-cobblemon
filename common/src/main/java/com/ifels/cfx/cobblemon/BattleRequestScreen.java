package com.ifels.cfx.cobblemon;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
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

    /** The screen's primary button, or null while the screen is still building itself. */
    public static Button primaryButton() {
        Object value = staticValue("requestButton");
        return value instanceof Button button ? button : null;
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
