package com.readtracker.interfaces;

import com.readtracker.db.LocalHighlight;

public interface DeleteLocalHighlightListener {
  /**
   * Called when the local highlight has been deleted.
   */
  public void onLocalHighlightDeleted(LocalHighlight deletedHighlight);

  /**
   * Called when a local highlight failed to be deleted.
   */
  public void onLocalHighlightDeletedFailed(LocalHighlight deletedHighlight);
}
