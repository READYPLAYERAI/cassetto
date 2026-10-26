# Cassetto

A native Android client for [Karakeep](https://karakeep.app) — the self-hostable
"bookmark everything" app — built with Kotlin and Jetpack Compose, following
Material 3 Expressive.

Cassetto is an independent client: it shares no code with the official Karakeep
apps and talks to a Karakeep server over its public HTTP API.

## Status

Early development. Login is the first milestone.

## Building

Requires JDK 17+ and the Android SDK (compileSdk 37).

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Licence

AGPL-3.0 — see [LICENSE](LICENSE).
