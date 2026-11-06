package com.ifels.cfx.cobblemon;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pseudo key mappings ControlFlex discovers by scanning {@code mc.options.keyMappings}.
 *
 * <p>Defaults are deliberately unbound: a real default key would also fire on real
 * keystrokes and double-fire against Cobblemon's own screen handling.</p>
 */
public final class BattleKeys {

    private static final Map<BattleAction, KeyMapping> KEYS = new EnumMap<>(BattleAction.class);

    private BattleKeys() {
    }

    public static void registerAll(Consumer<KeyMapping> sink) {
        for (BattleAction action : BattleAction.values()) {
            KeyMapping mapping = new KeyMapping(
                    action.id(), InputConstants.UNKNOWN.getValue(), BattleAction.CATEGORY);
            KEYS.put(action, mapping);
            sink.accept(mapping);
        }
    }

    public static KeyMapping get(BattleAction action) {
        return KEYS.get(action);
    }

    public static boolean isEmpty() {
        return KEYS.isEmpty();
    }

    /** Drop queued clicks so a press made outside the battle screen does not fire later. */
    public static void drainAll() {
        for (KeyMapping mapping : KEYS.values()) {
            while (mapping.consumeClick()) {
                // discard
            }
        }
    }
}
