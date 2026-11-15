# Cassetto

An independent native Android client for [Karakeep](https://karakeep.app), the
self-hostable "bookmark everything" app.

Cassetto is a native Android app written in Kotlin with Jetpack Compose and
Material 3 Expressive. It shares no code with the official Karakeep apps and
talks to a Karakeep server over its HTTP API. It is not affiliated with the
Karakeep project.

## Status

`0.1.0` — browse, search and read.

The app is focused on browsing, search and reading. Creating bookmarks from the
app and a Settings screen are not available yet.

## Features

### Sign-in

- Sign in with email and password (the app mints a Karakeep API key via
  `apiKeys.exchange`), or paste an existing API key.
- Editable server address and a "Test connection" health check.
- Credentials are stored locally; the API key is encrypted with an Android
  Keystore AES-GCM key.

### Browse

- Paginated bookmark feed.
- Lists carousel and a profile sheet.
- Filter chips: `All`, `★ Favourites`, `Notes`, `Links`, `PDFs`.

### Search

- Server full-text search with a ~250 ms debounce.
- Recent searches and top tags from the loaded bookmarks.
- Newest/oldest sort, paging, and retry/error states.

### Lists

- Read-only "All lists" tree, including parents/children and lists shared with
  you.
- List detail with a sub-list carousel, the smart-list query shown read-only,
  bookmark rows, and search within a list.

### Reader

- Bookmark detail renders Karakeep HTML and Markdown content as native blocks:
  headings, paragraphs, quotes, code, images, tables, task lists and embeds.
- Text size normal/large/XL, a metadata line, read-only tags and an attachments
  list.

### Bookmark actions

- Favourite toggle.
- Note editing with autosave.
- Request an AI summary on demand.
- Archive, delete (with confirmation), share, copy link, open in browser.

### Account

- Shows the signed-in user and the server address.
- Sign out, with best-effort API-key revocation.

### Theme

- Material 3 Expressive, with dynamic (Material You) colour on Android 12+ and
  fallback light/dark palettes.
- Edge-to-edge layout.

## Requirements

- Android 8.0 or newer (API 26).
- A reachable Karakeep server. HTTPS is recommended; `http://` addresses are
  allowed for self-hosted/LAN servers.
- A Karakeep account or an API key.

## Install

1. Download `Cassetto-v0.1.0.apk` from the
   [Releases page](https://github.com/READYPLAYERAI/cassetto/releases).
2. Sideload it, allowing installs from the chosen browser or file manager when
   Android asks.
3. Open the app, enter the server address and sign in.

## Building

Requires JDK 17+ and an Android SDK with `compileSdk 37`.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

Unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Release builds read signing credentials from `~/keystores/cassetto.properties`
and fall back to debug signing when that file is absent.

## Releasing

Preview first, then publish:

```bash
./scripts/publish-release.sh --dry-run 0.1.0
./scripts/publish-release.sh 0.1.0
```

Prerequisites:

- `~/keystores/cassetto.properties` with the signing credentials.
- A GitHub token at `~/.config/cassetto/github.token`.

The script builds the release APK, verifies the signature, bumps `versionCode`
and `versionName`, tags the release, pushes `master` plus the tag, and creates a
GitHub Release with `Cassetto-v<version>.apk`.

## Known limitations

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
- Attachments cannot be previewed in-app (the app says so); use "Open in
  browser".
- No share-target or deep links: the app cannot receive URLs from Android's
  share sheet yet.
- English only.

## Privacy

- No analytics, no telemetry, no crash reporting and no third-party servers.
- The app only talks to the Karakeep server address you configure.
- The API key is stored encrypted with an Android Keystore key; the server
  address is stored in local app storage.
- `http://` server addresses are allowed for self-hosted/LAN servers; HTTPS is
  recommended.

## Licence

AGPL-3.0 — see [LICENSE](LICENSE).
