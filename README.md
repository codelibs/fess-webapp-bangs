# Fess Bangs Plugin

[![Java CI with Maven](https://github.com/codelibs/fess-webapp-bangs/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-webapp-bangs/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A [Fess](https://fess.codelibs.org/) WebApp plugin that adds DuckDuckGo-style **bangs**. A bang
sends a search to another site instead of searching Fess:

| Query | Goes to |
| --- | --- |
| `airplane !g` | Google, searching for `airplane` |
| `!w Ohm's law` | Wikipedia, searching for `Ohm's law` |
| `!g` | Google's home page |
| `airplane !unknown` | Fess, as a normal search |

## How it works

- A bang is `!` followed by a trigger, written as a separate word.
- The bang is removed, and the rest of the query is sent to the bang's URL.
- Only the first defined bang in a query counts.
- An unknown bang is searched as usual.
- Triggers are case-insensitive.

The search page and `/api/v2/search` follow the redirect: the API returns `redirect_url`, and the
`bootstrap` theme bundled with Fess navigates to it. Other themes need the same handling.

Other callers cannot redirect: chat, MCP, the v1 and classic JSON APIs, and admin search. For them,
the bang is dropped and the rest of the query is searched.

`-g` and `NOT g` still exclude the word `g`; only `!g` is a bang.

## Requirements

Fess 15.9 or later. The plugin uses the `QueryMarker` and
`SearchRequestParams#redirectTo` extension points.

## Installation

1. Download the plugin JAR from the [Maven repository](https://maven.codelibs.org/release/org/codelibs/fess/fess-webapp-bangs/).
2. Place it in your Fess plugin directory (`app/WEB-INF/plugin`), or install it from **Administration > Plugin** in the Fess admin UI.
3. Restart Fess.

## Configuration

Bangs are defined by the system property `bangs.definitions`. Set it in
`app/WEB-INF/conf/system.properties` or pass it as `-Dfess.system.bangs.definitions=...`.

The value is a whitespace-separated list of `trigger=url` entries. In the URL, `{query}` is replaced
with the URL-encoded rest of the query:

```properties
bangs.definitions=g=https://www.google.com/search?q={query} w=https://en.wikipedia.org/wiki/Special:Search?search={query}
```

- Only absolute `http`/`https` URLs are accepted. An invalid entry is logged and skipped.
- If the same trigger is defined twice, the first definition is used.
- An empty value turns every bang off.
- Changes are picked up without a restart.

When the property is not set, these defaults are used:

| Trigger | Site |
| --- | --- |
| `g` | Google |
| `b` | Bing |
| `ddg` | DuckDuckGo |
| `w` | Wikipedia (English) |
| `gh` | GitHub |

## Build

```bash
mvn clean package
```

## License

Apache License 2.0
