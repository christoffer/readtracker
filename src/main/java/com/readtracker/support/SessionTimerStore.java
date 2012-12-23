package com.readtracker.support;

import android.content.SharedPreferences;
import android.util.Log;
import com.readtracker.ApplicationReadTracker;

/**
 * Handles storing and loading of a session timer
 */
public class SessionTimerStore {
  public static final String KEY_ACTIVE_TIMESTAMP = "reading-state-handler-is-active";
  public static final String KEY_ELAPSED = "reading-state-handler-elapsed";
  public static final String KEY_LOCAL_READING_ID = "reading-state-handler-local-reading-id";

  private static final String TAG = SessionTimerStore.class.getName();

  /**
   * Stores the given reading state in preferences.
   *
   * @param sessionTimer reading state to store
   */
  public static void store(SessionTimer sessionTimer) {
    Log.d(TAG, "Storing reading state: " + (sessionTimer == null ? "NULL" : sessionTimer));
    if(sessionTimer == null) {
      return;
    }

    ApplicationReadTracker.getApplicationPreferences().
      edit().
      putLong(KEY_ELAPSED, sessionTimer.getElapsedBeforeTimestamp()).
      putLong(KEY_ACTIVE_TIMESTAMP, sessionTimer.getActiveTimestamp()).
      commit();
  }

  /**
   * Loads a previously stored SessionTimer
   *
   * @return the SessionTimer or null
   */
  public static SessionTimer load() {
    Log.d(TAG, "Loading reading state");
    SharedPreferences pref = ApplicationReadTracker.getApplicationPreferences();

    int localReadingId = pref.getInt(KEY_LOCAL_READING_ID, -1);
    long elapsedMilliseconds = pref.getLong(KEY_ELAPSED, 0);
    long activeTimestamp = pref.getLong(KEY_ACTIVE_TIMESTAMP, 0);

    if(localReadingId == -1 || elapsedMilliseconds == 0) {
      Log.d(TAG, " - No reading state found");
      return null;
    }

    SessionTimer sessionTimer = new SessionTimer(localReadingId, elapsedMilliseconds, activeTimestamp);
    Log.d(TAG, " - Found reading state: " + sessionTimer);
    return sessionTimer;
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
