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

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.query.MarkedQuery;
import org.codelibs.fess.query.QueryMarker;
import org.codelibs.fess.query.QueryProcessor;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fesen.opensearch.index.query.QueryBuilder;

/**
 * Marks the negated bare terms that are bang triggers ({@code !g} parses as {@code NOT g}).
 *
 * <p>When the query holds the bang itself, the clause is dropped: a search that is not
 * redirected (chat, MCP, other APIs) searches the rest of the query. A {@code -g} or {@code NOT g}
 * typed to exclude the word stays an exclusion.</p>
 */
public class BangQueryMarker extends QueryMarker {

    /** The bang definitions. */
    protected BangRegistry bangRegistry;

    /**
     * Default constructor.
     */
    public BangQueryMarker() {
        // Default constructor
    }

    @Override
    public boolean matches(final Occur occur, final Query query) {
        return occur == Occur.MUST_NOT && query instanceof final TermQuery termQuery
                && Constants.DEFAULT_FIELD.equals(termQuery.getTerm().field()) && bangRegistry.contains(termQuery.getTerm().text());
    }

    @Override
    public QueryBuilder execute(final QueryContext context, final MarkedQuery query, final float boost) {
        final String trigger = ((TermQuery) query.getQuery()).getTerm().text();
        if (BangTokens.contains(context.getQueryString(), trigger)) {
            return null;
        }
        return getQueryProcessor().execute(context, query.getQuery(), boost);
    }

    /**
     * Gets the query processor that converts an unmarked clause.
     *
     * @return the query processor
     */
    protected QueryProcessor getQueryProcessor() {
        return ComponentUtil.getQueryProcessor();
    }

    /**
     * Sets the bang definitions.
     *
     * @param bangRegistry the bang definitions
     */
    public void setBangRegistry(final BangRegistry bangRegistry) {
        this.bangRegistry = bangRegistry;
    }
}
