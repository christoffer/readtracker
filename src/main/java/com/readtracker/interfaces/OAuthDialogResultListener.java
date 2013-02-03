package com.readtracker.interfaces;

public interface OAuthDialogResultListener {
  /**
   * Called when the OAuth process fails.
   */
  void onOAuthFailure();

  /**
   * Called when the OAuth process succeeds.
   */
  void onOAuthSuccess();
}
