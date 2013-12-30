package com.readtracker.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.readtracker.android.db.Highlights;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.interfaces.PersistLocalHighlightListener;

/**
 * Persist LocalHighlights.
 */
public class PersistLocalHighlightTask extends AsyncTask<Void, Void, Dao.CreateOrUpdateStatus> {
  private static final String TAG = PersistLocalHighlightTask.class.getName();
  private PersistLocalHighlightListener mListener = null;
  private LocalHighlight mLocalHighlight;

  private PersistLocalHighlightTask(LocalHighlight localHighlight, PersistLocalHighlightListener listener) {
    mLocalHighlight = localHighlight;
    mListener = listener;
  }

  public static void persist(LocalHighlight localHighlight, PersistLocalHighlightListener listener) {
    new PersistLocalHighlightTask(localHighlight, listener).execute();
  }

  @Override
  protected Dao.CreateOrUpdateStatus doInBackground(Void... ignored) {
    Log.i(TAG, "Saving local highlight with content: " + mLocalHighlight.content);
    return Highlights.createOrUpdate(mLocalHighlight);
  }

  @Override
  protected void onPostExecute(Dao.CreateOrUpdateStatus status) {
    if(mListener == null) {
      return;
    }

    if(status == null) {
      mListener.onLocalHighlightPersistedFailed();
    } else {
      mListener.onLocalHighlightPersisted(mLocalHighlight.id, status.isCreated());
    }
  }
}
