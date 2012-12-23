package com.readtracker_beta.interfaces;

import com.readtracker_beta.db.LocalReading;

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
