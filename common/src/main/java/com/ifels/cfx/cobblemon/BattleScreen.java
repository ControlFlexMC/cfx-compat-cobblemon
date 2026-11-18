package com.ifels.cfx.cobblemon;

import com.ifels.cfx.cobblemon.BattleSlots.Page;
import com.ifels.cfx.cobblemon.BattleSlots.PageKind;
import com.ifels.cfx.cobblemon.BattleSlots.Slot;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection.MoveTile;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection.SwitchTile;
import com.cobblemon.mod.common.client.gui.battle.subscreen.ForfeitConfirmationSelection;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed adapter to Cobblemon 1.5.2's battle screen. This is the only class importing Cobblemon.
 *
 * <p>Clicks and scrolls go through the Minecraft supertypes' method names so Forge's
 * reobfuscation and Loom's remapping translate them; Cobblemon's own member names are stable.</p>
 */
public final class BattleScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static boolean loggedFailure;

    private BattleScreen() {
    }

    public static boolean isBattleScreen(Screen screen) {
        return screen instanceof BattleGUI;
    }

    public static Page page(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return Page.empty();
        }
        try {
            BattleActionSelection selection = gui.getCurrentActionSelection();
            if (selection == null) {
                return Page.empty();
            }
            if (selection instanceof BattleGeneralActionSelection root) {
                return rootPage(selection, root);
            }
            if (selection instanceof BattleMoveSelection move) {
                return movePage(selection, move);
            }
            if (selection instanceof BattleSwitchPokemonSelection swap) {
                return switchPage(selection, swap);
            }
            if (selection instanceof ForfeitConfirmationSelection confirm) {
                return confirmPage(selection, confirm);
            }
            return new Page(selection, PageKind.OTHER, List.of());
        } catch (RuntimeException e) {
            logFailure("read page", e);
            return Page.empty();
        }
    }

    public static boolean click(Screen screen, Slot slot) {
        if (!(screen instanceof BattleGUI) || slot == null) {
            return false;
        }
        try {
            return screen.mouseClicked(slot.centerX(), slot.centerY(), 0);
        } catch (RuntimeException e) {
            logFailure("click " + slot.displayName(), e);
            return false;
        }
    }

    public static boolean canGoBack(Screen screen) {
        Page page = page(screen);
        return page.id() != null && BattleSlots.canGoBack(page.kind());
    }

    public static boolean back(Screen screen) {
        if (!(screen instanceof BattleGUI gui)) {
            return false;
        }
        Page page = page(screen);
        if (page.id() == null || !BattleSlots.canGoBack(page.kind())) {
            return false;
        }
        try {
            gui.changeActionSelection(null);
            return true;
        } catch (RuntimeException e) {
            logFailure("back", e);
            return false;
        }
    }

    public static void scrollLog(Screen screen, int lines) {
        if (screen == null || lines == 0) {
            return;
        }
        try {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof BattleMessagePane pane) {
                    AbstractSelectionList<?> list = pane;
                    list.setScrollAmount(list.getScrollAmount()
                            + (double) lines * BattleMessagePane.LINE_HEIGHT);
                    return;
                }
            }
        } catch (RuntimeException e) {
            logFailure("scroll log", e);
        }
    }

    private static Page rootPage(Object id, BattleGeneralActionSelection root) {
        List<BattleOptionTile> tiles = root.getTiles();
        List<Slot> slots = new ArrayList<>(tiles.size());
        for (int i = 0; i < tiles.size(); i++) {
            BattleOptionTile tile = tiles.get(i);
            BattleAction action = BattleSlots.rootActionFor(tile.getResource().getPath());
            if (action == null) {
                continue;
            }
            slots.add(new Slot(action, tile.getX(), tile.getY(),
                    BattleOptionTile.OPTION_WIDTH, BattleOptionTile.OPTION_HEIGHT,
                    i / 2, labelWidth(tile)));
        }
        return new Page(id, PageKind.ROOT, slots);
    }

    private static Page movePage(Object id, BattleMoveSelection move) {
        List<MoveTile> tiles = move.getMoveTiles();
        List<Slot> slots = new ArrayList<>(4);
        for (int i = 0; i < tiles.size() && i < 4; i++) {
            MoveTile tile = tiles.get(i);
            if (!tile.getSelectable()) {
                continue;
            }
            slots.add(new Slot(BattleSlots.moveAction(i), tile.getX(), tile.getY(),
                    BattleMoveSelection.MOVE_WIDTH, BattleMoveSelection.MOVE_HEIGHT,
                    i / 2, 0));
        }
        return new Page(id, PageKind.MOVE, slots);
    }

    private static Page switchPage(Object id, BattleSwitchPokemonSelection swap) {
        List<SwitchTile> tiles = swap.getTiles();
        List<Slot> slots = new ArrayList<>(6);
        for (int i = 0; i < tiles.size() && i < 6; i++) {
            SwitchTile tile = tiles.get(i);
            slots.add(new Slot(BattleSlots.partyAction(i), tile.getX(), tile.getY(),
                    BattleOverlay.TILE_WIDTH, BattleOverlay.TILE_HEIGHT, i / 2, 0));
        }
        return new Page(id, PageKind.SWITCH, slots);
    }

    private static Page confirmPage(Object id, ForfeitConfirmationSelection confirm) {
        BattleOptionTile button = confirm.getForfeitButton();
        if (button == null) {
            return Page.empty();
        }
        return new Page(id, PageKind.CONFIRM, List.of(new Slot(null,
                button.getX(), button.getY(),
                BattleOptionTile.OPTION_WIDTH, BattleOptionTile.OPTION_HEIGHT, 0, 0)));
    }

    private static int labelWidth(BattleOptionTile tile) {
        return Minecraft.getInstance().font.width(tile.getText());
    }

    private static void logFailure(String what, RuntimeException e) {
        if (loggedFailure) {
            return;
        }
        loggedFailure = true;
        LOGGER.warn("[cfx-compat-cobblemon] battle gateway failed to {}; "
                + "actions stay unavailable until restart", what, e);
    }
}
