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

import java.util.ArrayList;
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
    /** Ring thickness. Two pixels so it survives a one-pixel scissor at the tile's edge. */
    private static final int HIGHLIGHT_THICKNESS = 2;
    private static final int HIGHLIGHT_EDGE = 0xFFFFD54F;
    private static final int HIGHLIGHT_TINT = 0x30FFD54F;
    /** Distance from the screen edge for the LB/RB hint. */
    private static final int NAV_MARGIN = 6;
    private static final int NAV_TEXT = 0xFFE0E0E0;
    /** Dimmer text for the toggle hint while the hints themselves are hidden. */
    private static final int NAV_TEXT_OFF = 0xFF8A8A8A;
    /** ControlFlex's built-in cross-phase action; the battle bridge does not own it. */
    private static final String HUD_TOGGLE_ACTION = "hud_toggle";

    private static boolean diagnosed;
    private static boolean visible = true;
    /** Transient confirmation for the HUD toggle. Drawn here, not through the vanilla action bar. */
    private static final long TOAST_MS = 1400;
    private static long toastAt;
    private static boolean toastVisible;
    /** Page + index the ring was last logged for, so the log stays at one line per target. */
    private static Object loggedHighlightPage;
    private static int loggedHighlightIndex = -1;

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
        // The toggle confirmation must survive hiding itself: draw it before the visibility gate.
        drawToggleToast(graphics, screen);
        // Deliberately before the gate too: this is the way back when the hints are off, so it
        // stays on screen (dimmed) instead of vanishing with everything else.
        drawHudToggleHint(graphics, screen, provider);
        if (!visible) {
            return;
        }

        int selected = BattleSelection.INSTANCE.sync(selection, slots.size());
        if (selected >= 0 && selected < slots.size()) {
            drawHighlight(graphics, HudLayout.highlightFor(slots.get(selected), kind), selection, selected);
        }

        // Collect the badges before placing them: whether they go inside or outside is one
        // decision for the whole page, so it cannot be taken while drawing one tile at a time.
        List<BattleScreen.Slot> badged = new ArrayList<>();
        List<List<IControllerGlyph>> badgeGlyphs = new ArrayList<>();
        List<Integer> badgeWidths = new ArrayList<>();
        for (BattleScreen.Slot slot : slots) {
            IActionBindingInfo info = resolve(provider, slot.action());
            if (info == null || !info.isBound()) {
                continue;
            }
            List<IControllerGlyph> glyphs = info.getGlyphs();
            if (glyphs.isEmpty()) {
                continue;
            }
            badged.add(slot);
            badgeGlyphs.add(glyphs);
            badgeWidths.add(rowWidth(glyphs));
        }
        List<HudLayout.Anchor> anchors = HudLayout.anchorsFor(badged, kind, badgeWidths);
        for (int i = 0; i < badged.size(); i++) {
            HudLayout.Anchor anchor = anchors.get(i);
            drawRow(graphics, badgeGlyphs.get(i), anchor.x(), anchor.y());
        }

        drawPageNavHint(graphics, screen, provider, slots.size());
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
     * Apply a pushed state. ControlFlex owns the toggle: it flips, persists and notifies (spec:
     * screen-handler-hud-toggle §6) — this class only renders what it is told to.
     */
    public static void setVisible(boolean shown) {
        if (visible == shown) {
            return;
        }
        visible = shown;
        LOGGER.info("[cfx-compat-cobblemon] key hints {}", visible ? "shown" : "hidden");
    }

    public static boolean isVisible() {
        return visible;
    }

    /**
     * Tint + ring on the tile LB/RB is pointing at, so confirm has a visible target.
     *
     * <p>The ring is drawn <em>inside</em> the rect: a screen that scissors its widget area to the
     * tile edge would otherwise clip a border sitting exactly on that edge, leaving the highlight
     * invisible while the tint and the badges still show.</p>
     */
    private static void drawHighlight(GuiGraphics graphics, HudLayout.Rect rect,
                                      Object page, int index) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), HIGHLIGHT_TINT);
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + HIGHLIGHT_THICKNESS, HIGHLIGHT_EDGE);
        graphics.fill(rect.x(), rect.bottom() - HIGHLIGHT_THICKNESS, rect.right(), rect.bottom(), HIGHLIGHT_EDGE);
        graphics.fill(rect.x(), rect.y(), rect.x() + HIGHLIGHT_THICKNESS, rect.bottom(), HIGHLIGHT_EDGE);
        graphics.fill(rect.right() - HIGHLIGHT_THICKNESS, rect.y(), rect.right(), rect.bottom(), HIGHLIGHT_EDGE);

        if (page != loggedHighlightPage || index != loggedHighlightIndex) {
            loggedHighlightPage = page;
            loggedHighlightIndex = index;
            LOGGER.info("[cfx-compat-cobblemon] highlight rect=({}, {}) {}x{}", rect.x(), rect.y(),
                    rect.width(), rect.height());
        }
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

    /**
     * Draw one badge, left to right: glyphs separated by "+".
     *
     * <p>No backdrop: the badge sits on the tile's own artwork, which is already flat enough to
     * read against, and a translucent patch made the tile look dirty.</p>
     */
    private static void drawRow(GuiGraphics graphics, List<IControllerGlyph> glyphs, int x, int y) {
        drawGlyphRow(graphics, glyphs, x, y);
    }

    /**
     * The LB/RB hint, parked in the screen's bottom-right corner.
     *
     * <p>Not a per-tile hint: prev/next act on the list, not on one option, so repeating them on
     * every tile would be noise. The corner is the only place the battle screen leaves empty —
     * the message pane sits above it and the party bar is centred.</p>
     *
     * <p>The two buttons are separated by a slash rather than drawn adjacent: side by side with
     * nothing between reads as a chord (press both at once), which is the opposite of what these
     * do. The trailing label says what they switch.</p>
     */
    private static void drawPageNavHint(GuiGraphics graphics, Screen screen,
                                        IActionBindingProvider provider, int slotCount) {
        if (slotCount <= 1) {
            return;
        }
        List<IControllerGlyph> prev = glyphsOf(provider, BattleAction.PREV);
        List<IControllerGlyph> next = glyphsOf(provider, BattleAction.NEXT);
        if (prev.isEmpty() && next.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        String label = Component.translatable("cfx_compat_cobblemon.hud.prev_next").getString();
        String slash = "/";
        String gap = " ";

        int width = rowWidth(prev) + rowWidth(next)
                + (prev.isEmpty() || next.isEmpty() ? 0 : mc.font.width(slash) + 2)
                + (label.isEmpty() ? 0 : mc.font.width(gap) + mc.font.width(label));

        int y = screen.height - NAV_MARGIN - HudLayout.GLYPH_SIZE;
        int x = screen.width - NAV_MARGIN - width;

        int cursor = x;
        if (!prev.isEmpty()) {
            cursor = drawGlyphRow(graphics, prev, cursor, y);
        }
        if (!prev.isEmpty() && !next.isEmpty()) {
            graphics.drawString(mc.font, slash, cursor + 1, y + 1, NAV_TEXT, false);
            cursor += mc.font.width(slash) + 2;
        }
        if (!next.isEmpty()) {
            cursor = drawGlyphRow(graphics, next, cursor, y);
        }
        if (!label.isEmpty()) {
            cursor += mc.font.width(gap);
            graphics.drawString(mc.font, label, cursor, y + 1, NAV_TEXT, false);
        }
    }

    /**
     * Bottom-left, on the same baseline as the LB/RB hint: which key turns this overlay on and off.
     *
     * <p>Drawn even while the hints are hidden (dimmed) — otherwise the one control the player
     * needs to bring them back would disappear together with them.</p>
     */
    private static void drawHudToggleHint(GuiGraphics graphics, Screen screen,
                                          IActionBindingProvider provider) {
        IActionBindingInfo info = provider.getActiveBinding(HUD_TOGGLE_ACTION);
        if (info == null || !info.isBound() || info.getGlyphs().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String label = Component.translatable("cfx_compat_cobblemon.hud.toggle").getString();
        int y = screen.height - NAV_MARGIN - HudLayout.GLYPH_SIZE;
        int cursor = drawGlyphRow(graphics, info.getGlyphs(), NAV_MARGIN, y);
        if (!label.isEmpty()) {
            cursor += mc.font.width(" ");
            graphics.drawString(mc.font, label, cursor, y + 1,
                    visible ? NAV_TEXT : NAV_TEXT_OFF, false);
        }
    }

    /** Called by the screen handler on a real press, so the player sees the toggle land. */
    public static void flashToggle(boolean shown) {
        toastVisible = shown;
        toastAt = System.currentTimeMillis();
    }

    /**
     * One line just above the page-navigation hint — the only area this page leaves free at the
     * bottom. Drawn in the HUD's own pass rather than through the vanilla action bar, whose fixed
     * position lands between the option tiles and the message box here.
     */
    private static void drawToggleToast(GuiGraphics graphics, Screen screen) {
        if (toastAt == 0 || System.currentTimeMillis() - toastAt > TOAST_MS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Component text = Component.translatable(toastVisible
                ? "cfx_compat_cobblemon.hud.on"
                : "cfx_compat_cobblemon.hud.off");
        int x = screen.width - NAV_MARGIN - mc.font.width(text);
        int y = screen.height - NAV_MARGIN - HudLayout.GLYPH_SIZE - 10;
        graphics.drawString(mc.font, text, x, y, NAV_TEXT, true);
    }

    private static List<IControllerGlyph> glyphsOf(IActionBindingProvider provider, BattleAction action) {
        IActionBindingInfo info = resolve(provider, action);
        if (info == null || !info.isBound()) {
            return List.of();
        }
        return info.getGlyphs();
    }

    /** Width of a glyph row including its "+" separators. */
    private static int rowWidth(List<IControllerGlyph> glyphs) {
        if (glyphs.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            if (i > 0) {
                width += Minecraft.getInstance().font.width("+") + 2;
            }
            width += HudLayout.GLYPH_SIZE;
        }
        return width;
    }

    /** Draw a glyph row and return the x just past its last glyph. */
    private static int drawGlyphRow(GuiGraphics graphics, List<IControllerGlyph> glyphs, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        int cursor = x;
        for (int i = 0; i < glyphs.size(); i++) {
            if (i > 0) {
                graphics.drawString(mc.font, "+", cursor + 1, y + 1, NAV_TEXT, false);
                cursor += mc.font.width("+") + 2;
            }
            IControllerGlyph glyph = glyphs.get(i);
            String fallback = glyph.getTextFallback();
            if (fallback != null) {
                float scale = 0.6f;
                graphics.pose().pushPose();
                graphics.pose().translate(cursor, y + HudLayout.GLYPH_SIZE / 2f, 0);
                graphics.pose().scale(scale, scale, 1f);
                graphics.drawString(mc.font, fallback, 0,
                        Math.round(-mc.font.lineHeight / 2f), 0xFFFFFFFF, false);
                graphics.pose().popPose();
            } else {
                ResourceLocation texture = ResourceLocation.parse(glyph.getTextureId());
                graphics.blit(texture, cursor, y, HudLayout.GLYPH_SIZE, HudLayout.GLYPH_SIZE,
                        glyph.getU(), glyph.getV(), glyph.getWidth(), glyph.getHeight(),
                        glyph.getTextureWidth(), glyph.getTextureHeight());
            }
            cursor += HudLayout.GLYPH_SIZE;
        }
        return cursor;
    }
}
