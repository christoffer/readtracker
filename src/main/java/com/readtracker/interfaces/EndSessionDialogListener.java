package com.readtracker.interfaces;

import com.readtracker.db.LocalSession;

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
