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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds bang tokens ({@code !trigger}) in a query string.
 *
 * <p>The query parser folds {@code !x}, {@code -x} and {@code NOT x} into the same negated
 * clause, so the query string is the only place where a bang can be told apart from an
 * exclusion. A bang is a {@code !} followed by the trigger, standing alone: it starts the query
 * or follows whitespace or {@code (}, and ends the query or is followed by whitespace or
 * {@code )} (the search wraps the query in parentheses when it adds filters).</p>
 */
public final class BangTokens {

    private static final Pattern BANG = Pattern.compile("(?<=^|[\\s(])!([^\\s()!\"=:]+)(?=$|[\\s)])");

    private static final Pattern TRIGGER = Pattern.compile("[^\\s()!\"=:]+");

    /**
     * A bang token found in a query string.
     *
     * @param trigger the trigger word, without the leading {@code !}
     * @param start the index of the {@code !}
     * @param end the index after the trigger
     */
    public record Token(String trigger, int start, int end) {
    }

    private BangTokens() {
    }

    /**
     * Finds the bang tokens in a query string, in order.
     *
     * @param query the query string
     * @return the tokens
     */
    public static List<Token> find(final String query) {
        final List<Token> tokens = new ArrayList<>();
        if (query == null) {
            return tokens;
        }
        final Matcher matcher = BANG.matcher(query);
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(1), matcher.start(), matcher.end()));
        }
        return tokens;
    }

    /**
     * Returns whether the query string holds the bang for a trigger, ignoring case.
     *
     * @param query the query string
     * @param trigger the trigger word, without the leading {@code !}
     * @return true if {@code !trigger} appears as a bang
     */
    public static boolean contains(final String query, final String trigger) {
        return find(query).stream().anyMatch(token -> token.trigger().equalsIgnoreCase(trigger));
    }

    /**
     * Removes a token from the query string and tidies the whitespace left behind.
     *
     * @param query the query string
     * @param token the token to remove
     * @return the rest of the query
     */
    public static String remove(final String query, final Token token) {
        return (query.substring(0, token.start()) + " " + query.substring(token.end())).trim().replaceAll("\\s+", " ");
    }

    /**
     * Returns whether a word can be used as a trigger.
     *
     * @param trigger the word
     * @return true if it is a valid trigger
     */
    public static boolean isTrigger(final String trigger) {
        return trigger != null && TRIGGER.matcher(trigger).matches();
    }
}
