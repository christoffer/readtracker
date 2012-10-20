package com.readtracker;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Screen for signing in to Readmill through a web interface
 */
public class ActivitySignInAndAuthorize extends ReadTrackerActivity {
  private static LinearLayout mLayoutProgressBar;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sign_in_and_authorize);

    mLayoutProgressBar = (LinearLayout) findViewById(R.id.layoutProgressBar);

    int webViewAction;

    try {
      Bundle intentExtras = getIntent().getExtras();
      webViewAction = intentExtras.getInt(IntentKeys.WEB_VIEW_ACTION);
    } catch(Exception ex) {
      Log.e(TAG, "Failed to get intent keys", ex);
      toastLong("An error occurred when trying to open the browser");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    RelativeLayout parentLayout = (RelativeLayout) findViewById(R.id.layoutParentForWebview);

    WebView webView = createWebContentView();
    RelativeLayout.LayoutParams paramsWebView = new RelativeLayout.LayoutParams(
      LayoutParams.FILL_PARENT,
      LayoutParams.FILL_PARENT);
    paramsWebView.addRule(RelativeLayout.CENTER_IN_PARENT);
    webView.setLayoutParams(paramsWebView);

    parentLayout.addView(webView, 0);

    // Load empty page so view is not transparent
    webView.loadData("<html><body></body></html>", "text/html", "utf-8");
    webView.setBackgroundColor(0);
    String url = null;

    if(webViewAction == IntentKeys.WEB_VIEW_CREATE_ACCOUNT) {
      url = readmillApi().createAccountUrl();
    } else if(webViewAction == IntentKeys.WEB_VIEW_SIGN_IN_AND_AUTHORIZE) {
      url = readmillApi().authorizeUrl();
    }

    if(url != null) {
      Log.i(TAG, "Loading url: " + url);
      webView.loadUrl(url);
      webView.setBackgroundColor(0);
    }

  }

  @Override
  protected void requestWindowFeatures() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  private WebView createWebContentView() {
    WebView webView = new WebView(this);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);
        boolean isAuthorizationCallback = uri.getScheme().equals(getString(R.string.redirect_scheme));

        if(isAuthorizationCallback) {
          String code = uri.getQueryParameter("code");
          Log.i(TAG, "Got authorization code: " + code);
          view.setVisibility(View.INVISIBLE);
          onAuthorization(code);
          return false;
        }

        return super.shouldOverrideUrlLoading(view, url);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        setProgressVisible(false);
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        setProgressVisible(true);
      }

      private void setProgressVisible(boolean shouldBeVisible) {
        LinearLayout layout = ActivitySignInAndAuthorize.mLayoutProgressBar;
        if(layout != null) {
          layout.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
        }
      }

    });

    webView.getSettings().setJavaScriptEnabled(true);
    return webView;
  }

  /**
   * Attemt to exchange the provided authorization code for a token and update
   * the application state.
   *
   * @param code the received authorization code
   */
  private void onAuthorization(String code) {
    if(!readmillApi().authorize(code)) {
      Log.d(TAG, "Failed getting a token - exiting");
      toast("Authorization failed");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    setResult(RESULT_OK);
    finish();
  }
}
