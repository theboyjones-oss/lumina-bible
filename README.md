# Lumina Bible — Android app

Native Android wrapper (WebView) around the single-file Lumina Bible Study app.

## Build the APK (GitHub Actions)
1. Create a new **private** GitHub repo and upload everything in this folder (keep the folder structure, including `.github/`).
2. The **Build APK** workflow runs automatically on push to `main` (or run it manually from the Actions tab).
3. Open the finished run → **Artifacts** → download `LuminaBible-apk` → unzip → `LuminaBible.apk`.
4. On the phone: open the APK and allow "Install unknown apps" for your browser/file manager.

## What's Android-specific
- App HTML lives in `app/src/main/assets/index.html` — edit it there and push to rebuild.
- `android-bridge.js` swaps the browser speech engine (which doesn't work in Android WebView) for Android's own Text-to-Speech, routes **Export JSON** to the Android save dialog, matches the status bar to the theme, and makes Back close open panels first.
- **Read-aloud voices:** uses Android's built-in neural voices (Speech Services by Google) — free, no API key, no limits, and offline once a voice is downloaded. Voice settings → *More voices…* opens Android's voice download screen; install English (UK) voices for the best quality.
- **AI Meanings:** every verse has a *Meanings* button that opens a ~500-word AI study discussion (Context, Key Words, Meaning, Cross-References, Reflection), which can be read aloud with the device voice. Works free with no key via Pollinations.ai (about one discussion every 45 seconds; the app counts down and retries automatically). Optionally add a Gemini key in Aa → Display → AI Meanings for higher-quality answers; it falls back to the free service if Gemini fails. Discussions are cached on the phone.
- Screen stays awake while a chapter is being read aloud.
- Releases are signed with `app/lumina-release.jks`, whose password exists only in the `KEYSTORE_PASSWORD` repository secret. Keep that password safe — the same key lets new builds install over old ones.
- Latest APK: https://github.com/theboyjones-oss/lumina-bible/releases/latest/download/LuminaBible.apk Bump `versionCode` in `app/build.gradle.kts` for each update.
