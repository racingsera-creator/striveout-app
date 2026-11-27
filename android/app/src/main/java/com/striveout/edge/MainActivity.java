package com.striveout.edge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestRuntimePermissions();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileChooserLauncher.launch(intent);
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void requestRuntimePermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        }
        if (!needed.isEmpty()) {
            permissionLauncher.launch(needed.toArray(new String[0]));
        }
    }
}
