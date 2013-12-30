package com.readtracker.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.SaveLocalReadingListener;

import java.sql.SQLException;

/**
 * Saves a LocalReading instance in the background and gives a callback when
 * the save is complete. The database entry will be created if it does not
 * already exist.
 */
public class SaveLocalReadingTask extends AsyncTask<LocalReading, Void, LocalReading> {
  private static final String TAG = SaveLocalReadingTask.class.getName();
  SaveLocalReadingListener mListener;

  public SaveLocalReadingTask(SaveLocalReadingListener listener) {
    mListener = listener;
  }

  /**
   * Saves a local reading and calls the given callback when done.
   * @param localReading Local reading to save.
   * @param listener listener to invoke when save is complete.
   */
  public static void save(LocalReading localReading, SaveLocalReadingListener listener) {
    SaveLocalReadingTask task = new SaveLocalReadingTask(listener);
    task.execute(localReading);
  }

  @Override
  protected LocalReading doInBackground(LocalReading... localReadings) {
    LocalReading localReading = localReadings[0];
    try {
      ApplicationReadTracker.getReadingDao().createOrUpdate(localReading);
      return localReadings[0];
    } catch(SQLException e) {
      Log.e(TAG, "An error occurred while saving the LocalReading", e);
      return null;
    }
  }

  @Override
  protected void onPostExecute(LocalReading localReading) {
    mListener.onLocalReadingSaved(localReading);
  }
}
