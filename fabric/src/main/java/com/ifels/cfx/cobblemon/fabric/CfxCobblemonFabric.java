package com.ifels.cfx.cobblemon.fabric;

import com.ifels.cfx.cobblemon.BattleHud;
import com.ifels.cfx.cobblemon.BattleKeys;
import com.ifels.cfx.cobblemon.BattleTicker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

/**
 * Fabric entry point: registers the pseudo key mappings, pumps the ticker and hooks the
 * screen overlay used for the key hints.
 */
public final class CfxCobblemonFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BattleKeys.registerAll(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.END_CLIENT_TICK.register(client -> BattleTicker.tick());
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register(
                        (rendered, graphics, mouseX, mouseY, tickDelta) ->
                                BattleHud.render(graphics, rendered)));
    }
}
