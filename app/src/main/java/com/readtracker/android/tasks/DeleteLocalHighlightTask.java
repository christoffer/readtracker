package com.readtracker.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.interfaces.DeleteLocalHighlightListener;

import java.sql.SQLException;

/**
 * Delete a LocalHighlight from the database.
 */
public class DeleteLocalHighlightTask extends AsyncTask<LocalHighlight, Void, DeleteLocalHighlightResult> {
  private static final String TAG = DeleteLocalHighlightTask.class.getName();
  DeleteLocalHighlightListener mListener;

  public DeleteLocalHighlightTask(DeleteLocalHighlightListener listener) {
    mListener = listener;
  }

  /**
   * Deletes a local highlight and calls the given callback when done.
   * @param localHighlight Local highlight to delete.
   * @param listener listener to invoke when delete is complete.
   */
  public static void delete(LocalHighlight localHighlight, DeleteLocalHighlightListener listener) {
    DeleteLocalHighlightTask task = new DeleteLocalHighlightTask(listener);
    task.execute(localHighlight);
  }

  @Override
  protected DeleteLocalHighlightResult doInBackground(LocalHighlight... localHighlights) {
    LocalHighlight localHighlight = localHighlights[0];
    try {
      ApplicationReadTracker.getHighlightDao().delete(localHighlight);
      return new DeleteLocalHighlightResult(localHighlight, true);
    } catch(SQLException e) {
      Log.e(TAG, "An error occurred while deleting the LocalHighlight", e);
      return new DeleteLocalHighlightResult(localHighlight, false);
    }
  }

  @Override
  protected void onPostExecute(DeleteLocalHighlightResult result) {
    if(result.success) {
      mListener.onLocalHighlightDeleted(result.highlight);
    } else {
      mListener.onLocalHighlightDeletedFailed(result.highlight);
    }
  }
}

class DeleteLocalHighlightResult {
  LocalHighlight highlight;
  boolean success;

  public DeleteLocalHighlightResult(LocalHighlight highlight, boolean success) {
    this.success = success;
    this.highlight = highlight;
  }
}
