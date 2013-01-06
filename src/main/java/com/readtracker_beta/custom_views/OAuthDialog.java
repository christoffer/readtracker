package com.readtracker_beta.custom_views;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import com.readtracker_beta.ApplicationReadTracker;
import com.readtracker_beta.R;
import com.readtracker_beta.interfaces.OAuthDialogResultListener;
import com.readtracker_beta.support.ReadmillApiHelper;

/**
 * Screen for signing in to Readmill through a web interface
 */
public class OAuthDialog extends DialogFragment {
  private static final String TAG = OAuthDialog.class.getName();

  public OAuthDialog() {

  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NO_TITLE, 0);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    LinearLayout root = new LinearLayout(getActivity());
    root.setMinimumHeight(10000); // Avoid jumping in size by always being max size
    WebView webView = createWebContentView(getActivity());

    // Load empty page so view is not transparent
    webView.loadData("<html><body></body></html>", "text/html", "utf-8");
    webView.setBackgroundColor(0xff000000);

    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
      LayoutParams.FILL_PARENT,
      LayoutParams.WRAP_CONTENT);

    String url = ApplicationReadTracker.getReadmillApiHelper().authorizeUrl();
    Log.i(TAG, "Loading url: " + url);

    root.addView(webView, layoutParams);
    webView.loadUrl(url);

    return root;
  }

  private WebView createWebContentView(Context context) {
    WebView webView = new WebView(context);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);
        boolean isAuthorizationCallback = uri.getScheme().equals(getString(R.string.redirect_scheme));

        if(isAuthorizationCallback) {
          String code = uri.getQueryParameter("code");
          Log.i(TAG, "Got authorization code: " + code);
          view.setVisibility(View.INVISIBLE);
          (new TokenExchangeAsyncTask()).execute(code);
          return false;
        }

        return super.shouldOverrideUrlLoading(view, url);
      }
    });

    webView.getSettings().setJavaScriptEnabled(true);
    return webView;
  }

  private void onTokenExchangeComplete(boolean success) {
    OAuthDialogResultListener listener = ((OAuthDialogResultListener) getActivity());
    if(success) {
      listener.onOAuthSuccess();
    } else {
      listener.onOAuthFailure();
    }
    dismiss();
  }

  // Perform the actual exchange in a background thread
  private class TokenExchangeAsyncTask extends AsyncTask<String, Void, Boolean> {
    @Override protected Boolean doInBackground(String... args) {
      ReadmillApiHelper apiHelper = ApplicationReadTracker.getReadmillApiHelper();
      return apiHelper.authorize(args[0]);
    }

    @Override protected void onPostExecute(Boolean success) {
      Log.v(TAG, "Completed exchange with success: " + success);
      onTokenExchangeComplete(success);
    }
  }
}
