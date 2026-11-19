package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.ButtonName;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.TriggerMode;

/**
 * Declares the summary tab actions to ControlFlex.
 *
 * <p>Scoped to {@code Summary}: the same LB/RB used to cycle options in battle and formats on the
 * challenge panel become the page turn here, which is the shoulder-button convention for tabs.</p>
 *
 * <p>ControlFlex's built-in {@code gui_prev_tab}/{@code gui_next_tab} share this default and also
 * fire on this screen; they only act on the vanilla tabbed screens (creative, recipe book, guide),
 * so on the summary the press is handled here alone.</p>
 *
 * @since 0.10.0
 */
public final class SummaryActions {

    private SummaryActions() {
    }

    public static void register(IActionRegistrar registrar) {
        for (SummaryAction action : SummaryAction.values()) {
            registrar.register(ActionRegistration.builder(action.actionId())
                    .nameKey(action.id())
                    .groupModId("cobblemon")
                    .layer(ActionLayer.SCREEN)
                    .targetClass(SummaryScreen.GUI_CLASS)
                    .defaultBinding(defaultBinding(action))
                    .build());
        }
    }

    private static DefaultBinding defaultBinding(SummaryAction action) {
        return switch (action) {
            case PREV -> DefaultBinding.of(ButtonName.LEFT_BUMPER, TriggerMode.PRESS);
            case NEXT -> DefaultBinding.of(ButtonName.RIGHT_BUMPER, TriggerMode.PRESS);
        };
    }

    /** Maps a callback id back to the action; null when the id is not ours. */
    public static SummaryAction byId(String actionId) {
        for (SummaryAction action : SummaryAction.values()) {
            if (action.actionId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }
}
