package com.readtracker_beta.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.readtracker_beta.ApplicationReadTracker;
import com.readtracker_beta.db.LocalHighlight;
import com.readtracker_beta.interfaces.CreateHighlightTaskListener;

import java.sql.SQLException;

/**
 * Persist LocalHighlights.
 */
public class CreateHighlightAsyncTask extends AsyncTask<LocalHighlight, Void, Boolean> {
  private static final String TAG = CreateHighlightAsyncTask.class.getName();
  private CreateHighlightTaskListener mListener = null;

  public CreateHighlightAsyncTask(CreateHighlightTaskListener listener) {
    mListener = listener;
  }

  @Override
  protected Boolean doInBackground(LocalHighlight... localHighlights) {
    LocalHighlight localHighlight = localHighlights[0];

    Log.i(TAG, "Saving local highlight with content: " + localHighlight.content);

    try {
      ApplicationReadTracker.getHighlightDao().create(localHighlight);
      return true;
    } catch(SQLException e) {
      Log.e(TAG, "Failed to create Highlight", e);
      return false;
    }

  }

  @Override
  protected void onPostExecute(Boolean result) {
    if(mListener != null) {
      mListener.onReadingHighlightCreated(result);
    }
  }
}
