# Music Mania — Kotlin + Jetpack Compose

A retro-styled Android music explorer using the Apple iTunes Search API and Media3. The existing Compose UI/animations are retained; the data/playback layer is live.

## Stack
- Kotlin 2.0.20
- Jetpack Compose
- Retrofit + kotlinx.serialization
- Apple iTunes Search API (music search, album/artist lookup, artwork, preview URLs)
- Media3 ExoPlayer + MediaSessionService
- SharedPreferences + kotlinx.serialization for device-local persistence

## What is live
- Home music is fetched from Apple at runtime.
- Search is fetched from Apple at runtime with debounce.
- Album/artist cards fetch their tracks from Apple when selected.
- Only tracks with a real preview URL are shown as playable.
- 30-second Apple previews play through Media3.
- Playback is hosted by MediaSessionService for background/lock-screen controls.

## What is remembered on the device
- Likes, saved songs, My Mix
- Listening log
- Last queue and selected track
- Installation profile identifier

This is device-local persistence. Cross-device accounts would require authentication/cloud storage.

## Open in Android Studio
Open the **MusicMania** folder itself — the folder containing `settings.gradle.kts` and `app/`. Do not open an outer ZIP/extraction folder.

Use a JDK compatible with Android Studio/AGP (JDK 17 is the safest choice for this project). Let Android Studio download the configured Gradle 8.7 distribution and Android SDK components if prompted.

## Playback limitation
The iTunes Search API exposes preview audio rather than unrestricted full-song streaming. Music Mania intentionally uses those previews.
