package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionBindingInfo;
import com.ifels.controlflex.api.IActionBindingProvider;
import com.ifels.controlflex.api.IControllerGlyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Draws a key hint on every actionable tile of the open battle sub-page.
 *
 * <p>Badges hang outside the option grid, flush with each tile's left edge: rows 0 and 2 above
 * the tile, rows 1 and 3 below it. See {@link HudLayout} for why the placement alternates.</p>
 *
 * <p>Bindings come from ControlFlex ({@code IActionBindingProvider}), which resolves against
 * the open screen, the active controller skin and shift layers in one call. Without
 * ControlFlex, or on an API older than 0.10.0, nothing is drawn and the screen is unchanged.</p>
 */
public final class BattleHud {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);
    private static final int BADGE_BG = 0x90000000;
    private static final int HIGHLIGHT_EDGE = 0xFFFFD54F;
    private static final int HIGHLIGHT_TINT = 0x30FFD54F;

    private static boolean diagnosed;
    private static boolean visible = true;

    private BattleHud() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        if (!BattleScreen.isBattleScreen(screen)) {
            return;
        }
        IActionBindingProvider provider = provider();
        if (provider == null) {
            return;
        }
        Object selection = BattleScreen.currentSelection(screen);
        List<BattleScreen.Slot> slots = BattleScreen.slots(selection);
        if (slots.isEmpty()) {
            return;
        }
        BattleScreen.PageKind kind = BattleScreen.kindOf(selection);

        // Diagnostics run even while hidden, so a report still shows whether bindings resolved.
        diagnoseOnce(provider, slots);
        if (!visible) {
            return;
        }

        int selected = BattleSelection.INSTANCE.sync(selection, slots.size());
        if (selected >= 0 && selected < slots.size()) {
            drawHighlight(graphics, slots.get(selected));
        }

        for (BattleScreen.Slot slot : slots) {
            IActionBindingInfo info = resolve(provider, slot.action());
            if (info == null || !info.isBound()) {
                continue;
            }
            List<IControllerGlyph> glyphs = info.getGlyphs();
            if (glyphs.isEmpty()) {
                continue;
            }
            HudLayout.Anchor anchor = HudLayout.anchorFor(slot, kind);
            drawRow(graphics, glyphs, anchor.x(), anchor.y());
        }
    }

    /**
     * ControlFlex may know an action under either the bridge mod id or the compat config's
     * {@code mod_id}, depending on whether the compat config loaded before or after the key
     * scan. Try both spellings rather than guessing.
     */
    private static IActionBindingInfo resolve(IActionBindingProvider provider, BattleAction action) {
        IActionBindingInfo last = null;
        for (String id : action.candidateIds()) {
            IActionBindingInfo info = provider.getActiveBinding(id);
            if (info != null && info.isBound()) {
                return info;
            }
            last = info;
        }
        return last;
    }

    /** One line per session so a report can tell "no bindings" from "no provider" from "no tiles". */
    private static void diagnoseOnce(IActionBindingProvider provider, List<BattleScreen.Slot> slots) {
        if (diagnosed || slots.isEmpty()) {
            return;
        }
        diagnosed = true;
        IActionBindingInfo probe = resolve(provider, slots.get(0).action());
        LOGGER.info("[cfx-compat-cobblemon] battle sub-page open: {} tile(s); first={} bound={} layer={}",
                slots.size(),
                slots.get(0).action().id(),
                probe != null && probe.isBound(),
                probe == null ? "-" : probe.getLayer());
    }

    /**
     * Flip the hint overlay and confirm it on the action bar.
     *
     * <p>Session-scoped on purpose: no config file, no loader-specific config API. If it should
     * survive a restart, that belongs in a config, not in a static field.</p>
     */
    public static void toggle() {
        visible = !visible;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.setOverlayMessage(
                    Component.translatable(visible
                            ? "cfx_compat_cobblemon.hud.on"
                            : "cfx_compat_cobblemon.hud.off"),
                    false);
        }
        LOGGER.info("[cfx-compat-cobblemon] key hints {}", visible ? "shown" : "hidden");
    }

    public static boolean isVisible() {
        return visible;
    }

    /** Outline + tint on the tile LB/RB is pointing at, so confirm has a visible target. */
    private static void drawHighlight(GuiGraphics graphics, BattleScreen.Slot slot) {
        int x1 = (int) slot.x();
        int y1 = (int) slot.y();
        int x2 = (int) (slot.x() + slot.width());
        int y2 = (int) (slot.y() + slot.height());
        graphics.fill(x1, y1, x2, y2, HIGHLIGHT_TINT);
        graphics.fill(x1, y1, x2, y1 + 1, HIGHLIGHT_EDGE);
        graphics.fill(x1, y2 - 1, x2, y2, HIGHLIGHT_EDGE);
        graphics.fill(x1, y1, x1 + 1, y2, HIGHLIGHT_EDGE);
        graphics.fill(x2 - 1, y1, x2, y2, HIGHLIGHT_EDGE);
    }

    /**
     * The binding provider, or null when ControlFlex is absent or older than API 0.10.0.
     *
     * <p>{@code getActionBindingProvider()} does not exist in older API jars, so the lookup
     * itself has to be guarded — a {@code NoSuchMethodError} here would break the whole
     * render pass instead of just dropping the hint overlay.</p>
     */
    private static IActionBindingProvider provider() {
        try {
            return ControlFlexApi.getActionBindingProvider();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void drawRow(GuiGraphics graphics, List<IControllerGlyph> glyphs, int x, int y) {
        int width = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            if (i > 0) {
                width += Minecraft.getInstance().font.width("+");
            }
            width += HudLayout.GLYPH_SIZE;
        }
        graphics.fill(x - 1, y - 1, x + width + 1, y + HudLayout.GLYPH_SIZE + 1, BADGE_BG);

        int cursor = x;
        for (int i = 0; i < glyphs.size(); i++) {
            if (i > 0) {
                graphics.drawString(Minecraft.getInstance().font, "+", cursor, y + 1, 0xFFE0E0E0, false);
                cursor += Minecraft.getInstance().font.width("+");
            }
            IControllerGlyph glyph = glyphs.get(i);
            String fallback = glyph.getTextFallback();
            if (fallback != null) {
                float scale = 0.6f;
                graphics.pose().pushPose();
                graphics.pose().translate(cursor, y + HudLayout.GLYPH_SIZE / 2f, 0);
                graphics.pose().scale(scale, scale, 1f);
                graphics.drawString(Minecraft.getInstance().font, fallback, 0,
                        Math.round(-Minecraft.getInstance().font.lineHeight / 2f), 0xFFFFFFFF, false);
                graphics.pose().popPose();
            } else {
                ResourceLocation texture = ResourceLocation.parse(glyph.getTextureId());
                graphics.blit(texture, cursor, y, HudLayout.GLYPH_SIZE, HudLayout.GLYPH_SIZE,
                        glyph.getU(), glyph.getV(), glyph.getWidth(), glyph.getHeight(),
                        glyph.getTextureWidth(), glyph.getTextureHeight());
            }
            cursor += HudLayout.GLYPH_SIZE;
        }
    }
}
