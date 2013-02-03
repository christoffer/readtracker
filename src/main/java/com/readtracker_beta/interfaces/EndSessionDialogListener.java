package com.readtracker_beta.interfaces;

import com.readtracker_beta.db.LocalSession;

public interface EndSessionDialogListener {
  /**
   * Called when a local session was successfully created
   */
  void onSessionCreated(LocalSession localSession);

  /**
   * Called when the local session could not be created.
   */
  void onSessionFailed();
}
