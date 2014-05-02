package com.readtracker.android.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.readtracker.R;
import com.readtracker.android.IntentKeys;

/** Generic WebView for viewing a http url. */
public class InAppBrowserActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.webview);

    WebView webView = (WebView) findViewById(R.id.webview);
    String url = getIntent().getStringExtra(IntentKeys.WEB_VIEW_URL);
    if(url != null) {
      webView.loadUrl(url);
    }
  }
}
