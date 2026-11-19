package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionTrigger;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.ICompatAssetInstaller;
import com.ifels.controlflex.api.IControlFlexPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlFlex SPI entry point.
 *
 * <p>Installs the bundled compat config, which registers Cobblemon's screens with the Cobblemon
 * card: {@code BattleGUI}, {@code Summary}, {@code BattleConfigureGUI} and the interaction wheel.
 * The compat file owns those entries under 模组适配 → Cobblemon → Screen 适配, and ControlFlex
 * fills each page from the actions declared in {@link #onRegisterActions}. Without the file a
 * screen has no owner, so its page cannot be opened and its actions have nowhere to be edited.</p>
 */
public final class CfxCobblemonPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(BattleAction.MOD_ID);

    public static final String COMPAT_FILE_NAME = "cfx_compat_cobblemon.json";
    public static final String COMPAT_RESOURCE =
            "/assets/cfx_compat_cobblemon/compat/" + COMPAT_FILE_NAME;

    @Override
    public String getModId() {
        return BattleAction.MOD_ID;
    }

    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        if (!installer.install(COMPAT_RESOURCE, COMPAT_FILE_NAME)) {
            LOGGER.warn("[cfx-compat-cobblemon] failed to install {} — the Cobblemon screen pages "
                            + "will not be listed under Cobblemon → Screen 适配, so the battle, "
                            + "request and summary actions cannot be rebound per screen",
                    COMPAT_FILE_NAME);
        }
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
        BattleActions.register(registrar);
        BattleRequestActions.register(registrar);
        SummaryActions.register(registrar);
        LOGGER.info("[cfx-compat-cobblemon] declared {} battle actions, {} request actions and "
                        + "{} summary actions",
                BattleAction.values().length, BattleRequestAction.values().length,
                SummaryAction.values().length);
    }

    @Override
    public void onActionTriggered(ActionTrigger trigger) {
        BattleAction action = BattleActions.byId(trigger.actionId());
        if (action != null) {
            BattleActionHandler.handle(action);
            return;
        }
        BattleRequestAction request = BattleRequestActions.byId(trigger.actionId());
        if (request != null) {
            BattleRequestHandler.handle(request);
            return;
        }
        SummaryAction summary = SummaryActions.byId(trigger.actionId());
        if (summary != null) {
            SummaryActionHandler.handle(summary);
        }
    }

    @Override
    public void onControlFlexReady() {
        LOGGER.info("[cfx-compat-cobblemon] ControlFlex ready; {} battle actions, {} request "
                        + "actions and {} summary actions available",
                BattleAction.values().length, BattleRequestAction.values().length,
                SummaryAction.values().length);
        // The only point where ControlFlex has certainly published its API registries.
        BattleScreenHandler.register();
        BattleRequestScreenHandler.register();
        SummaryScreenHandler.register();
    }
}
