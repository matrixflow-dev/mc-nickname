package jp.matrixflow.nickname;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NameRulesTest {

    private final NameRules rules = new NameRules(16, List.of("アイ"));
    private final List<String> taken = List.of("Sum_shi", ".tdualdir", "たろう");

    @Test
    void acceptsJapaneseAndAscii() {
        assertEquals("ゆう", rules.check("ゆう", "tdual", taken).nickname());
        assertEquals("田本_2", rules.check(" 田本_2 ", "tdual", taken).nickname());
        assertEquals("ゆー・たもと", rules.check("ゆー・たもと", "tdual", taken).nickname());
    }

    @Test
    void ownAccountNameResets() {
        NameRules.Verdict v = rules.check("TDUAL", "tdual", taken);
        assertFalse(v.refused());
        assertNull(v.nickname());
    }

    @Test
    void refusesTakenAndReservedNames() {
        assertTrue(rules.check("sum_shi", "tdual", taken).refused());
        assertTrue(rules.check("tdualdir", "tdual", taken).refused());
        assertTrue(rules.check("たろう", "tdual", taken).refused());
        assertTrue(rules.check("アイ", "tdual", taken).refused());
    }

    @Test
    void refusesBadNames() {
        assertTrue(rules.check("", "tdual", taken).refused());
        assertTrue(rules.check("   ", "tdual", taken).refused());
        assertTrue(rules.check("ゆう たもと", "tdual", taken).refused());
        assertTrue(rules.check("§cRed", "tdual", taken).refused());
        assertTrue(rules.check("<b>x", "tdual", taken).refused());
        assertTrue(rules.check(".ゆう", "tdual", taken).refused());
        assertTrue(rules.check("あ".repeat(17), "tdual", taken).refused());
        assertFalse(rules.check("あ".repeat(16), "tdual", taken).refused());
        assertTrue(rules.check("😀", "tdual", taken).refused());
    }
}
