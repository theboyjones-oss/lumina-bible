/* Lumina Android bridge — loaded before the app script.
   - Replaces the Web Speech API (not functional in Android WebView) with native Android TextToSpeech
     (Speech Services by Google neural voices: free, no key, offline once downloaded)
   - Routes JSON backup downloads to the Android "Save as" picker
   - Syncs status/navigation bar colours with the reading theme
   - Handles the hardware Back button (closes modals / search drawer first) */
(function () {
  'use strict';
  var TTS = window.AndroidTTS;
  var BR = window.AndroidBridge;

  /* ---------- 1. speechSynthesis polyfill backed by Android TTS ---------- */
  if (TTS) {
    var nextId = 1;
    var pending = {};
    var voices = [];

    function loadVoices() {
      try { voices = JSON.parse(TTS.getVoices() || '[]'); } catch (e) { voices = []; }
      return voices;
    }

    function Utterance(text) {
      this.text = String(text == null ? '' : text);
      this.lang = 'en-US';
      this.rate = 1;
      this.pitch = 1;
      this.volume = 1;
      this.voice = null;
      this.onstart = null;
      this.onend = null;
      this.onerror = null;
    }

    var synth = {
      onvoiceschanged: null,
      speaking: false,
      pending: false,
      paused: false,
      getVoices: function () { return voices.length ? voices : loadVoices(); },
      speak: function (u) {
        var id = 'u' + (nextId++);
        pending[id] = u;
        synth.speaking = true;
        TTS.speak(id, u.text, Number(u.rate) || 1, Number(u.pitch) || 1, u.voice ? u.voice.name : '', u.lang || 'en-US');
      },
      cancel: function () {
        pending = {};
        synth.speaking = false;
        TTS.stop();
      },
      // Android-only: insert a pause into the speech queue (used for "pause between verses")
      speakSilence: function (ms) { TTS.silence(Number(ms) || 0); },
      pause: function () { synth.cancel(); },
      resume: function () {},
      addEventListener: function (t, fn) { if (t === 'voiceschanged') synth.onvoiceschanged = fn; },
      removeEventListener: function () {}
    };

    // Called from native code
    window.__luminaTts = function (id, type, code) {
      if (type === 'voices') {
        loadVoices();
        if (typeof synth.onvoiceschanged === 'function') { try { synth.onvoiceschanged(); } catch (e) {} }
        return;
      }
      var u = pending[id];
      if (!u) return;
      var ev = { utterance: u, error: code || '' };
      if (type === 'start') {
        if (typeof u.onstart === 'function') u.onstart(ev);
      } else if (type === 'end') {
        delete pending[id];
        if (!Object.keys(pending).length) synth.speaking = false;
        if (typeof u.onend === 'function') u.onend(ev);
      } else if (type === 'error') {
        delete pending[id];
        if (!Object.keys(pending).length) synth.speaking = false;
        if (typeof u.onerror === 'function') u.onerror(ev);
      }
    };

    try {
      Object.defineProperty(window, 'speechSynthesis', { value: synth, configurable: true, writable: true });
    } catch (e) { window.speechSynthesis = synth; }
    try {
      Object.defineProperty(window, 'SpeechSynthesisUtterance', { value: Utterance, configurable: true, writable: true });
    } catch (e) { window.SpeechSynthesisUtterance = Utterance; }
    loadVoices();
  }

  if (!BR) return;

  /* ---------- 2. Blob downloads -> Android Save dialog ---------- */
  var blobs = new Map();
  var origCreate = URL.createObjectURL.bind(URL);
  URL.createObjectURL = function (obj) {
    var url = origCreate(obj);
    if (obj instanceof Blob) blobs.set(url, obj);
    return url;
  };
  var origClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    var href = this.getAttribute('href') || '';
    if (this.hasAttribute('download') && href.indexOf('blob:') === 0 && blobs.has(href)) {
      var name = this.getAttribute('download') || 'download.json';
      var blob = blobs.get(href);
      blobs.delete(href);
      blob.text().then(function (txt) { BR.saveTextFile(name, blob.type || 'application/json', txt); });
      return;
    }
    return origClick.call(this);
  };

  /* ---------- 3. Theme -> system bar colours ---------- */
  function syncTheme() {
    var t = document.documentElement.getAttribute('data-theme') || 'dark';
    try { BR.setTheme(t); } catch (e) {}
  }
  new MutationObserver(syncTheme).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  document.addEventListener('DOMContentLoaded', syncTheme);

  /* ---------- 4. Back button ---------- */
  window.__luminaBack = function () {
    var open = document.querySelectorAll('.modal-backdrop:not(.hidden)');
    if (open.length) {
      open.forEach(function (m) { m.classList.add('hidden'); });
      return 'handled';
    }
    if (window.__luminaHomeBack && window.__luminaHomeBack()) return 'handled';
    var drawer = document.getElementById('searchDrawer');
    if (drawer && !drawer.classList.contains('hidden')) {
      drawer.classList.add('hidden');
      var b = document.getElementById('btnToggleSearch');
      if (b) b.classList.remove('is-active');
      return 'handled';
    }
    return 'exit';
  };
})();

