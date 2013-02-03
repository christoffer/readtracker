package com.readtracker.interfaces;

import com.readtracker.db.LocalReading;

/**
 * Callbacks for a prolonged sync with Readmill
 */
public interface ReadmillSyncProgressListener {
  /**
   * Called when the sync starts
   */
  public void onSyncStart();

  /**
   * Called when the sync is complete
   */
  public void onSyncDone();

  /**
   * Called when the sync has made significant progress
   *
   * @param message  a short description of what progress that was made,
   *                 or null if not applicable
   * @param progress progress of the total sync (0.0 - 1.0)
   *                 or null if not applicable
   */
  public void onSyncProgress(String message, Float progress);

  /**
   * Called when the sync failed
   *
   * @param message a message describing what went wrong
   */
  public void onSyncFailed(String message, int HTTPStatusCode);

  /**
   * Called when a local reading has been added, or an existing local reading
   * has been updated with new data.
   *
   * @param localReading the changed local reading
   */
  public void onReadingUpdated(LocalReading localReading);


  /**
   * Called when the local reading has been deleted.
   *
   * @param localReadingId id of the deleted local reading
   */
  void onReadingDeleted(int localReadingId);
}
