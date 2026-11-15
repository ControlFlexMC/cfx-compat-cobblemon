package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.ButtonName;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.TriggerMode;

/**
 * Declares the battle request actions to ControlFlex.
 *
 * <p>Scoped to {@code BattleConfigureGUI}: the same LB/RB a battle screen uses for its option
 * list, and Y for the primary button so a challenge is one press away instead of a cursor trip
 * to the middle of the panel.</p>
 *
 * @since 0.2.0
 */
public final class BattleRequestActions {

    private BattleRequestActions() {
    }

    public static void register(IActionRegistrar registrar) {
        for (BattleRequestAction action : BattleRequestAction.values()) {
            registrar.register(ActionRegistration.builder(action.actionId())
                    .nameKey(action.id())
                    .groupModId("cobblemon")
                    .layer(ActionLayer.SCREEN)
                    .targetClass(BattleRequestScreen.GUI_CLASS)
                    .defaultBinding(defaultBinding(action))
                    .build());
        }
    }

    private static DefaultBinding defaultBinding(BattleRequestAction action) {
        return switch (action) {
            case PREV -> DefaultBinding.of(ButtonName.LEFT_BUMPER, TriggerMode.PRESS);
            case NEXT -> DefaultBinding.of(ButtonName.RIGHT_BUMPER, TriggerMode.PRESS);
            case CONFIRM -> DefaultBinding.of(ButtonName.Y, TriggerMode.PRESS);
        };
    }

    /** Maps a callback id back to the action; null when the id is not ours. */
    public static BattleRequestAction byId(String actionId) {
        for (BattleRequestAction action : BattleRequestAction.values()) {
            if (action.actionId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }
}
