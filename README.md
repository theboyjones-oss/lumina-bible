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
- **Gemini key**, two ways (either works):
  1. **Built into the APK:** in the GitHub repo go to Settings → Secrets and variables → Actions → *New repository secret*, name it `GEMINI_API_KEY`, paste your key, then re-run the build. AI voice works straight after install.
  2. **In the app:** Voice → *Gemini API Key*. A key pasted here overrides the built-in one.
  Get a free key at https://aistudio.google.com/apikey.
- Screen stays awake while a chapter is being read aloud.
- Releases are signed with `app/lumina-release.jks` (password `lumina-bible`). Keep it — the same key lets new builds install over old ones. Bump `versionCode` in `app/build.gradle.kts` for each update.
