package com.ifels.cfx.cobblemon.neoforge;

import com.ifels.cfx.cobblemon.BattleHud;
import com.ifels.cfx.cobblemon.BattleKeys;
import com.ifels.cfx.cobblemon.BattleTicker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * NeoForge entry point: registers the pseudo key mappings, pumps the ticker and hooks the
 * screen overlay used for the key hints.
 */
@Mod(CfxCobblemonNeoForge.MOD_ID)
@EventBusSubscriber(modid = CfxCobblemonNeoForge.MOD_ID, value = Dist.CLIENT)
public final class CfxCobblemonNeoForge {

    public static final String MOD_ID = "cfx_compat_cobblemon";

    public CfxCobblemonNeoForge(IEventBus modBus) {
        modBus.addListener(this::registerKeys);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        BattleKeys.registerAll(event::register);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BattleTicker.tick();
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        BattleHud.render(event.getGuiGraphics(), event.getScreen());
    }
}
