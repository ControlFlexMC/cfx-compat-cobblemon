package com.ifels.cfx.cobblemon;

import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection gateway to Cobblemon's battle screen.
 *
 * <p>Deliberately reflection-based: a compile dependency on Cobblemon would have to be
 * remapped per loader (Fabric's production jars are intermediary, NeoForge's are official),
 * and would pin this bridge to one exact Cobblemon build. Cobblemon's own class and method
 * names are not remapped, so looking them up by name works on both loaders and degrades to
 * "no actions" instead of a {@code NoSuchMethodError} if a future version moves them.</p>
 *
 * <p>Everything here is read-only except {@link #click} / {@link #back} / {@link #scrollLog},
 * which replay what a mouse click or wheel event would do — through Cobblemon's own hit
 * testing, so disabled tiles and illegal targets are still rejected by Cobblemon.</p>
 */
public final class BattleScreen {

    public static final String GUI_CLASS = "com.cobblemon.mod.common.client.gui.battle.BattleGUI";

    private static final int ROOT_W = 90;
    private static final int ROOT_H = 26;
    private static final int MOVE_W = 92;
    private static final int MOVE_H = 24;
    private static final int SWITCH_W = 94;
    private static final int SWITCH_H = 29;

    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private BattleScreen() {
    }

    /** One clickable tile in the sub-page that is open right now. */
    public record Slot(BattleAction action, float x, float y, float width, float height, int row) {
        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }
    }

    public static boolean isBattleScreen(Screen screen) {
        return screen != null && GUI_CLASS.equals(screen.getClass().getName());
    }

    /** The sub-page currently mounted on the battle screen, or null while waiting on the opponent. */
    public static Object currentSelection(Screen screen) {
        if (!isBattleScreen(screen)) {
            return null;
        }
        return invoke(screen, "getCurrentActionSelection");
    }

    /**
     * Tiles of the open sub-page, in reading order, with the action each one stands for.
     *
     * <p>Root tiles are identified by their icon texture rather than by position: Cobblemon
     * drops the party tile when the active Pokémon is trapped, which shifts the remaining
     * tiles.</p>
     */
    /** Which sub-page is mounted, for callers that place hints differently per page. */
    public enum PageKind {
        ROOT,
        MOVE,
        SWITCH,
        OTHER
    }

    public static PageKind kindOf(Object selection) {
        if (selection == null) {
            return PageKind.OTHER;
        }
        String name = selection.getClass().getName();
        if (name.endsWith("BattleGeneralActionSelection")) {
            return PageKind.ROOT;
        }
        if (name.endsWith("BattleMoveSelection")) {
            return PageKind.MOVE;
        }
        if (name.endsWith("BattleSwitchPokemonSelection")) {
            return PageKind.SWITCH;
        }
        return PageKind.OTHER;
    }

    public static List<Slot> slots(Object selection) {
        if (selection == null) {
            return List.of();
        }
        String name = selection.getClass().getName();
        if (name.endsWith("BattleGeneralActionSelection")) {
            return rootSlots(selection);
        }
        if (name.endsWith("BattleMoveSelection")) {
            return moveSlots(selection);
        }
        if (name.endsWith("BattleSwitchPokemonSelection")) {
            return switchSlots(selection);
        }
        return List.of();
    }

    private static List<Slot> rootSlots(Object selection) {
        List<?> tiles = asList(invoke(selection, "getTiles"));
        List<Slot> out = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size(); i++) {
            Object tile = tiles.get(i);
            BattleAction action = rootActionFor(String.valueOf(invoke(tile, "getResource")));
            if (action == null) {
                continue;
            }
            out.add(new Slot(action, num(invoke(tile, "getX")), num(invoke(tile, "getY")),
                    ROOT_W, ROOT_H, i / 2));
        }
        return out;
    }

    private static List<Slot> moveSlots(Object selection) {
        List<?> tiles = asList(invoke(selection, "getMoveTiles"));
        List<Slot> out = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size() && i < 4; i++) {
            Object tile = tiles.get(i);
            if (Boolean.FALSE.equals(invoke(tile, "getSelectable"))) {
                continue;
            }
            out.add(new Slot(BattleAction.move(i + 1),
                    num(invoke(tile, "getX")), num(invoke(tile, "getY")), MOVE_W, MOVE_H, i / 2));
        }
        return out;
    }

    private static List<Slot> switchSlots(Object selection) {
        List<?> tiles = asList(invoke(selection, "getTiles"));
        List<Slot> out = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size() && i < 6; i++) {
            Object tile = tiles.get(i);
            out.add(new Slot(BattleAction.party(i + 1),
                    num(invoke(tile, "getX")), num(invoke(tile, "getY")), SWITCH_W, SWITCH_H, i / 2));
        }
        return out;
    }

    private static BattleAction rootActionFor(String resource) {
        if (resource == null) {
            return null;
        }
        if (resource.endsWith("battle_menu_fight.png")) {
            return BattleAction.ROOT_FIGHT;
        }
        if (resource.endsWith("battle_menu_switch.png")) {
            return BattleAction.ROOT_SWITCH;
        }
        if (resource.endsWith("battle_menu_bag.png")) {
            return BattleAction.ROOT_CAPTURE;
        }
        if (resource.endsWith("battle_menu_run.png") || resource.endsWith("battle_menu_forfeit.png")) {
            return BattleAction.ROOT_RUN;
        }
        return null;
    }

    /** Replay a left click at the tile centre. Returns whether the sub-page accepted it. */
    public static boolean click(Object selection, Slot slot) {
        if (selection == null || slot == null) {
            return false;
        }
        Object result = invoke(selection, "mousePrimaryClicked",
                new Class<?>[]{double.class, double.class},
                new Object[]{slot.centerX(), slot.centerY()});
        return Boolean.TRUE.equals(result);
    }

    /**
     * Whether there is a page to step back to. The action menu is the top level — from there the
     * default Escape should minimise the battle instead.
     */
    public static boolean canGoBack(Object selection) {
        return selection != null
                && !selection.getClass().getName().endsWith("BattleGeneralActionSelection");
    }

    /**
     * Leave the current sub-page and land back on the action menu.
     *
     * <p>{@code changeActionSelection} belongs to {@code BattleGUI}, not to the sub-page — calling
     * it on the page silently found nothing and left back to the built-in Escape, which closed the
     * whole screen. This mirrors what Cobblemon's own back button does.</p>
     *
     * @return whether the switch was issued
     */
    public static boolean back(Screen screen, Object selection) {
        if (screen == null || !canGoBack(selection)) {
            return false;
        }
        return invokeSingleArg(screen, "changeActionSelection");
    }

    /** Invoke a one-argument method on {@code target} with {@code null}, without knowing its type. */
    private static boolean invokeSingleArg(Object target, String name) {
        for (Method method : target.getClass().getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            try {
                method.invoke(target, new Object[]{null});
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /** The battle log widget, or null when the screen has none. */
    public static Object messagePane(Screen screen) {
        if (screen == null) {
            return null;
        }
        for (Object child : screen.children()) {
            if (child != null && child.getClass().getName().endsWith("BattleMessagePane")) {
                return child;
            }
        }
        return null;
    }

    /** Scroll the battle log by {@code lines} rows (positive scrolls down). */
    public static void scrollLog(Screen screen, int lines) {
        Object pane = messagePane(screen);
        if (pane == null) {
            return;
        }
        Object amount = invoke(pane, "getScrollAmount");
        if (!(amount instanceof Number current)) {
            return;
        }
        Method setter = findMethod(pane.getClass(), "setScrollAmount", double.class);
        if (setter == null) {
            return;
        }
        try {
            setter.invoke(pane, current.doubleValue() + lines * 10.0);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // log widget disappeared mid-tick
        }
    }

    // ===== reflection plumbing =====

    private static Object invoke(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), name, types);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... types) {
        String key = type.getName() + '#' + name + java.util.Arrays.toString(types);
        Method cached = METHODS.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Method found = type.getMethod(name, types);
            METHODS.put(key, found);
            return found;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : Collections.emptyList();
    }

    private static float num(Object value) {
        return value instanceof Number number ? number.floatValue() : 0f;
    }
}
