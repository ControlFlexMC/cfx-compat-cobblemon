package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.DefaultBinding;
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

class BattleRequestActionsTest {

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
    void everyRequestActionIsScopedToTheConfigureScreen() {
        Recorder recorder = new Recorder();
        BattleRequestActions.register(recorder);

        assertEquals(BattleRequestAction.values().length, recorder.registrations.size());
        for (ActionRegistration registration : recorder.registrations) {
            assertTrue(registration.actionId().startsWith("cfx_compat_cobblemon:battle_request_"),
                    registration.actionId());
            assertEquals(java.util.EnumSet.of(ActionLayer.SCREEN), registration.layers());
            assertEquals(List.of(BattleRequestScreen.GUI_CLASS), registration.targetClasses());
            assertEquals("cobblemon", registration.groupModId());
        }
    }

    @Test
    void bumpersStepTheFormatDpadStepsTheRuleAndYPressesTheButton() {
        Recorder recorder = new Recorder();
        BattleRequestActions.register(recorder);

        assertEquals("leftBumper", binding(recorder, BattleRequestAction.PREV).button());
        assertEquals("rightBumper", binding(recorder, BattleRequestAction.NEXT).button());
        assertEquals("dpadLeft", binding(recorder, BattleRequestAction.LEVEL_PREV).button());
        assertEquals("dpadRight", binding(recorder, BattleRequestAction.LEVEL_NEXT).button());
        assertEquals("buttonY", binding(recorder, BattleRequestAction.CONFIRM).button());
        for (BattleRequestAction action : BattleRequestAction.values()) {
            assertEquals(TriggerMode.PRESS, binding(recorder, action).mode(), action.name());
        }
    }

    @Test
    void idsRoundTripBackToActions() {
        for (BattleRequestAction action : BattleRequestAction.values()) {
            assertNotNull(BattleRequestActions.byId(action.actionId()), action.actionId());
        }
        assertNull(BattleRequestActions.byId("cfx_compat_cobblemon:nope"));
        // The battle screen's ids must not be claimed by the request table.
        assertNull(BattleRequestActions.byId("cfx_compat_cobblemon:battle_prev"));
    }

    private static DefaultBinding binding(Recorder recorder, BattleRequestAction action) {
        ActionRegistration registration = recorder.registrations.stream()
                .filter(r -> r.actionId().equals(action.actionId()))
                .findFirst()
                .orElseThrow();
        DefaultBinding binding = registration.defaultBinding();
        assertNotNull(binding, action.name());
        return binding;
    }
}
