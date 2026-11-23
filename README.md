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
- **AI Voice (Gemini)** uses the current Gemini TTS models (`gemini-3.8-flash-tts`, then `gemini-3.8-flash-lite-tts`, then `gemini-2.5-pro-preview-tts`). Requests go through Android's own networking, not the web view.
- **Gemini key:** paste it in the app under Voice → *Gemini API Key* (free from https://aistudio.google.com/apikey). It is stored only on the phone; no key is ever built into the APK.
- Screen stays awake while a chapter is being read aloud.
- Releases are signed with `app/lumina-release.jks`, whose password exists only in the `KEYSTORE_PASSWORD` repository secret. Keep that password safe — the same key lets new builds install over old ones.
- Latest APK: https://github.com/theboyjones-oss/lumina-bible/releases/latest/download/LuminaBible.apk Bump `versionCode` in `app/build.gradle.kts` for each update.
