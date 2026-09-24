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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.query.MarkedQuery;
import org.codelibs.fess.query.QueryProcessor;
import org.codelibs.fess.query.parser.QueryParser;
import org.codelibs.fesen.opensearch.index.query.QueryBuilder;
import org.codelibs.fesen.opensearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BangRequestRewriterTest {

    private BangQueryMarker marker;

    private QueryParser queryParser;

    private BangRequestRewriter rewriter;

    @BeforeEach
    void setUp() {
        final BangRegistry registry = BangRegistryTest.registry(BangRegistry.DEFAULT_DEFINITIONS);
        marker = new BangQueryMarker() {
            @Override
            protected QueryProcessor getQueryProcessor() {
                return new QueryProcessor() {
                    @Override
                    public QueryBuilder execute(final QueryContext context, final Query query, final float boost) {
                        return QueryBuilders.termQuery("excluded", query.toString());
                    }
                };
            }
        };
        marker.setBangRegistry(registry);
        queryParser = new QueryParser();
        queryParser.init();
        queryParser.addMarker(marker);
        rewriter = new BangRequestRewriter() {
            @Override
            protected QueryParser getQueryParser() {
                return queryParser;
            }
        };
        rewriter.setBangRegistry(registry);
        rewriter.setBangQueryMarker(marker);
    }

    private String redirect(final String query) {
        final SearchRequestParams params = new TestSearchRequestParams(query, true);
        assertSame(params, rewriter.rewrite(params));
        return params.getRedirectUrl();
    }

    @Test
    void rewrite_redirectsBangs() {
        assertEquals("https://www.google.com/search?q=airplane", redirect("airplane !g"));
        assertEquals("https://en.wikipedia.org/wiki/Special:Search?search=Ohm%27s%20law", redirect("!w Ohm's law"));
        assertEquals("https://www.google.com/", redirect("!g"));
        assertEquals("https://www.google.com/search?q=airplane", redirect("airplane !G"));
    }

    @Test
    void rewrite_firstKnownBangWins() {
        assertEquals("https://en.wikipedia.org/wiki/Special:Search?search=ohm%20%21g", redirect("!w ohm !g"));
        assertEquals("https://www.google.com/search?q=%21unknown%20ohm", redirect("!unknown ohm !g"));
    }

    @Test
    void rewrite_leavesOtherQueriesAlone() {
        assertNull(redirect("airplane"));
        assertNull(redirect("airplane !unknown"));
        // an exclusion is not a bang
        assertNull(redirect("airplane -g"));
        assertNull(redirect("airplane NOT g"));
        assertNull(redirect("\"airplane !g\""));
        // unparsable
        assertNull(redirect("airplane !g ("));
        assertNull(redirect(""));
    }

    @Test
    void rewrite_callerThatCannotRedirect() {
        final SearchRequestParams params = new TestSearchRequestParams("airplane !g", false);
        assertSame(params, rewriter.rewrite(params));
        assertFalse(params.isRedirected());
    }

    @Test
    void marker_matchesNegatedTriggerTerms() {
        assertTrue(marker.matches(Occur.MUST_NOT, new TermQuery(new Term(Constants.DEFAULT_FIELD, "g"))));
        assertTrue(marker.matches(Occur.MUST_NOT, new TermQuery(new Term(Constants.DEFAULT_FIELD, "GH"))));
        assertFalse(marker.matches(Occur.MUST, new TermQuery(new Term(Constants.DEFAULT_FIELD, "g"))));
        assertFalse(marker.matches(Occur.MUST_NOT, new TermQuery(new Term("title", "g"))));
        assertFalse(marker.matches(Occur.MUST_NOT, new TermQuery(new Term(Constants.DEFAULT_FIELD, "x"))));
        assertFalse(marker.matches(Occur.MUST_NOT, new PhraseQuery(Constants.DEFAULT_FIELD, "g", "h")));
    }

    @Test
    void marker_dropsTheBangButKeepsAnExclusion() {
        final MarkedQuery marked = new MarkedQuery(marker, Occur.MUST_NOT, new TermQuery(new Term(Constants.DEFAULT_FIELD, "g")));
        assertNull(marker.execute(new QueryContext("airplane !g", false), marked, 1.0f));
        assertNull(marker.execute(new QueryContext("(airplane !g) label:\"x\"", false), marked, 1.0f));
        assertEquals(QueryBuilders.termQuery("excluded", "_default:g"),
                marker.execute(new QueryContext("airplane -g", false), marked, 1.0f));
        assertEquals(QueryBuilders.termQuery("excluded", "_default:g"),
                marker.execute(new QueryContext("airplane NOT g", false), marked, 1.0f));
    }
}
