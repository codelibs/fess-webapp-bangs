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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Holds the bang definitions: a trigger word and the URL a search is sent to.
 *
 * <p>The definitions are read from the system property {@value #DEFINITIONS_KEY}
 * ({@code conf/system.properties}, or {@code -Dfess.system.bangs.definitions}). The value is a
 * whitespace-separated list of {@code trigger=url} entries, and {@code {query}} in the URL is
 * replaced with the rest of the query. An empty value turns every bang off. Without the property
 * the {@link #DEFAULT_DEFINITIONS defaults} are used.</p>
 */
public class BangRegistry {

    private static final Logger logger = LogManager.getLogger(BangRegistry.class);

    /** The system property holding the bang definitions. */
    public static final String DEFINITIONS_KEY = "bangs.definitions";

    /** The placeholder replaced with the rest of the query. */
    public static final String QUERY_PLACEHOLDER = "{query}";

    /** The definitions used when {@value #DEFINITIONS_KEY} is not set. */
    public static final String DEFAULT_DEFINITIONS = String.join("\n", //
            "g=https://www.google.com/search?q={query}", //
            "b=https://www.bing.com/search?q={query}", //
            "ddg=https://duckduckgo.com/?q={query}", //
            "w=https://en.wikipedia.org/wiki/Special:Search?search={query}", //
            "gh=https://github.com/search?q={query}");

    /** The parsed definitions, keyed by the raw value they were parsed from. */
    private volatile Definitions definitions = new Definitions(null, Collections.emptyMap());

    private record Definitions(String raw, Map<String, String> templates) {
    }

    /**
     * Default constructor.
     */
    public BangRegistry() {
        // Default constructor
    }

    /**
     * Returns whether the trigger is a defined bang. Triggers are case-insensitive.
     *
     * @param trigger the trigger word, without the leading {@code !}
     * @return true if the trigger is defined
     */
    public boolean contains(final String trigger) {
        return trigger != null && getTemplates().containsKey(trigger.toLowerCase(Locale.ROOT));
    }

    /**
     * Builds the URL a bang sends a search to.
     *
     * @param trigger the trigger word, without the leading {@code !}
     * @param rest the rest of the query; blank sends the search to the site's home page
     * @return the URL, or null if the trigger is not defined
     */
    public String buildUrl(final String trigger, final String rest) {
        final String template = trigger == null ? null : getTemplates().get(trigger.toLowerCase(Locale.ROOT));
        if (template == null) {
            return null;
        }
        if (StringUtil.isBlank(rest)) {
            final URI uri = URI.create(template.replace(QUERY_PLACEHOLDER, ""));
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/";
        }
        return template.replace(QUERY_PLACEHOLDER, URLEncoder.encode(rest.trim(), StandardCharsets.UTF_8).replace("+", "%20"));
    }

    /**
     * Gets the bang URL templates keyed by lower-case trigger, parsing the definitions again
     * only when they changed.
     *
     * @return the templates
     */
    protected Map<String, String> getTemplates() {
        final String raw = loadDefinitions();
        Definitions current = definitions;
        if (!raw.equals(current.raw())) {
            current = new Definitions(raw, parse(raw));
            definitions = current;
        }
        return current.templates();
    }

    /**
     * Loads the raw definitions.
     *
     * @return the raw definitions, never null
     */
    protected String loadDefinitions() {
        final String value = ComponentUtil.getFessConfig().getSystemProperty(DEFINITIONS_KEY, DEFAULT_DEFINITIONS);
        return value == null ? DEFAULT_DEFINITIONS : value;
    }

    /**
     * Parses whitespace-separated {@code trigger=url} entries. An invalid entry is logged and skipped.
     *
     * @param raw the raw definitions
     * @return the templates keyed by lower-case trigger
     */
    protected Map<String, String> parse(final String raw) {
        final Map<String, String> templates = new LinkedHashMap<>();
        for (final String entry : raw.trim().split("\\s+")) {
            if (entry.isEmpty()) {
                continue;
            }
            final int pos = entry.indexOf('=');
            final String trigger = pos > 0 ? entry.substring(0, pos).toLowerCase(Locale.ROOT) : null;
            final String template = pos > 0 ? entry.substring(pos + 1) : null;
            if (trigger == null || !BangTokens.isTrigger(trigger) || !isHttpTemplate(template)) {
                logger.warn("Ignoring an invalid bang definition in {}: {}", DEFINITIONS_KEY, entry);
                continue;
            }
            templates.putIfAbsent(trigger, template);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Loaded bangs: {}", templates.keySet());
        }
        return Collections.unmodifiableMap(templates);
    }

    /**
     * Checks that a template is an absolute http(s) URL with a host.
     *
     * @param template the URL template
     * @return true if it is valid
     */
    protected boolean isHttpTemplate(final String template) {
        if (StringUtil.isBlank(template)) {
            return false;
        }
        try {
            final URI uri = URI.create(template.replace(QUERY_PLACEHOLDER, "q"));
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && StringUtil.isNotBlank(uri.getHost());
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
