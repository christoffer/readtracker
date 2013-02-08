package com.readtracker.custom_views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.R;
import com.readtracker.activities.ReadTrackerActivity;
import com.readtracker.interfaces.OAuthDialogResultListener;
import com.readtracker.support.ReadmillApiHelper;

/**
 * Screen for signing in to Readmill through a web interface
 */
public class OAuthDialog extends DialogFragment {
  private static final String TAG = OAuthDialog.class.getName();

  private ProgressBar mProgressBar;

  private boolean mStatusVisible = false;

  public OAuthDialog() {

  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NO_TITLE, 0);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RelativeLayout root = new RelativeLayout(getActivity());
    root.setMinimumHeight(10000); // Avoid jumping in size by always being max size
    WebView webView = createWebContentView(getActivity());

    // Load empty page so view is not transparent
    webView.loadData("<html><body></body></html>", "text/html", "utf-8");
    webView.setBackgroundColor(0xff000000);

    RelativeLayout.LayoutParams layoutFill = new RelativeLayout.LayoutParams(
      LayoutParams.FILL_PARENT,
      LayoutParams.WRAP_CONTENT);
    layoutFill.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    webView.setLayoutParams(layoutFill);

    String url = ApplicationReadTracker.getReadmillApiHelper().authorizeUrl();
    Log.i(TAG, "Loading url: " + url);

    mProgressBar = createStatusIndicator();
    mStatusVisible = true;

    root.addView(webView);
    root.addView(mProgressBar);
    webView.loadUrl(url);

    return root;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    super.onViewCreated(view, savedInstanceState);
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

    webView.setWebChromeClient(new WebChromeClient() {
      @Override public void onProgressChanged(WebView view, int newProgress) {
        if(newProgress == 100 && mStatusVisible) {
          Animation hide = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
          hide.setFillAfter(true);
          mProgressBar.startAnimation(hide);
          mStatusVisible = false;
        } else if(newProgress < 100 && !mStatusVisible) {
          Animation show = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
          show.setFillAfter(true);
          mProgressBar.startAnimation(show);
          mStatusVisible = true;
        }
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

  /**
   * Creates the TextView that shows the loading status.
   *
   * @return the text view
   */
  private ProgressBar createStatusIndicator() {
    ProgressBar indicator = new ProgressBar(getActivity());
    indicator.setIndeterminate(true);

    int indicatorSize = ((ReadTrackerActivity) getActivity()).getPixels(24);
    RelativeLayout.LayoutParams layoutTop = new RelativeLayout.LayoutParams(
      indicatorSize,
      indicatorSize
    );

    layoutTop.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

    int margin = ((ReadTrackerActivity) getActivity()).getPixels(12);
    layoutTop.setMargins(margin, margin, 0, 0);

    int padding = ((ReadTrackerActivity) getActivity()).getPixels(6);
    indicator.setPadding(padding, padding, padding, padding);

    int color = Color.parseColor("#88000000");

    GradientDrawable backgroundDrawable = new GradientDrawable();
    backgroundDrawable.setCornerRadius(((ReadTrackerActivity) getActivity()).getPixels(24));
    backgroundDrawable.setColor(color);

    indicator.setBackgroundDrawable(backgroundDrawable);

    indicator.setLayoutParams(layoutTop);
    return indicator;
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
