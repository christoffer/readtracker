package com.readtracker.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.SaveLocalReadingListener;

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
