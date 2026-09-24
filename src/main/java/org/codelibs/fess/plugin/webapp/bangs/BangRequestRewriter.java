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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.exception.QueryParseException;
import org.codelibs.fess.helper.SearchHelper.SearchRequestParamsRewriter;
import org.codelibs.fess.query.MarkedQuery;
import org.codelibs.fess.query.parser.QueryParser;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Sends a search that holds a bang to the bang's site: {@code airplane !g} goes to Google with
 * {@code airplane}, and {@code !g} alone goes to Google's home page.
 *
 * <p>Only the first bang the query parser marked counts; an unknown bang is searched as usual.
 * Callers that cannot redirect (chat, MCP, other APIs) are left alone, and the
 * {@link BangQueryMarker} drops the bang clause for them.</p>
 */
public class BangRequestRewriter implements SearchRequestParamsRewriter {

    private static final Logger logger = LogManager.getLogger(BangRequestRewriter.class);

    /** The bang definitions. */
    protected BangRegistry bangRegistry;

    /** The marker whose marked clauses are bangs. */
    protected BangQueryMarker bangQueryMarker;

    /**
     * Default constructor.
     */
    public BangRequestRewriter() {
        // Default constructor
    }

    /**
     * Registers this rewriter with the search helper.
     */
    public void register() {
        ComponentUtil.getSearchHelper().addRewriter(this);
    }

    @Override
    public SearchRequestParams rewrite(final SearchRequestParams params) {
        if (!params.isRedirectable() || params.isRedirected()) {
            return params;
        }
        final String query = params.getQuery();
        if (StringUtil.isBlank(query)) {
            return params;
        }
        final List<BangTokens.Token> tokens = BangTokens.find(query).stream().filter(t -> bangRegistry.contains(t.trigger())).toList();
        if (tokens.isEmpty()) {
            return params;
        }
        final Set<String> markedTriggers = findMarkedTriggers(query);
        for (final BangTokens.Token token : tokens) {
            if (markedTriggers.contains(token.trigger().toLowerCase(Locale.ROOT))) {
                final String url = bangRegistry.buildUrl(token.trigger(), BangTokens.remove(query, token));
                if (logger.isDebugEnabled()) {
                    logger.debug("Redirecting a bang search: query={}, url={}", query, url);
                }
                params.redirectTo(url);
                return params;
            }
        }
        return params;
    }

    /**
     * Parses the query and collects the triggers of the clauses {@link #bangQueryMarker} marked.
     *
     * @param query the query string
     * @return the marked triggers in lower case, empty if the query cannot be parsed
     */
    protected Set<String> findMarkedTriggers(final String query) {
        final Query parsed;
        try {
            parsed = getQueryParser().parse(new QueryContext(query, false).getQueryString());
        } catch (final QueryParseException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not a bang search, the query cannot be parsed: {}", query, e);
            }
            return Set.of();
        }
        return MarkedQuery.collect(parsed)
                .stream()
                .filter(q -> q.getMarker() == bangQueryMarker && q.getQuery() instanceof TermQuery)
                .map(q -> ((TermQuery) q.getQuery()).getTerm().text().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    /**
     * Gets the query parser.
     *
     * @return the query parser
     */
    protected QueryParser getQueryParser() {
        return ComponentUtil.getQueryParser();
    }

    /**
     * Sets the bang definitions.
     *
     * @param bangRegistry the bang definitions
     */
    public void setBangRegistry(final BangRegistry bangRegistry) {
        this.bangRegistry = bangRegistry;
    }

    /**
     * Sets the marker whose marked clauses are bangs.
     *
     * @param bangQueryMarker the marker
     */
    public void setBangQueryMarker(final BangQueryMarker bangQueryMarker) {
        this.bangQueryMarker = bangQueryMarker;
    }
}
