/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.plugin.webapp.bangs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BangTokensTest {

    private static List<String> triggers(final String query) {
        return BangTokens.find(query).stream().map(BangTokens.Token::trigger).toList();
    }

    @Test
    void find_standaloneBangs() {
        assertEquals(List.of("g"), triggers("airplane !g"));
        assertEquals(List.of("w"), triggers("!w Ohm's law"));
        assertEquals(List.of("g"), triggers("!g"));
        assertEquals(List.of("w", "g"), triggers("!w ohm !g"));
        // the search wraps the query in parentheses when it adds filters
        assertEquals(List.of("g"), triggers("(airplane !g) label:\"x\""));
    }

    @Test
    void find_ignoresNonBangs() {
        assertEquals(List.of(), triggers("airplane -g"));
        assertEquals(List.of(), triggers("airplane NOT g"));
        assertEquals(List.of(), triggers("airplane!g"));
        assertEquals(List.of(), triggers("title:!g"));
        assertEquals(List.of(), triggers("\"airplane !g\""));
        assertEquals(List.of(), triggers("airplane !"));
        assertEquals(List.of(), triggers(null));
    }

    @Test
    void contains_ignoresCase() {
        assertTrue(BangTokens.contains("airplane !G", "g"));
        assertFalse(BangTokens.contains("airplane -g", "g"));
    }

    @Test
    void remove_tidiesWhitespace() {
        final String query = "  !w   Ohm's  law ";
        assertEquals("Ohm's law", BangTokens.remove(query, BangTokens.find(query).get(0)));
        assertEquals("airplane", BangTokens.remove("airplane !g", BangTokens.find("airplane !g").get(0)));
        assertEquals("", BangTokens.remove("!g", BangTokens.find("!g").get(0)));
    }

    @Test
    void isTrigger() {
        assertTrue(BangTokens.isTrigger("gh"));
        assertFalse(BangTokens.isTrigger("g:h"));
        assertFalse(BangTokens.isTrigger(""));
        assertFalse(BangTokens.isTrigger(null));
    }
}
