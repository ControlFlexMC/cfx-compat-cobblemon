package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ICompatAssetInstaller;
import com.ifels.controlflex.api.IControlFlexPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlFlex SPI entry point.
 *
 * <p>Installs the bundled compat config, which moves the pseudo key mappings into
 * ControlFlex's GUI layer. Without it they stay on the MAIN layer and never fire while a
 * screen is open.</p>
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
            LOGGER.warn("[cfx-compat-cobblemon] failed to install {} — the {} battle actions "
                            + "will never fire while the battle screen is open",
                    COMPAT_FILE_NAME, BattleAction.values().length);
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
