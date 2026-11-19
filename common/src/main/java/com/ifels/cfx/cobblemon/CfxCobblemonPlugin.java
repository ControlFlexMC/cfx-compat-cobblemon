package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionTrigger;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.ICompatAssetInstaller;
import com.ifels.controlflex.api.IControlFlexPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlFlex SPI entry point: installs the screen compat file, declares the 18 battle actions and
 * the 2 summary-tab actions, and registers the {@code BattleGUI} and {@code Summary} screen
 * handlers once ControlFlex is ready.
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
                            + "will not be listed under Cobblemon → Screen 适配, so the battle and "
                            + "summary actions cannot be rebound per screen",
                    COMPAT_FILE_NAME);
        }
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
        BattleActions.register(registrar);
        SummaryActions.register(registrar);
        LOGGER.info("[cfx-compat-cobblemon] declared {} battle actions and {} summary actions",
                BattleAction.values().length, SummaryAction.values().length);
    }

    @Override
    public void onActionTriggered(ActionTrigger trigger) {
        BattleAction action = BattleActions.byId(trigger.actionId());
        if (action != null) {
            BattleActionHandler.handle(action);
            return;
        }
        SummaryAction summary = SummaryActions.byId(trigger.actionId());
        if (summary != null) {
            SummaryActionHandler.handle(summary);
        }
    }

    @Override
    public void onControlFlexReady() {
        LOGGER.info("[cfx-compat-cobblemon] ControlFlex ready; {} battle actions and {} summary "
                        + "actions available",
                BattleAction.values().length, SummaryAction.values().length);
        BattleScreenHandler.register();
        SummaryScreenHandler.register();
    }
}
