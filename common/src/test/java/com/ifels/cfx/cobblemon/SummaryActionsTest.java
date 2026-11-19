package com.ifels.cfx.cobblemon;

import com.ifels.controlflex.api.ActionLayer;
import com.ifels.controlflex.api.ActionRegistration;
import com.ifels.controlflex.api.DefaultBinding;
import com.ifels.controlflex.api.IActionRegistrar;
import com.ifels.controlflex.api.IControlFlexAction;
import com.ifels.controlflex.api.TriggerMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryActionsTest {

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
    void everySummaryActionIsScopedToTheSummaryScreen() {
        Recorder recorder = new Recorder();
        SummaryActions.register(recorder);

        assertEquals(SummaryAction.values().length, recorder.registrations.size());
        for (ActionRegistration registration : recorder.registrations) {
            assertTrue(registration.actionId().startsWith("cfx_compat_cobblemon:summary_"),
                    registration.actionId());
            assertEquals(EnumSet.of(ActionLayer.SCREEN), registration.layers());
            assertEquals(List.of(SummaryTabs.GUI_CLASS), registration.targetClasses());
            assertEquals("cobblemon", registration.groupModId());
        }
    }

    @Test
    void bumpersSwitchTabs() {
        Recorder recorder = new Recorder();
        SummaryActions.register(recorder);

        assertEquals("leftBumper", binding(recorder, SummaryAction.PREV).button());
        assertEquals("rightBumper", binding(recorder, SummaryAction.NEXT).button());
        for (SummaryAction action : SummaryAction.values()) {
            assertEquals(TriggerMode.PRESS, binding(recorder, action).mode(), action.name());
        }
    }

    @Test
    void idsRoundTripBackToActions() {
        for (SummaryAction action : SummaryAction.values()) {
            assertNotNull(SummaryActions.byId(action.actionId()), action.actionId());
        }
        assertNull(SummaryActions.byId("cfx_compat_cobblemon:nope"));
        // The battle screen's ids must not be claimed by the summary table.
        assertNull(SummaryActions.byId("cfx_compat_cobblemon:battle_prev"));
    }

    private static DefaultBinding binding(Recorder recorder, SummaryAction action) {
        ActionRegistration registration = recorder.registrations.stream()
                .filter(r -> r.actionId().equals(action.actionId()))
                .findFirst()
                .orElseThrow();
        DefaultBinding binding = registration.defaultBinding();
        assertNotNull(binding, action.name());
        return binding;
    }
}
