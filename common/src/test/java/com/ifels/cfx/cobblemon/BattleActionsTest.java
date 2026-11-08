package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexAction;
import com.ifels.controlflex.api.TriggerMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleActionsTest {

    private static final class Recorder implements IActionRegistrar {
        private final List<ActionRegistration> registrations = new ArrayList<>();

        @Override
        public IControlFlexAction register(ActionRegistration registration) {
            registrations.add(registration);
            return new IControlFlexAction() {
                @Override
                public String getActionId() {
                    return registration.actionId();
                }

                @Override
                public boolean isActive() {
                    return false;
                }
            };
        }
    }

    @Test
    void everyActionIsDeclaredAsAScreenActionForBattleGui() {
        Recorder recorder = new Recorder();
        BattleActions.register(recorder);

        assertEquals(BattleAction.values().length, recorder.registrations.size());
        for (ActionRegistration registration : recorder.registrations) {
            assertTrue(registration.actionId().startsWith("cfx_compat_cobblemon:"),
                    registration.actionId());
            assertEquals(java.util.EnumSet.of(ActionLayer.SCREEN), registration.layers());
            assertEquals(List.of(BattleScreen.GUI_CLASS), registration.targetClasses());
            assertEquals("cobblemon", registration.groupModId());
        }
    }

    @Test
    void onlyTheFourNavigationActionsCarryDefaults() {
        Recorder recorder = new Recorder();
        BattleActions.register(recorder);

        assertEquals("leftBumper", registration(recorder, BattleAction.PREV).defaultBinding().button());
        assertEquals(TriggerMode.PRESS,
                registration(recorder, BattleAction.PREV).defaultBinding().mode());
        assertEquals("rightBumper", registration(recorder, BattleAction.NEXT).defaultBinding().button());
        assertEquals("rightStickUp", registration(recorder, BattleAction.LOG_UP).defaultBinding().button());
        assertEquals("rightStickDown",
                registration(recorder, BattleAction.LOG_DOWN).defaultBinding().button());
        assertNull(registration(recorder, BattleAction.ROOT_FIGHT).defaultBinding());
        assertNull(registration(recorder, BattleAction.PARTY_6).defaultBinding());
    }

    private static ActionRegistration registration(Recorder recorder, BattleAction action) {
        return recorder.registrations.stream()
                .filter(r -> r.actionId().equals(action.actionId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void idsRoundTripBackToActions() {
        for (BattleAction action : BattleAction.values()) {
            assertNotNull(BattleActions.byId(action.actionId()), action.actionId());
        }
        assertNull(BattleActions.byId("cfx_compat_cobblemon:nope"));
    }
}
