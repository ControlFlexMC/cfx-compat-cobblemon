package com.ifels.cfx.cobblemon;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The real widget path, not just the index maths: tabs are found among a screen's children, and a
 * bumper press runs the target tab's own {@code onPress} — the same method a mouse click runs.
 *
 * <p>Lives under {@code fabric/} rather than {@code common/} because it touches Minecraft's
 * {@code Screen}/{@code Button}, and the NeoForge ModDev test classpath does not carry Minecraft
 * types; {@code common/} tests stay loader-neutral. The bridge code under test is the same shared
 * source both loaders compile.</p>
 */
class SummaryTabSwitchTest {

    @Test
    void tabsAreFoundLeftToRightAndOtherWidgetsAreIgnored() {
        Summary screen = new Summary();
        SummaryTab second = new SummaryTab(40, false);
        SummaryTab first = new SummaryTab(10, true);
        screen.add(second);
        screen.add(exitButton());
        screen.add(first);

        assertEquals(List.of(first, second), SummaryScreen.tabs(screen));
    }

    @Test
    void cyclePressesTheNeighbouringTab() {
        Summary screen = new Summary();
        List<SummaryTab> pressed = new ArrayList<>();
        SummaryTab first = new SummaryTab(10, true, pressed);
        SummaryTab second = new SummaryTab(40, false, pressed);
        screen.add(first);
        screen.add(second);

        assertEquals(new SummaryScreen.Switch(0, 1, 2), SummaryScreen.cycle(screen, 1));
        assertEquals(List.of(second), pressed, "the second tab's own onPress must be the one that runs");
    }

    @Test
    void cycleWrapsFromTheLastTabToTheFirst() {
        Summary screen = new Summary();
        List<SummaryTab> pressed = new ArrayList<>();
        SummaryTab first = new SummaryTab(10, false, pressed);
        SummaryTab second = new SummaryTab(40, true, pressed);
        screen.add(first);
        screen.add(second);

        assertEquals(new SummaryScreen.Switch(1, 0, 2), SummaryScreen.cycle(screen, 1));
        assertEquals(List.of(first), pressed);
    }

    @Test
    void cycleWrapsBackwardsFromTheFirstTabToTheLast() {
        Summary screen = new Summary();
        List<SummaryTab> pressed = new ArrayList<>();
        SummaryTab first = new SummaryTab(10, true, pressed);
        SummaryTab second = new SummaryTab(40, false, pressed);
        screen.add(first);
        screen.add(second);

        assertEquals(new SummaryScreen.Switch(0, 1, 2), SummaryScreen.cycle(screen, -1));
        assertEquals(List.of(second), pressed);
    }

    @Test
    void cycleUsesTheStoredPageWhenNoTabFlagsItselfActive() {
        Summary screen = new Summary();
        screen.setMainScreenIndex(1);
        List<SummaryTab> pressed = new ArrayList<>();
        SummaryTab first = new SummaryTab(10, false, pressed);
        SummaryTab second = new SummaryTab(40, false, pressed);
        screen.add(first);
        screen.add(second);

        assertEquals(new SummaryScreen.Switch(1, 0, 2), SummaryScreen.cycle(screen, 1),
                "a renamed isActive must not turn every RB press into tab 2");
        assertEquals(List.of(first), pressed);
    }

    @Test
    void cycleIsRefusedWhenTheScreenHasNoTabs() {
        assertNull(SummaryScreen.cycle(new Summary(), 1), "nothing to press, nothing to report");
    }

    @Test
    void cycleIsRefusedWithASingleTab() {
        Summary screen = new Summary();
        screen.add(new SummaryTab(10, true));

        assertNull(SummaryScreen.cycle(screen, 1), "one tab is not a cycle");
    }

    private static Button exitButton() {
        return Button.builder(Component.literal("exit"), b -> {
        }).bounds(0, 20, 20, 20).build();
    }

    /** Same class-name suffix and same private flag as Cobblemon's {@code SummaryTab}. */
    public static final class SummaryTab extends Button {

        private boolean isActive;
        private final List<SummaryTab> pressed;

        SummaryTab(int x, boolean isActive) {
            this(x, isActive, new ArrayList<>());
        }

        SummaryTab(int x, boolean isActive, List<SummaryTab> pressed) {
            super(x, 0, 39, 13, Component.empty(), b -> {
            }, DEFAULT_NARRATION);
            this.isActive = isActive;
            this.pressed = pressed;
        }

        @Override
        public void onPress() {
            pressed.add(this);
        }
    }
}
