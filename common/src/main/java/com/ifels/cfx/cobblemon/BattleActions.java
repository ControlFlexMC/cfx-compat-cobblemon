package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.TriggerMode;

/**
 * Declares the battle actions to ControlFlex.
 *
 * <p>No {@code KeyMapping} is created: these ids exist only inside ControlFlex, appear under the
 * Cobblemon adaptation card, and never touch the vanilla controls screen. The four defaults are
 * the ones a controller player expects out of the box; the other fourteen start unbound.</p>
 *
 * @since 0.1.0
 */
public final class BattleActions {

    private BattleActions() {
    }

    public static void register(IActionRegistrar registrar) {
        for (BattleAction action : BattleAction.values()) {
            ActionRegistration.Builder builder = ActionRegistration.builder(action.actionId())
                    .nameKey(action.id())
                    .groupModId("cobblemon")
                    .layer(ActionLayer.SCREEN)
                    .targetClass(BattleScreen.GUI_CLASS);
            DefaultBinding defaultBinding = defaultBinding(action);
            if (defaultBinding != null) {
                builder.defaultBinding(defaultBinding);
            }
            registrar.register(builder.build());
        }
    }

    private static DefaultBinding defaultBinding(BattleAction action) {
        return switch (action) {
            case PREV -> DefaultBinding.of(
                    com.ifels.controlflex.api.ButtonName.LEFT_BUMPER, TriggerMode.PRESS);
            case NEXT -> DefaultBinding.of(
                    com.ifels.controlflex.api.ButtonName.RIGHT_BUMPER, TriggerMode.PRESS);
            case LOG_UP -> DefaultBinding.of(
                    com.ifels.controlflex.api.ButtonName.RIGHT_STICK_UP, TriggerMode.PRESS);
            case LOG_DOWN -> DefaultBinding.of(
                    com.ifels.controlflex.api.ButtonName.RIGHT_STICK_DOWN, TriggerMode.PRESS);
            default -> null;
        };
    }

    /** Maps a callback id back to the action; null when the id is not ours. */
    public static BattleAction byId(String actionId) {
        for (BattleAction action : BattleAction.values()) {
            if (action.actionId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }
}
