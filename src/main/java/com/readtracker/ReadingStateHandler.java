package com.readtracker;

import android.content.SharedPreferences;
import android.util.Log;
import com.readtracker.value_objects.ReadingState;

/**
 * Handles storing and loading of a temporary reading state
 */
class ReadingStateHandler {
  public static final String KEY_ACTIVE_TIMESTAMP = "reading-state-handler-is-active";
  public static final String KEY_ELAPSED = "reading-state-handler-elapsed";
  public static final String KEY_LOCAL_READING_ID = "reading-state-handler-local-reading-id";

  private static final String TAG = ReadingStateHandler.class.getName();

  /**
   * Stores the given reading state in preferences.
   *
   * @param localReadingId      the id of the LocalReading of the session
   * @param elapsedMilliseconds the elapsed time in ms
   * @param activeTimestamp     timestamp since last started timing
   */
  public static void store(int localReadingId, long elapsedMilliseconds, long activeTimestamp) {
    store(new ReadingState(localReadingId, elapsedMilliseconds, activeTimestamp));
  }

  /**
   * Stores the given reading state in preferences.
   *
   * @param readingState reading state to store
   */
  public static void store(ReadingState readingState) {
    Log.d(TAG, "Storing reading state: " + (readingState == null ? "NULL" : readingState));
    if(readingState == null) {
      return;
    }

    ApplicationReadTracker.getApplicationPreferences().
      edit().
      putInt(KEY_LOCAL_READING_ID, readingState.getLocalReadingId()).
      putLong(KEY_ELAPSED, readingState.getElapsedBeforeTimestamp()).
      putLong(KEY_ACTIVE_TIMESTAMP, readingState.getActiveTimestamp()).
      commit();
  }

  /**
   * Loads a previously stored ReadingState
   *
   * @return the ReadingState or null
   */
  public static ReadingState load() {
    Log.d(TAG, "Loading reading state");
    SharedPreferences pref = ApplicationReadTracker.getApplicationPreferences();

    int localReadingId = pref.getInt(KEY_LOCAL_READING_ID, -1);
    long elapsedMilliseconds = pref.getLong(KEY_ELAPSED, 0);
    long activeTimestamp = pref.getLong(KEY_ACTIVE_TIMESTAMP, 0);

    if(localReadingId == -1 || elapsedMilliseconds == 0) {
      Log.d(TAG, " - No reading state found");
      return null;
    }

    ReadingState readingState = new ReadingState(localReadingId, elapsedMilliseconds, activeTimestamp);
    Log.d(TAG, " - Found reading state: " + readingState);
    return readingState;
  }

  public static void clear() {
    Log.d(TAG, "Clearing reading state");
    ApplicationReadTracker.getApplicationPreferences().
      edit().
      remove(KEY_LOCAL_READING_ID).
      remove(KEY_ELAPSED).
      remove(KEY_ACTIVE_TIMESTAMP).
      commit();
  }
}
