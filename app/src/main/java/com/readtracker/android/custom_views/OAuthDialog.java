package com.readtracker.android.custom_views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.R;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.interfaces.OAuthDialogResultListener;
import com.readtracker.android.support.ReadmillApiHelper;

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
      LayoutParams.FILL_PARENT);
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

  /** Show the dialog. */
  public static void show(FragmentManager fragmentManager) {
    OAuthDialog dialog = new OAuthDialog();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(dialog, "oauth-dialog");
    fragmentTransaction.commitAllowingStateLoss();
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
          try {
            Animation hide = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
            hide.setFillAfter(true);
            mProgressBar.startAnimation(hide);
          } catch(NullPointerException ex) {
            // It is observed that the getActivity() sometimes does not yield a
            // context (instead it is null) on 2.3.4.
            // For these cases we just fall back to hiding / showing the
            // progress bar.
            mProgressBar.setAnimation(null);
            mProgressBar.setVisibility(View.GONE);
          }

          mStatusVisible = false;
        } else if(newProgress < 100 && !mStatusVisible) {
          try {
            Animation show = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
            show.setFillAfter(true);
            mProgressBar.startAnimation(show);
          } catch(NullPointerException ignored) {
            mProgressBar.setAnimation(null);
            mProgressBar.setVisibility(View.VISIBLE);
          }
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

    int indicatorSize = ((BaseActivity) getActivity()).getPixels(24);
    RelativeLayout.LayoutParams lpSpinner = new RelativeLayout.LayoutParams(
      indicatorSize,
      indicatorSize
    );

    lpSpinner.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    lpSpinner.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

    int margin = ((BaseActivity) getActivity()).getPixels(24);
    lpSpinner.setMargins(margin, margin, margin, margin);

    int padding = ((BaseActivity) getActivity()).getPixels(6);
    indicator.setPadding(padding, padding, padding, padding);

    int color = Color.parseColor("#88000000");

    GradientDrawable backgroundDrawable = new GradientDrawable();
    backgroundDrawable.setCornerRadius(((BaseActivity) getActivity()).getPixels(24));
    backgroundDrawable.setColor(color);

    indicator.setBackgroundDrawable(backgroundDrawable);

    indicator.setLayoutParams(lpSpinner);
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
