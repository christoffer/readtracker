package com.readtracker_beta.interfaces;

import com.readtracker_beta.db.LocalReading;

public interface SaveLocalReadingListener {
  /**
   * Callback for when a LocalReading has been persisted to the database.
   * @param localReading the item that was saved if saved successfully,
   *                    or null, indicating an error.
   */
  public void onLocalReadingSaved(LocalReading localReading);
}
