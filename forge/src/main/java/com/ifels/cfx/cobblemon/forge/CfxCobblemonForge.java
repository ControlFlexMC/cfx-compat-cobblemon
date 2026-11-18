package com.ifels.cfx.cobblemon.forge;

import com.ifels.cfx.cobblemon.BattleHud;
import com.ifels.cfx.cobblemon.BattleTicker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(CfxCobblemonForge.MOD_ID)
@Mod.EventBusSubscriber(modid = CfxCobblemonForge.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CfxCobblemonForge {

    public static final String MOD_ID = "cfx_compat_cobblemon";

    public CfxCobblemonForge() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BattleTicker.tick();
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        BattleHud.render(event.getGuiGraphics(), event.getScreen());
    }
}
