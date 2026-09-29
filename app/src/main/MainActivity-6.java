package com.flavouratlas.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1;
    private static final int SAVE_FILE = 2;
    private static final String HOST = "appassets.androidplatform.net";
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            web = new WebView(this);
            setContentView(web);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setAllowFileAccess(false);
            s.setAllowContentAccess(false);

            final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                    .setDomain(HOST)
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                    .build();

            web.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    Uri u = request.getUrl();
                    String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase();
                    // The app's own private address: serve files from inside the APK.
                    if (HOST.equals(u.getHost())) return loader.shouldInterceptRequest(u);
                    // Block real internet traffic only. data:, blob: and about: are the
                    // app's own content and must be allowed (this was the startup bug).
                    if (scheme.equals("http") || scheme.equals("https")) {
                        return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked", null,
                                new ByteArrayInputStream(new byte[0]));
                    }
                    return null;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String scheme = request.getUrl().getScheme();
                    if (scheme != null && (scheme.equals("http") || scheme.equals("https")))
                        return !HOST.equals(request.getUrl().getHost());
                    return false;
                }
            });

            web.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage m) {
                    if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                        Toast.makeText(MainActivity.this, "Page error: " + m.message()
                                + " (line " + m.lineNumber() + ")", Toast.LENGTH_LONG).show();
                    return true;
                }

                @Override
                public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                    if (fileCallback != null) fileCallback.onReceiveValue(null);
                    fileCallback = cb;
                    try {
                        startActivityForResult(p.createIntent(), PICK_FILE);
                    } catch (Exception e) {
                        fileCallback = null;
                        return false;
                    }
                    return true;
                }
            });

            web.addJavascriptInterface(new Bridge(), "AndroidBridge");
            if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) loadApp();
        } catch (Throwable t) {
            showError("Flavour Atlas could not start", t);
        }
    }

    /** Reads the page from inside the APK and shows it: fully offline. */
    private void loadApp() {
        String html;
        try (InputStream in = getAssets().open("index.html")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            html = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            showError("index.html is missing. It must be at app/src/main/assets/index.html", e);
            return;
        }
        // Same private address as before, so saved notes and photos stay available.
        web.loadDataWithBaseURL("https://" + HOST + "/assets/", html, "text/html", "UTF-8", null);
    }

    /** Shows startup problems as plain text (no web page needed). */
    private void showError(String title, Throwable t) {
        TextView tv = new TextView(this);
        tv.setPadding(40, 60, 40, 40);
        tv.setTextSize(16);
        tv.setTextColor(Color.BLACK);
        tv.setBackgroundColor(Color.WHITE);
        tv.setTextIsSelectable(true);
        tv.setText(title + "\n\n" + t);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        sv.addView(tv);
        setContentView(sv);
    }

    /** Lets the page save a backup file; the person chooses where. */
    class Bridge {
        @JavascriptInterface
        public void saveFile(final String name, final String content) {
            runOnUiThread(() -> {
                pendingSave = content;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, name);
                startActivityForResult(i, SAVE_FILE);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        } else if (requestCode == SAVE_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSave != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    out.write(pendingSave.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Backup not saved: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingSave = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web == null) { finish(); return; }
        // Back closes an open sheet first; otherwise leaves the app.
        web.evaluateJavascript(
            "(function(){var s=document.getElementById('sheet');"
          + "if(s&&s.classList.contains('open')){closeSheet();return 1}return 0})()",
            r -> { if (!"1".equals(r)) finish(); });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }
}
