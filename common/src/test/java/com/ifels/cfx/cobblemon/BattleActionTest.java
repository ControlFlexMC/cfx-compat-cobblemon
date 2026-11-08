package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleActionTest {

    private static final String COMPAT =
            "/assets/cfx_compat_cobblemon/compat/cfx_compat_cobblemon.json";

    @Test
    void idsAreUniqueAndNamespaced() {
        Set<String> seen = new HashSet<>();
        for (BattleAction action : BattleAction.values()) {
            String id = action.id();
            assertTrue(id.startsWith("key.cfx_compat_cobblemon."), id);
            assertTrue(seen.add(id), "duplicate id: " + id);
        }
    }

    @Test
    void everyActionHasAStableControlFlexId() {
        assertEquals("cfx_compat_cobblemon:battle_fight", BattleAction.ROOT_FIGHT.actionId());
        for (BattleAction action : BattleAction.values()) {
            assertTrue(action.actionId().startsWith("cfx_compat_cobblemon:"), action.actionId());
        }
    }

    @Test
    void slotHelpersStayInRange() {
        assertEquals(BattleAction.MOVE_1, BattleAction.move(1));
        assertEquals(BattleAction.MOVE_4, BattleAction.move(4));
        assertEquals(BattleAction.PARTY_1, BattleAction.party(1));
        assertEquals(BattleAction.PARTY_6, BattleAction.party(6));
    }

    @Test
    void compatConfigNoLongerCarriesScreenKeys() throws Exception {
        String json = read();
        // Actions are declared through onRegisterActions now; a leftover screenKeys section would
        // re-attach them to the global GUI layer this mechanism exists to avoid.
        assertFalse(json.contains("\"screenKeys\""));
        assertTrue(json.contains("\"mod_name\""));
    }

    private static String read() throws Exception {
        try (InputStream in = BattleActionTest.class.getResourceAsStream(COMPAT)) {
            assertNotNull(in, "compat config not on the test classpath: " + COMPAT);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
