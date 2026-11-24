package com.lumina.biblestudy;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends ComponentActivity {

    private static final String START_URL = "https://appassets.androidplatform.net/assets/index.html";

    private WebView webView;
    private FrameLayout root;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    private ValueCallback<Uri[]> fileCallback;
    private ActivityResultLauncher<Intent> filePicker;
    private ActivityResultLauncher<String> saveDoc;
    private String pendingSaveText;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#111827"));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0b1120"));
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Keep content clear of the status bar, navigation bar and keyboard
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        setupLaunchers();
        setupTts();
        setupWebView();
        setupBack();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(START_URL);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    filePicker.launch(Intent.createChooser(i, "Import study backup"));
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        webView.addJavascriptInterface(new AppBridge(), "AndroidBridge");
    }

    private void setupLaunchers() {
        filePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (fileCallback == null) return;
            Uri[] uris = null;
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                uris = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(uris);
            fileCallback = null;
        });

        saveDoc = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            String text = pendingSaveText;
            pendingSaveText = null;
            if (uri == null || text == null) return;
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) os.write(text.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not save backup", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript("window.__luminaBack ? window.__luminaBack() : 'exit'", value -> {
                    if (value == null || value.contains("exit")) {
                        moveTaskToBack(true);
                    }
                });
            }
        });
    }

    /* ---------------- Native Text-to-Speech ---------------- */

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            tts.setLanguage(Locale.US);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { emit(id, "start", ""); }
                @Override public void onDone(String id) { emit(id, "end", ""); }
                @Override public void onError(String id) { emit(id, "error", "synthesis-failed"); }
                @Override public void onError(String id, int code) { emit(id, "error", "synthesis-failed"); }
                @Override public void onStop(String id, boolean interrupted) { emit(id, "error", "interrupted"); }
            });
            ttsReady = true;
            emit("", "voices", "");
        });
    }

    private void emit(String id, String type, String code) {
        final String js = "window.__luminaTts && window.__luminaTts("
                + JSONObject.quote(id) + "," + JSONObject.quote(type) + "," + JSONObject.quote(code) + ")";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    private class TtsBridge {
        @JavascriptInterface
        public void speak(String id, String text, double rate, double pitch, String voiceName, String lang) {
            if (!ttsReady) {
                emit(id, "error", "not-ready");
                return;
            }
            tts.setSpeechRate((float) rate);
            tts.setPitch((float) pitch);
            boolean set = false;
            if (voiceName != null && !voiceName.isEmpty()) {
                try {
                    Set<Voice> vs = tts.getVoices();
                    if (vs != null) for (Voice v : vs) {
                        if (v.getName().equals(voiceName)) { tts.setVoice(v); set = true; break; }
                    }
                } catch (Exception ignored) { }
            }
            if (!set) tts.setLanguage(Locale.forLanguageTag(lang == null || lang.isEmpty() ? "en-US" : lang));
            Bundle params = new Bundle();
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, id);
        }

        @JavascriptInterface
        public void silence(long ms) {
            if (ttsReady && ms > 0) tts.playSilentUtterance(ms, TextToSpeech.QUEUE_ADD, "sil" + System.nanoTime());
        }

        @JavascriptInterface
        public String getEngineInfo() {
            JSONObject o = new JSONObject();
            try {
                String engine = tts != null ? tts.getDefaultEngine() : "";
                o.put("ready", ttsReady);
                o.put("engine", engine == null ? "" : engine);
                String label = engine;
                try {
                    PackageManager pm = getPackageManager();
                    ApplicationInfo ai = pm.getApplicationInfo(engine, 0);
                    label = String.valueOf(pm.getApplicationLabel(ai));
                } catch (Exception ignored) { }
                o.put("label", label == null ? "" : label);
            } catch (Exception ignored) { }
            return o.toString();
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public String getVoices() {
            JSONArray arr = new JSONArray();
            if (!ttsReady) return arr.toString();
            try {
                Set<Voice> vs = tts.getVoices();
                Voice def = tts.getDefaultVoice();
                if (vs != null) for (Voice v : vs) {
                    if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                    JSONObject o = new JSONObject();
                    o.put("name", v.getName());
                    o.put("voiceURI", v.getName());
                    o.put("lang", v.getLocale().toLanguageTag());
                    o.put("localService", !v.isNetworkConnectionRequired());
                    o.put("default", def != null && def.getName().equals(v.getName()));
                    o.put("quality", v.getQuality());
                    arr.put(o);
                }
            } catch (Exception ignored) { }
            return arr.toString();
        }
    }

    /* ---------------- App bridge (theme, file save, voices, keep-awake) ---------------- */

    private class AppBridge {
        @JavascriptInterface
        public void setTheme(String theme) {
            runOnUiThread(() -> applyBars(theme));
        }

        @JavascriptInterface
        public void openVoiceSettings() {
            runOnUiThread(() -> {
                // Google's own "Install voice data" screen (voice packs per language)
                Intent install = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                install.setPackage("com.google.android.tts");
                if (tryStart(install)) return;
                Intent any = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                String engine = tts != null ? tts.getDefaultEngine() : null;
                if (engine != null) any.setPackage(engine);
                if (tryStart(any)) return;
                openTts();
            });
        }

        @JavascriptInterface
        public void openSystemTtsSettings() {
            runOnUiThread(MainActivity.this::openTts);
        }

        @JavascriptInterface
        public void openPlayStore(String pkg) {
            runOnUiThread(() -> {
                if (!tryStart(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)))) {
                    tryStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
                }
            });
        }

        @JavascriptInterface
        public void setPlaying(boolean playing) {
            runOnUiThread(() -> {
                if (playing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public void saveTextFile(String name, String mime, String text) {
            runOnUiThread(() -> {
                pendingSaveText = text;
                saveDoc.launch(name);
            });
        }
    }

    // Phone's Text-to-speech settings (engine choice, speed, voice data)
    private void openTts() {
        if (tryStart(new Intent("com.android.settings.TTS_SETTINGS"))) return;
        if (tryStart(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))) {
            jsToast("Open Text-to-speech output, then the gear next to Speech Services by Google");
            return;
        }
        tryStart(new Intent(android.provider.Settings.ACTION_SETTINGS));
        jsToast("Search Settings for \"Text-to-speech\"");
    }

    private void jsToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private boolean tryStart(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private void applyBars(String theme) {
        String surface;
        boolean light;
        if ("sepia".equals(theme)) { surface = "#efe3ca"; light = true; }
        else if ("light".equals(theme)) { surface = "#ffffff"; light = true; }
        else { surface = "#111827"; light = false; }
        root.setBackgroundColor(Color.parseColor(surface));
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(light);
        c.setAppearanceLightNavigationBars(light);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Voices may have been downloaded while the user was in Android's settings
        if (ttsReady) emit("", "voices", "");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
