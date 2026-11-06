package com.ifels.cfx.cobblemon;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one silent failure mode of this bridge: a pseudo key that exists in
 * {@link BattleAction} but is missing from the bundled compat config would simply never be
 * dispatched by ControlFlex, with no error anywhere.
 */
class BattleActionTest {

    private static final String COMPAT = "/assets/cfx_compat_cobblemon/compat/cfx_compat_cobblemon.json";
    private static final Pattern KEY = Pattern.compile("\"(key\\.cfx_compat_cobblemon\\.[a-z0-9_]+)\"\\s*:");

    @Test
    void everyActionAppearsInTheCompatConfig() throws Exception {
        String json = read();
        Set<String> declared = new HashSet<>();
        Matcher matcher = KEY.matcher(json);
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }

        for (BattleAction action : BattleAction.values()) {
            assertTrue(declared.contains(action.id()),
                    "missing from compat config: " + action.id());
        }
        assertEquals(BattleAction.values().length, declared.size(),
                "compat config declares keys that no action produces");
    }

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
    void slotHelpersStayInRange() {
        assertEquals(BattleAction.MOVE_1, BattleAction.move(1));
        assertEquals(BattleAction.MOVE_4, BattleAction.move(4));
        assertEquals(BattleAction.PARTY_1, BattleAction.party(1));
        assertEquals(BattleAction.PARTY_6, BattleAction.party(6));
    }

    @Test
    void candidateIdsCoverBothAttributionSpellings() {
        String[] ids = BattleAction.ROOT_FIGHT.candidateIds();
        assertEquals(3, ids.length);
        // ControlFlex re-attributes the action to the compat config's mod id on a normal launch.
        assertEquals("cobblemon:key.cfx_compat_cobblemon.battle_fight", ids[0]);
        assertEquals("cfx_compat_cobblemon:key.cfx_compat_cobblemon.battle_fight", ids[1]);
        assertEquals("key.cfx_compat_cobblemon.battle_fight", ids[2]);
    }

    private static String read() throws Exception {
        try (InputStream in = BattleActionTest.class.getResourceAsStream(COMPAT)) {
            assertNotNull(in, "compat config not on the test classpath: " + COMPAT);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
