package com.cobblemon.mod.common.client.gui.summary;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Test double that reports Cobblemon's screen class name, so {@link
 * com.ifels.cfx.cobblemon.SummaryScreen}'s exact-name check can be exercised without Cobblemon on
 * the test classpath. Fabric test sources only — never packaged, and intentionally not in
 * {@code common/}: the NeoForge ModDev test classpath has no Minecraft types.
 */
public class Summary extends Screen {

    /** Same private field Cobblemon uses for the open page, for the no-active-flag fallback. */
    private int mainScreenIndex;

    public Summary() {
        super(Component.literal("summary"));
    }

    public void add(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    public void setMainScreenIndex(int index) {
        this.mainScreenIndex = index;
    }
}
