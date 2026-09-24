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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.codelibs.fess.entity.FacetInfo;
import org.codelibs.fess.entity.GeoInfo;
import org.codelibs.fess.entity.HighlightInfo;
import org.codelibs.fess.entity.SearchRequestParams;

/** Minimal search request parameters for tests. */
class TestSearchRequestParams extends SearchRequestParams {

    private final String query;

    TestSearchRequestParams(final String query, final boolean redirectable) {
        this.query = query;
        if (redirectable) {
            enableRedirect();
        }
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public Map<String, String[]> getFields() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String[]> getConditions() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getLanguages() {
        return new String[0];
    }

    @Override
    public GeoInfo getGeoInfo() {
        return null;
    }

    @Override
    public FacetInfo getFacetInfo() {
        return null;
    }

    @Override
    public HighlightInfo getHighlightInfo() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public int getStartPosition() {
        return 0;
    }

    @Override
    public int getPageSize() {
        return 10;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public String[] getExtraQueries() {
        return new String[0];
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public Locale getLocale() {
        return Locale.ROOT;
    }

    @Override
    public SearchRequestType getType() {
        return SearchRequestType.SEARCH;
    }

    @Override
    public String getSimilarDocHash() {
        return null;
    }
}
