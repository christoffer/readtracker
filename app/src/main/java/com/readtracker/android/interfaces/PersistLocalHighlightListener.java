package com.readtracker.android.interfaces;

public interface PersistLocalHighlightListener {
  /**
   * Called when the LocalHighlight has been successfully saved or created in the database.
   *
   * @param id The id of the persisted object
   * @param created true if the object was created
   */
  public void onLocalHighlightPersisted(int id, boolean created);

  /**
   * Called when the LocalHighlight could not be saved.
   *
   */
  public void onLocalHighlightPersistedFailed();
}
