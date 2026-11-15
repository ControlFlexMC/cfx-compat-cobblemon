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
 * <p>Installs the bundled compat config, whose only job now is to register
 * {@code BattleGUI} as a known screen of the Cobblemon card: the compat file owns the entry in
 * 模组适配 → Cobblemon → Screen 适配, and ControlFlex fills that page from the actions declared
 * in {@link #onRegisterActions}. Without the file the screen has no owner, so the page cannot be
 * opened and the battle actions have nowhere to be edited.</p>
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
            LOGGER.warn("[cfx-compat-cobblemon] failed to install {} — BattleGUI will not be listed "
                            + "under Cobblemon → Screen 适配, so the {} battle actions cannot be "
                            + "seen or rebound in-game",
                    COMPAT_FILE_NAME, BattleAction.values().length);
        }
    }

    @Override
    public void onRegisterActions(IActionRegistrar registrar) {
        BattleActions.register(registrar);
        LOGGER.info("[cfx-compat-cobblemon] declared {} battle actions", BattleAction.values().length);
    }

    @Override
    public void onActionTriggered(ActionTrigger trigger) {
        BattleAction action = BattleActions.byId(trigger.actionId());
        if (action != null) {
            BattleActionHandler.handle(action);
        }
    }

    @Override
    public void onControlFlexReady() {
        LOGGER.info("[cfx-compat-cobblemon] ControlFlex ready; {} battle actions available",
                BattleAction.values().length);
        // The only point where ControlFlex has certainly published its API registries.
        BattleScreenHandler.register();
    }
}
