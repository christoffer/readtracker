package com.readtracker.interfaces;

import com.readtracker.db.LocalReading;

/**
 * Callbacks for interaction with a list of local reading items
 */
public interface LocalReadingInteractionListener {
  /**
   * A local reading was clicked in the list
   * @param localReading clicked local reading
   */
  public void onLocalReadingClicked(LocalReading localReading);
}
