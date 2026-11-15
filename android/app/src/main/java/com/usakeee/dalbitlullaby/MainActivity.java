package com.usakeee.dalbitlullaby;

import android.os.Bundle;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    /** True while a lullaby is playing; keeps the WebView (audio + timer) running with the screen off. */
    static volatile boolean keepAwake = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(SleepKeeperPlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        keepWebViewRunning();
    }

    @Override
    public void onStop() {
        super.onStop();
        keepWebViewRunning();
    }

    private void keepWebViewRunning() {
        if (!keepAwake || getBridge() == null) return;
        WebView webView = getBridge().getWebView();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }
}
