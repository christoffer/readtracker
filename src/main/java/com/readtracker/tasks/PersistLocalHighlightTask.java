package com.readtracker.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.j256.ormlite.dao.Dao;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.db.LocalHighlight;
import com.readtracker.interfaces.PersistLocalHighlightListener;

import java.sql.SQLException;

/**
 * Persist LocalHighlights.
 */
public class PersistLocalHighlightTask extends AsyncTask<LocalHighlight, Void, SuccessfulPersistingResult> {
  private static final String TAG = PersistLocalHighlightTask.class.getName();
  private PersistLocalHighlightListener mListener = null;

  public PersistLocalHighlightTask(PersistLocalHighlightListener listener) {
    mListener = listener;
  }

  @Override
  protected SuccessfulPersistingResult doInBackground(LocalHighlight... localHighlights) {
    LocalHighlight localHighlight = localHighlights[0];

    Log.i(TAG, "Saving local highlight with content: " + localHighlight.content);

    try {
      Dao.CreateOrUpdateStatus status = ApplicationReadTracker.getHighlightDao().createOrUpdate(localHighlight);
      return new SuccessfulPersistingResult(localHighlight.id, status.isCreated());
    } catch(SQLException e) {
      Log.e(TAG, "Failed to persist Highlight", e);
      return null;
    }
  }

  @Override
  protected void onPostExecute(SuccessfulPersistingResult successfulResult) {
    if(mListener == null) {
      return;
    }

    if(successfulResult == null) {
      mListener.onLocalHighlightPersistedFailed();
    } else {
      mListener.onLocalHighlightPersisted(successfulResult.id, successfulResult.wasCreated);
    }
  }
}

class SuccessfulPersistingResult {
  public int id;
  public boolean wasCreated;

  public SuccessfulPersistingResult(int id, boolean wasCreated) {
    this.id = id;
    this.wasCreated = wasCreated;
  }
}
