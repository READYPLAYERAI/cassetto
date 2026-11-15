# Changelog

All notable changes to Cassetto are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-11-15

### Added

- Sign-in with email and password (mints a Karakeep API key via
  `apiKeys.exchange`) or with an existing API key, with an editable server
  address and a "Test connection" health check.
- Home screen with a paginated bookmark feed, a lists carousel, a profile sheet,
  and filter chips (`All`, `★ Favourites`, `Notes`, `Links`, `PDFs`).
- Server full-text search with ~250 ms debounce, recent searches, top tags from
  loaded bookmarks, newest/oldest sort, paging, and retry/error states.
- Read-only "All lists" tree with parents/children and lists shared with you.
- List detail with a sub-list carousel, the smart-list query shown read-only,
  bookmark rows, and search within a list.
- Bookmark detail and reader that renders Karakeep HTML and Markdown content as
  native blocks: headings, paragraphs, quotes, code, images, tables, task lists
  and embeds. Text size normal/large/XL, a metadata line, read-only tags and an
  attachments list.
- Bookmark actions: favourite toggle, note editing with autosave, on-demand AI
  summary, archive, delete with confirmation, share, copy link and open in
  browser.
- Account screen showing the signed-in user and the server address, with sign
  out and best-effort API-key revocation.
- Material 3 Expressive theme with dynamic (Material You) colour on Android 12+,
  fallback light/dark palettes, and edge-to-edge layout.

### Known limitations

- Creating or saving bookmarks from the app is not available yet; the Home FAB
  shows "Saving bookmarks is coming soon".
- A Settings screen is not available yet; the profile → Settings action shows
  "Settings are coming soon". The server address can only be changed on the
  login screen, so sign out first.
- Tags are read-only; adding or removing tags is not implemented.
- Creating, renaming, deleting or sharing lists is not implemented.
- No offline cache or background sync: a reachable server is required to load
  content.
- Filter chips apply to the bookmarks already loaded in the current feed or
  search results.
- Attachments cannot be previewed in-app; use "Open in browser".
- No share-target or deep links: the app cannot receive URLs from Android's share
  sheet yet.
- English only.

### Security

- The Karakeep API key is stored encrypted with an Android Keystore AES-GCM key;
  the server address is stored in local app storage.
- No analytics, telemetry or crash reporting is sent, and the app only talks to
  the Karakeep server address the user configures.
- `http://` server addresses are allowed for self-hosted/LAN servers; HTTPS is
  recommended.
