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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BangRegistryTest {

    static BangRegistry registry(final String definitions) {
        return new BangRegistry() {
            @Override
            protected String loadDefinitions() {
                return definitions;
            }
        };
    }

    @Test
    void defaults() {
        final BangRegistry registry = registry(BangRegistry.DEFAULT_DEFINITIONS);
        for (final String trigger : new String[] { "g", "b", "ddg", "w", "gh", "G" }) {
            assertTrue(registry.contains(trigger), trigger);
        }
        assertFalse(registry.contains("x"));
        assertFalse(registry.contains(null));
    }

    @Test
    void buildUrl_encodesTheRest() {
        final BangRegistry registry = registry(BangRegistry.DEFAULT_DEFINITIONS);
        assertEquals("https://www.google.com/search?q=airplane", registry.buildUrl("g", "airplane"));
        assertEquals("https://en.wikipedia.org/wiki/Special:Search?search=Ohm%27s%20law", registry.buildUrl("W", "Ohm's law"));
        assertEquals("https://www.google.com/search?q=%E6%A4%9C%E7%B4%A2%20%26%20x%3D1", registry.buildUrl("g", "検索 & x=1"));
        assertNull(registry.buildUrl("x", "airplane"));
    }

    @Test
    void buildUrl_homePageWhenNothingIsLeft() {
        final BangRegistry registry = registry("g=https://www.google.com/search?q={query} p=http://localhost:8080/find/{query}");
        assertEquals("https://www.google.com/", registry.buildUrl("g", ""));
        assertEquals("http://localhost:8080/", registry.buildUrl("p", "  "));
        assertEquals("http://localhost:8080/find/a%20b", registry.buildUrl("p", "a b"));
    }

    @Test
    void parse_skipsInvalidEntries() {
        final BangRegistry registry = registry("""
                g=https://www.google.com/search?q={query}
                bad
                j=javascript:alert(1)
                r=/relative?q={query}
                =https://example.com/?q={query}
                G=https://example.com/?q={query}
                """);
        assertTrue(registry.contains("g"));
        assertFalse(registry.contains("bad"));
        assertFalse(registry.contains("j"));
        assertFalse(registry.contains("r"));
        // the first definition of a trigger wins
        assertEquals("https://www.google.com/search?q=x", registry.buildUrl("g", "x"));
    }

    @Test
    void emptyValueTurnsBangsOff() {
        assertFalse(registry("").contains("g"));
    }

    @Test
    void reloadsWhenTheDefinitionsChange() {
        final String[] value = { "g=https://www.google.com/search?q={query}" };
        final BangRegistry registry = new BangRegistry() {
            @Override
            protected String loadDefinitions() {
                return value[0];
            }
        };
        assertTrue(registry.contains("g"));
        value[0] = "w=https://en.wikipedia.org/wiki/Special:Search?search={query}";
        assertFalse(registry.contains("g"));
        assertTrue(registry.contains("w"));
    }
}
