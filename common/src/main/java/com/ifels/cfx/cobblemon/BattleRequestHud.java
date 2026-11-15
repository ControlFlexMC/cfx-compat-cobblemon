package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ControlFlexApi;
import com.ifels.controlflex.api.IActionBindingInfo;
import com.ifels.controlflex.api.IActionBindingProvider;
import com.ifels.controlflex.api.IControllerGlyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ifels.cfx.cobblemon.BattleHud.HUD_TOGGLE_ACTION;
import static com.ifels.cfx.cobblemon.BattleHud.NAV_MARGIN;
import static com.ifels.cfx.cobblemon.BattleHud.NAV_TEXT;
import static com.ifels.cfx.cobblemon.BattleHud.NAV_TEXT_OFF;
import static com.ifels.cfx.cobblemon.BattleHud.drawGlyphRow;
import static com.ifels.cfx.cobblemon.BattleHud.rowWidth;

/**
 * Key hints for the battle request screen, laid out like the battle screen's:
 * the toggle in the bottom-left, the format switch in the bottom-right, and the press that
 * commits the request drawn on the button it presses.
 *
 * <p>Bindings come from ControlFlex's {@code IActionBindingProvider}, so a rebind or a controller
 * swap shows up on the next frame — the same contract {@link BattleHud} works under.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestHud {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    /** Gap between the primary button and the glyph above it. */
    private static final int BADGE_GAP = 2;
    /** Gap between an arrow and the glyph sitting outside it. */
    private static final int ARROW_GAP = 1;

    private static boolean visible = true;

    private BattleRequestHud() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        if (!BattleRequestScreen.isConfigureScreen(screen)) {
            return;
        }
        IActionBindingProvider provider = provider();
        if (provider == null) {
            return;
        }
        // Drawn before the gate on purpose: it is the way back when the hints are off.
        drawHudToggleHint(graphics, screen, provider);
        if (!visible) {
            return;
        }
        drawConfirmBadge(graphics, provider);
        drawArrowGlyphs(graphics, screen, provider);
    }

    /** Same contract as {@link BattleHud#setVisible}: ControlFlex owns the toggle, we only draw. */
    public static void setVisible(boolean shown) {
        if (visible == shown) {
            return;
        }
        visible = shown;
        LOGGER.info("[cfx-compat-cobblemon] request key hints {}", visible ? "shown" : "hidden");
    }

    public static boolean isVisible() {
        return visible;
    }

    /**
     * The press that commits the request, parked just above the button it presses.
     *
     * <p>No label: the button's own text already says "Challenge"/"Accept"/"Invite", and
     * repeating it above the glyph read as two captions for one control.</p>
     */
    private static void drawConfirmBadge(GuiGraphics graphics, IActionBindingProvider provider) {
        Button button = BattleRequestScreen.primaryButton();
        if (button == null) {
            return;
        }
        List<IControllerGlyph> glyphs = glyphsOf(provider, BattleRequestAction.CONFIRM.actionId());
        if (glyphs.isEmpty()) {
            return;
        }
        int width = rowWidth(glyphs);
        int x = button.getX() + (button.getWidth() - width) / 2;
        int y = button.getY() - HudLayout.GLYPH_SIZE - BADGE_GAP;
        if (y < 0) {
            return;
        }
        drawGlyphRow(graphics, glyphs, x, y);
    }

    /**
     * Both arrow rows carry their own hint: one glyph outside each arrow, no label.
     *
     * <p>The arrow already says which way the press goes and the panel leaves no room for a
     * caption on both sides — a word would either overlap the format icons or push the glyph off
     * the panel.</p>
     */
    private static void drawArrowGlyphs(GuiGraphics graphics, Screen screen,
                                        IActionBindingProvider provider) {
        drawArrowPair(graphics, provider, BattleRequestScreen.formatArrows(screen),
                BattleRequestAction.PREV, BattleRequestAction.NEXT);
        drawArrowPair(graphics, provider, BattleRequestScreen.levelArrows(screen),
                BattleRequestAction.LEVEL_PREV, BattleRequestAction.LEVEL_NEXT);
    }

    private static void drawArrowPair(GuiGraphics graphics, IActionBindingProvider provider,
                                      List<Button> arrows,
                                      BattleRequestAction previous, BattleRequestAction next) {
        if (arrows.size() < 2) {
            return;
        }
        drawOutside(graphics, glyphsOf(provider, previous.actionId()), arrows.get(0), true);
        drawOutside(graphics, glyphsOf(provider, next.actionId()),
                arrows.get(arrows.size() - 1), false);
    }

    /** One glyph just outside an arrow, vertically centred on it. */
    private static void drawOutside(GuiGraphics graphics, List<IControllerGlyph> glyphs,
                                    Button arrow, boolean toTheLeft) {
        if (glyphs.isEmpty()) {
            return;
        }
        int width = rowWidth(glyphs);
        int x = toTheLeft
                ? arrow.getX() - width - ARROW_GAP
                : arrow.getX() + arrow.getWidth() + ARROW_GAP;
        int y = arrow.getY() + (arrow.getHeight() - HudLayout.GLYPH_SIZE) / 2;
        drawGlyphRow(graphics, glyphs, x, y);
    }

    /** Bottom-left, dimmed while the hints are off so the way back stays visible. */
    private static void drawHudToggleHint(GuiGraphics graphics, Screen screen,
                                           IActionBindingProvider provider) {
        IActionBindingInfo info = provider.getActiveBinding(HUD_TOGGLE_ACTION);
        if (info == null || !info.isBound() || info.getGlyphs().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String label = Component.translatable(visible
                ? "cfx_compat_cobblemon.hud.toggle.hide"
                : "cfx_compat_cobblemon.hud.toggle.show").getString();
        int y = screen.height - NAV_MARGIN - HudLayout.GLYPH_SIZE;
        int cursor = drawGlyphRow(graphics, info.getGlyphs(), NAV_MARGIN, y);
        if (!label.isEmpty()) {
            cursor += mc.font.width(" ");
            graphics.drawString(mc.font, label, cursor, y + 1,
                    visible ? NAV_TEXT : NAV_TEXT_OFF, false);
        }
    }

    private static List<IControllerGlyph> glyphsOf(IActionBindingProvider provider, String actionId) {
        IActionBindingInfo info = provider.getActiveBinding(actionId);
        if (info == null || !info.isBound()) {
            return List.of();
        }
        return info.getGlyphs();
    }

    private static IActionBindingProvider provider() {
        try {
            return ControlFlexApi.getActionBindingProvider();
        } catch (Throwable t) {
            return null;
        }
    }
}
