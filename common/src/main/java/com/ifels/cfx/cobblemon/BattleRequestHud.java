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
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ifels.cfx.cobblemon.BattleHud.drawGlyphRow;
import static com.ifels.cfx.cobblemon.BattleHud.rowWidth;

/**
 * Key hints for the battle request screen: the toggle in the bottom-left, one glyph beside each
 * arrow outside the panel, and the press that commits the request drawn on the button it presses,
 * just past the label.
 *
 * <p>Bindings come from ControlFlex's {@code IActionBindingProvider}, so a rebind or a controller
 * swap shows up on the next frame — the same contract {@link BattleHud} works under.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestHud {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    /**
     * Size of the format/level glyphs outside the panel.
     *
     * <p>ControlFlex draws its own hints at 16px, and the 32px atlas cell stays crisp at half
     * size; the panel has room for that on both sides, so the outer columns read at a glance.</p>
     */
    private static final int SIDE_GLYPH_SIZE = 16;
    /** x {@code BattleRequestButton} centres its label on (69px wide, text drawn at {@code x + 35}). */
    private static final int PRIMARY_LABEL_CENTER = 35;
    /** The font that button renders its label with ({@code CobblemonResources.DEFAULT_LARGE}). */
    private static final ResourceLocation PRIMARY_LABEL_FONT = ResourceLocation.parse("uniform");
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
        BattleHud.drawToggleHint(graphics, screen, provider, visible);
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
     * The press that commits the request, on the button's own line just past its label.
     *
     * <p>It used to hang above the button, where it collided with the opponent's name — the gap
     * between the name baseline and the button's top edge is thinner than a glyph. Beside the
     * label it also reads as "this button, this press" rather than as a caption above it.</p>
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
        int x = HudLayout.requestLabelBadgeX(button.getX() + PRIMARY_LABEL_CENTER,
                primaryLabelWidth());
        int y = HudLayout.requestLabelBadgeY(button.getY(), button.getHeight(), HudLayout.GLYPH_SIZE);
        drawGlyphRow(graphics, glyphs, x, y);
    }

    /** Advance of the label the button is drawing now, measured with the same style it uses. */
    private static int primaryLabelWidth() {
        Component label = BattleRequestScreen.primaryLabel();
        Component styled = label.copy()
                .withStyle(style -> style.withBold(true).withFont(PRIMARY_LABEL_FONT));
        return Minecraft.getInstance().font.width(styled);
    }

    /**
     * Both arrow rows carry their own hint: one glyph outside the panel on each side, no label.
     *
     * <p>The arrow already says which way the press goes and the panel leaves no room for a
     * caption on both sides — a word would either overlap the format icons or push the glyph off
     * the panel.</p>
     */
    private static void drawArrowGlyphs(GuiGraphics graphics, Screen screen,
                                        IActionBindingProvider provider) {
        int panelLeft = BattleRequestScreen.panelLeft(screen);
        int panelWidth = BattleRequestScreen.panelWidth();
        if (panelWidth <= 0) {
            return;
        }
        drawArrowPair(graphics, provider, BattleRequestScreen.formatArrows(screen),
                BattleRequestAction.PREV, BattleRequestAction.NEXT, panelLeft, panelWidth);
        List<Button> levelArrows = BattleRequestScreen.levelArrows(screen);
        // Cobblemon keeps the level arrows in the screen after a cycle to the team page and only
        // disables them; without this check the d-pad hints would point at a row that is not there.
        if (BattleRequestScreen.levelRowShown(levelArrows)) {
            drawArrowPair(graphics, provider, levelArrows,
                    BattleRequestAction.LEVEL_PREV, BattleRequestAction.LEVEL_NEXT,
                    panelLeft, panelWidth);
        }
    }

    private static void drawArrowPair(GuiGraphics graphics, IActionBindingProvider provider,
                                      List<Button> arrows, BattleRequestAction previous,
                                      BattleRequestAction next, int panelLeft, int panelWidth) {
        if (arrows.size() < 2) {
            return;
        }
        Button first = arrows.get(0);
        int y = first.getY() + (first.getHeight() - SIDE_GLYPH_SIZE) / 2;
        drawOutside(graphics, glyphsOf(provider, previous.actionId()), panelLeft, panelWidth, true, y);
        drawOutside(graphics, glyphsOf(provider, next.actionId()), panelLeft, panelWidth, false, y);
    }

    /**
     * One glyph just outside the panel, vertically centred on its row's arrows.
     *
     * <p>Both sides measure from the panel edge rather than from the arrow: the arrows are not
     * symmetric inside the 113 px artwork, so anchoring to them put the left glyph outside the
     * panel and the right one inside it. Measuring from the panel also makes the two rows line up
     * with each other.</p>
     */
    private static void drawOutside(GuiGraphics graphics, List<IControllerGlyph> glyphs,
                                    int panelLeft, int panelWidth, boolean toTheLeft, int y) {
        if (glyphs.isEmpty()) {
            return;
        }
        int width = rowWidth(glyphs, SIDE_GLYPH_SIZE);
        int x = toTheLeft
                ? panelLeft - width - ARROW_GAP
                : panelLeft + panelWidth + ARROW_GAP;
        drawGlyphRow(graphics, glyphs, x, y, SIDE_GLYPH_SIZE);
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
