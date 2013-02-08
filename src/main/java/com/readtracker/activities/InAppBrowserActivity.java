package com.readtracker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import com.readtracker.IntentKeys;
import com.readtracker.R;

/**
 * Generic WebView for viewing any web url.
 */
public class InAppBrowserActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.webview);

    WebView webView = (WebView) findViewById(R.id.webview);
    String url = getIntent().getExtras().getString(IntentKeys.WEB_VIEW_URL);
    if(url != null) {
      webView.loadUrl(url);
    }
  }
}