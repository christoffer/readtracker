package com.readtracker.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.Where;
import com.readtracker.android.ApplicationReadTracker;
import com.readtracker.android.db.Highlights;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.db.LocalSession;
import com.readtracker.android.interfaces.ReadmillSyncProgressListener;
import com.readtracker.android.support.ReadmillApiHelper;
import com.readtracker.android.support.ReadmillConverter;
import com.readtracker.android.support.ReadmillException;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.readtracker.android.support.ReadmillApiHelper.dumpJSON;

/**
 * Syncs data for the current user with their Readmill profile.
 */
public class ReadmillSyncAsyncTask extends AsyncTask<Long, ReadmillSyncProgressMessage, Integer> {
  private static final int STATUS_ERROR = -1;
  private static final int STATUS_OK = 0;

  private static final String TAG = ReadmillSyncAsyncTask.class.getName();
  private Dao<LocalReading, Integer> mReadingDao;
  private Dao<LocalSession, Integer> mSessionDao;
  private Dao<LocalHighlight, Integer> mHighlightDao;
  private ReadmillApiHelper mReadmillApi;
  private boolean mIsFullSync = false;

  ReadmillSyncProgressListener mProgressListener;

  public ReadmillSyncAsyncTask(ReadmillSyncProgressListener progressListener, ReadmillApiHelper readmillApi, boolean fullSync) {
    super();
    mProgressListener = progressListener;
    mReadmillApi = readmillApi;
    mIsFullSync = fullSync;
  }

  @Override
  protected Integer doInBackground(Long... id) {
    long readmillUserId = id[0];

    Log.i(TAG, "Starting " + (mIsFullSync ? "full " : "quick ") + " Readmill sync");

    try {
      mReadingDao = ApplicationReadTracker.getReadingDao();
      mSessionDao = ApplicationReadTracker.getSessionDao();
      mHighlightDao = ApplicationReadTracker.getHighlightDao();
    } catch(SQLException e) {
      Log.e(TAG, "Failed to get DAOs", e);
      return STATUS_ERROR;
    }

    try {
      syncUser(readmillUserId, mIsFullSync);
      return STATUS_OK;
    } catch(ReadmillException exception) {
      Log.w(TAG, "Readmill Exception while trying to sync readings", exception);
      int httpStatusCode = exception.getStatusCode();
      Log.d(TAG, "Finishing with status code " + httpStatusCode);
      return httpStatusCode == -1 ? STATUS_ERROR : httpStatusCode;
    } catch(JSONException e) {
      Log.w(TAG, "Unexpected JSON received from Readmill", e);
      return STATUS_ERROR;
    } catch(SQLException e) {
      Log.e(TAG, "Unexpected SQL error while syncing", e);
      return STATUS_ERROR;
    }
  }

  @Override
  protected void onPreExecute() {
    mProgressListener.onSyncStart();
  }

  @Override
  protected void onPostExecute(Integer statusCode) {
    Log.d(TAG, "onPostExecute(" + statusCode + ")");
    if(statusCode == STATUS_OK) {
      mProgressListener.onSyncDone();
    } else {
      mProgressListener.onSyncFailed("An error occurred while syncing", statusCode);
    }
  }

  @Override
  protected void onCancelled() {
    Log.d(TAG, "onCancelled()");
    mProgressListener.onSyncDone();
  }

  @Override
  protected void onProgressUpdate(ReadmillSyncProgressMessage... messages) {
    for(ReadmillSyncProgressMessage message : messages) {
      switch(message.getMessageType()) {
        case READING_CHANGED:
          mProgressListener.onReadingUpdated(message.getLocalReading());
          break;
        case READING_DELETED:
          mProgressListener.onReadingDeleted(message.getLocalReading().id);
          break;
        case SYNC_PROGRESS:
          mProgressListener.onSyncProgress(message.getMessage(), message.getProgress());
          break;
      }
    }
  }

  /**
   * Syncs all local and remote data for a given user.
   *
   * @param readmillUserId readmill id of user to sync data for
   * @param fullSync       skip change detection and force update of local data
   * @throws ReadmillException if an error occurs while communicating with Readmill
   * @throws org.json.JSONException     if the response from Readmill is not properly formatted
   */
  private void syncUser(long readmillUserId, boolean fullSync) throws ReadmillException, JSONException, SQLException {
    Log.i(TAG, "Syncing user with Readmill id: " + readmillUserId);

    deleteMarkedHighlights();
    // deleteMarkedReadings(); TODO This step should be here instead of in the remote/local loop
    uploadNewReadings();
    uploadNewSessions();
    uploadNewHighlights();

    ArrayList<LocalReading> localReadings = getAllConnectedLocalReadingForUserId(readmillUserId);
    ArrayList<JSONObject> remoteReadings = getAllRemoteReadingsForUserId(readmillUserId);

    syncLocalReadingsWithRemoteReadings(localReadings, remoteReadings, fullSync);

    ArrayList<LocalHighlight> localHighlightsToEdit = getEditedConnectedHighlights();
    pushEditedHighlights(localHighlightsToEdit);
  }

  /**
   * Posts a generic message and a progress.
   *
   * @param message     Message to post
   * @param currentStep current step
   * @param totalSteps  total steps
   */
  private void postProgressUpdateMessage(String message, int currentStep, int totalSteps) {
    final float progress = ((float) currentStep / totalSteps);
    publishProgress(ReadmillSyncProgressMessage.syncProgress(progress, message));
  }

  /**
   * Puts a reading as a progress update message.
   *
   * @param localReading Reading to post as progress update.
   */
  private void postProgressUpdateData(LocalReading localReading) {
    if(localReading != null) {
      publishProgress(ReadmillSyncProgressMessage.readingChanged(localReading));
    }
  }

  private void postProgressUpdateDeletedReading(LocalReading deletedLocalReading) {
    if(deletedLocalReading != null) {
      publishProgress(ReadmillSyncProgressMessage.readingDeleted(deletedLocalReading));
    }
  }

  /**
   * Updates a LocalReading object with metadata from a Readmill Reading.
   * Updates title, author, cover, state, closing remark and touched at.
   * Triggers update of all reading sessions for the given LocalReading.
   *
   * @param localReading  LocalReading to update
   * @param remoteReading Readmill Reading containing canonical metadata
   * @throws org.json.JSONException if the readmill response was not properly formatted
   */
  private void updateLocalReadingMetadata(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    Log.i(TAG, "Updating Metadata for Reading: " + localReading.readmillReadingId);

    JSONObject remoteBook = remoteReading.getJSONObject("book");

    localReading.title = remoteBook.getString("title");
    localReading.author = remoteBook.getString("author");
    localReading.setTouchedAt(ReadmillApiHelper.parseISO8601(remoteReading.getString("touched_at")));
    localReading.readmillState = ReadmillApiHelper.toIntegerState(remoteReading.getString("state"));
    localReading.readmillClosingRemark = ReadmillConverter.optString("closing_remark", null, remoteReading);
    localReading.readmillPrivate = remoteReading.getBoolean("private");

    String coverURL = remoteBook.getString("cover_url");
    if(coverURL.matches("default-cover")) {
      Log.v(TAG, "Not replacing with default cover");
    } else if(!localReading.coverURL.equals(coverURL)) {
      Log.i(TAG, "Replacing old cover url: " + localReading.coverURL + " with server cover: " + coverURL);
      localReading.coverURL = coverURL;
    }

    if(localReading.getRemoteTouchedAt().after(localReading.getLastReadAt())) {
      localReading.setLastReadAt(localReading.getRemoteTouchedAt());
    }

    mReadingDao.update(localReading);
  }

  private void syncDependentObjects(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    syncReadingSessions(localReading, remoteReading);
    syncHighlights(localReading, remoteReading);
  }

  /**
   * Create a LocalReading object from the provided Readmill Reading.
   * Triggers update of all reading sessions for the given LocalReading.
   *
   * @param remoteReading Readmill Reading to create LocalReading for
   * @return the created LocalReading or null
   * @throws org.json.JSONException if the readmill response was not properly formatted
   */
  private LocalReading createLocalReading(JSONObject remoteReading) throws JSONException, SQLException {
    LocalReading spawn = ReadmillConverter.createLocalReadingFromReadingJSON(remoteReading);
    mReadingDao.create(spawn);
    Log.i(TAG, "Created LocalReading for " + spawn.title + " with Readmill id: " + spawn.readmillReadingId);
    syncDependentObjects(spawn, remoteReading);
    return spawn;
  }

  /**
   * Loop through the given list of remote readings and create a local reading
   * for each of them.
   *
   * @param remoteReadings Remote readings to create locally
   * @throws org.json.JSONException if the readmill response was not properly formatted
   */
  private void createLocalReadings(List<JSONObject> remoteReadings) throws JSONException, SQLException {
    int totalCount = remoteReadings.size();
    Log.d(TAG, "Pulling " + totalCount + " new readings");
    for(int currentCount = 0; currentCount < totalCount; currentCount++) {
      if(isCancelled()) { return; }

      JSONObject remoteReading = remoteReadings.get(currentCount);
      JSONObject remoteBook = remoteReading.getJSONObject("book");

      String message = "Adding " + remoteBook.getString("title");
      postProgressUpdateMessage(message, currentCount, totalCount - 1);

      LocalReading spawn = createLocalReading(remoteReading);
      postProgressUpdateData(spawn);
    }
  }

  /**
   * Get updated information about a reading from Readmill and save it locally.
   *
   * @param localToRemoteReadingMap Map of local readings to remote readings
   */
  private void pullChanges(Map<LocalReading, JSONObject> localToRemoteReadingMap) throws SQLException {
    int totalCount = localToRemoteReadingMap.size();
    int currentCount = 0;
    Log.i(TAG, "Pulling changes for " + totalCount + " readings");

    for(Map.Entry<LocalReading, JSONObject> entry : localToRemoteReadingMap.entrySet()) {
      if(isCancelled()) { return; }

      LocalReading localReading = entry.getKey();
      JSONObject remoteReading = entry.getValue();

      postProgressUpdateMessage("Refreshing " + localReading.title, currentCount++, totalCount - 1);
      try {
        updateLocalReadingMetadata(localReading, remoteReading);
        syncDependentObjects(localReading, remoteReading);
      } catch(JSONException e) {
        Log.w(TAG, "Failed to update, unexpected JSON format of remote reading: " + (remoteReading == null ? "NULL" : remoteReading.toString()));
      }
      postProgressUpdateData(localReading);
    }
  }

  /**
   * Goes through the list of local readings and remote readings and divides
   * them into these buckets:
   * - Items that have local changes that should be pushed
   * - Items that have remote changes that should be pulled
   * - Items that does not exist locally and should be created
   * - Items that have been deleted locally and should be deleted remotely
   * - Items that maybe have become orphans (deleted remotely but not locally)
   * <p/>
   * Note that this sync currently does not handle deleting of readings well.
   * A locally deleted reading will be recreated on the next sync from the
   * server, and a reading deleted on the server will not be deleted locally.
   *
   * @param localReadings  List of local readings
   * @param remoteReadings List of remote readings
   * @throws org.json.JSONException if the response from Readmill was not correct
   */
  private void syncLocalReadingsWithRemoteReadings(ArrayList<LocalReading> localReadings, ArrayList<JSONObject> remoteReadings, boolean fullSync)
    throws JSONException, SQLException, ReadmillException {
    Map<LocalReading, JSONObject> pullChangesList = new HashMap<LocalReading, JSONObject>();
    List<LocalReading> pushClosedStateList = new ArrayList<LocalReading>();
    List<LocalReading> pushLocallyChangedList = new ArrayList<LocalReading>();
    List<JSONObject> pullReadingsList = new ArrayList<JSONObject>();
    List<LocalReading> readingsToDelete = new ArrayList<LocalReading>();
    List<LocalReading> possibleOrphans = new ArrayList<LocalReading>();

    Log.i(TAG, "Performing a sync between " + localReadings.size() + " local readings and " + remoteReadings.size() + " remote readings");

    // Check for orphaned local readings. That is readings that have been connected,
    // but are now deleted on the server.
    for(LocalReading localReading : localReadings) {
      if(!localReading.isConnected()) {
        // Unconnected readings can't be orphans
        continue;
      }

      boolean remoteFound = false;
      for(JSONObject remoteReading : remoteReadings) {
        final long remoteReadingId = remoteReading.getLong("id");
        if(remoteReadingId == localReading.readmillReadingId) {
          remoteFound = true;
          break;
        }
      }

      if(!remoteFound) {
        possibleOrphans.add(localReading);
      }
    }

    for(JSONObject remoteReading : remoteReadings) {
      final long remoteReadingId = remoteReading.getLong("id");
      Log.v(TAG, "Looking for a local reading matching remote id: " + remoteReadingId);

      if(isCancelled()) { return; }

      boolean foundLocal = false;
      for(LocalReading localReading : localReadings) {
        foundLocal = localReading.readmillReadingId == remoteReadingId;
        if(!foundLocal) {
          continue; // Not this reading
        } else if(remoteReading.getString("state").equals("interesting")) {
          Log.v(TAG, " - Found but is state: interesting, ignoring");
          break; // Don't involve "interesting" readings
        }
        Log.v(TAG, " - Found in local reading with id: " + localReading.id);

        // Resolve sync
        long remoteTouchedAt = ReadmillApiHelper.parseISO8601ToUnix(remoteReading.getString("touched_at"));

        if(localReading.deletedByUser) {
          Log.d(TAG, "Local reading has been deleted: " + localReading.readmillReadingId);
          readingsToDelete.add(localReading);
        } else {
          if(hasLocalChanges(localReading, remoteReading)) {
            Log.d(TAG, "Local reading has changes that will be pushed to remote reading: " + remoteReadingId);
            pushLocallyChangedList.add(localReading);
          }

          if(closedLocallyButNotRemotely(localReading, remoteReading)) {
            Log.d(TAG, "Local reading has been closed, readmill id:" + remoteReadingId);
            pushClosedStateList.add(localReading);
          }

          if(fullSync || localReading.hasRemoteChangedFrom(remoteTouchedAt)) {
            Log.d(TAG, "Remote reading has changed, readmill id: " + remoteReadingId);
            Log.d(TAG, " - Local timestamp: " + (0.001 * localReading.getRemoteTouchedAt().getTime()) + " vs remote: " + remoteTouchedAt);
            pullChangesList.put(localReading, remoteReading);
          }
        }

        break;
      }

      if(!foundLocal) {
        Log.d(TAG, "Local reading is missing, readmill id: " + remoteReadingId);
        pullReadingsList.add(remoteReading);
      }
    }

    confirmAndDeleteOrphans(possibleOrphans);
    pushDeletions(readingsToDelete);
    pushLocallyChanged(pushLocallyChangedList);
    pushClosedStates(pushClosedStateList);
    pullChanges(pullChangesList);
    createLocalReadings(pullReadingsList);
  }

  /**
   * Delete local readings after being confirmed as orphans.
   * Confirming a reading as orphan is done by verifying that the Readmill server
   * responds with a 404 for the given reading.
   */
  private void confirmAndDeleteOrphans(List<LocalReading> maybeOrphanLocalReadings) throws SQLException {
    for(LocalReading localReading : maybeOrphanLocalReadings) {
      if(verifyReadingNotOnReadmill(localReading.readmillReadingId)) {
        Log.i(TAG, "Deleting remotely deleted reading: " + localReading);
        mReadingDao.delete(localReading);
        postProgressUpdateDeletedReading(localReading);
      } else {
        Log.i(TAG, "Could not verify that reading is not readmill: " + localReading);
      }
    }
  }

  /**
   * Delete the remote and the local reading.
   *
   * @param localReadings Local readings to delete
   */
  private void pushDeletions(List<LocalReading> localReadings) throws SQLException, ReadmillException {
    Log.d(TAG, "Deleting " + localReadings.size() + " readings");
    for(LocalReading localReading : localReadings) {
      Log.v(TAG, "Deleting Readmill Reading with id: " + localReading.readmillReadingId);
      mReadmillApi.deleteReading(localReading.readmillReadingId);
      Log.v(TAG, "Reading deleted on Readmill. Deleting locally: " + localReading);
      mReadingDao.delete(localReading);
    }
  }

  /**
   * Push changes from the device into Readmill.
   *
   * Currently only pushes the Readmill privacy.
   *
   * @param localReadings changed readings to push.
   */
  private void pushLocallyChanged(List<LocalReading> localReadings) throws SQLException {
    Log.d(TAG, "Changing privacy on the server for " + localReadings.size() + " readings");
    for(LocalReading localReading : localReadings) {
      Log.v(TAG, "Changing privacy on Readmill for LocalReading: " + localReading.toString());
      mReadmillApi.updateReading(localReading.readmillReadingId, localReading.readmillPrivate);

      // Readmill does not set the touched_at when the reading is changed, so we have
      // to reset the local timestamp to avoid falling into the same comparison (local vs. remote timestamp)
      // on the next sync.
      localReading.setUpdatedAt(null);
      mReadingDao.update(localReading);
    }
  }

  /*
   * Push a local change to Readmill.
   * Currently only supports syncing the state and closing remark.
   */
  private void pushClosedStates(List<LocalReading> localReadings) {
    Log.d(TAG, "Pushing " + localReadings.size() + " readings");
    int totalCount = localReadings.size();
    int currentCount = 0;

    for(LocalReading localReading : localReadings) {
      if(isCancelled()) { return; }
      postProgressUpdateMessage("Updating " + localReading.title, currentCount++, totalCount - 1);
      mReadmillApi.closeReading(localReading.readmillReadingId, localReading.readmillState, localReading.readmillClosingRemark);
    }
  }

  /**
   * Updates highlights on the server with local edits made to highlights.
   *
   * @param editedLocalHighlights List of highlights to push edits for.
   */
  private void pushEditedHighlights(List<LocalHighlight> editedLocalHighlights) throws ReadmillException, SQLException {
    Log.d(TAG, "Pushing " + editedLocalHighlights.size() + " edited highlights");
    for(LocalHighlight localHighlight : editedLocalHighlights) {
      updateHighlight(localHighlight);
    }
  }

  private void updateHighlight(LocalHighlight localHighlight) throws ReadmillException, SQLException {
    try {
      mReadmillApi.updateHighlight(localHighlight.readmillHighlightId, localHighlight.content, localHighlight.position);
      localHighlight.editedAt = null;
      localHighlight.syncedAt = new Date();
      mHighlightDao.update(localHighlight);
    } catch(ReadmillException ex) {
      if(ex.getStatusCode() == 404) {
        Log.d(TAG, "Tried updating a removed highlight. Removing from device.");
        mHighlightDao.delete(localHighlight);
      } else {
        throw ex;
      }
    }
  }

  /**
   * Sends a request to readmill for the given ID and asserts that the response is a 404,
   * and is in fact coming from Readmill (to avoid airport proxy hijacking etc).
   *
   * @param readingId Readmill id to verify
   * @return true if Readmill does not have a reading with the given ID.
   */
  private boolean verifyReadingNotOnReadmill(long readingId) {
    return mReadmillApi.verifyReadingMissing(readingId);
  }

  /**
   * Determine if the local reading has been closed locally, but is not yet
   * closed on Readmill.
   *
   * @param localReading  the local reading
   * @param remoteReading the co-responding remote reading
   * @return true if the the local reading should be pushed to Readmill
   * @throws org.json.JSONException if the response from Readmill is not properly formatted
   */
  private boolean closedLocallyButNotRemotely(LocalReading localReading, JSONObject remoteReading) throws JSONException {
    boolean isClosedLocally = localReading.hasClosedAt();

    String remoteState = remoteReading.getString("state");
    boolean isClosedRemotely = remoteState.equals("finished") || remoteState.equals("abandoned");

    return isClosedLocally && !isClosedRemotely;
  }

  /**
   * Check if a local reading has data that has been changed by the user and not
   * yet pushed to the server.
   * @param localReading LocalReading to check
   * @param remoteReading Reading object on readmill to compare against
   * @return true if the local reading has any changes that should be pushed.
   * @throws org.json.JSONException if the readmill object is not properly formatted
   */
  private boolean hasLocalChanges(LocalReading localReading, JSONObject remoteReading) throws JSONException {
    // Currently the only local data that can fall out of sync (except the state)
    // is privacy. Extend this guard if/when more fields are added later.
    if(localReading.readmillPrivate == remoteReading.getBoolean("private")) {
      return false;
    }

    // Push a local change only if it's newer than the servers. This relies on
    // the fact that the device time is (somewhat) correct.
    long locallyChangedAt = (localReading.getUpdatedAt().getTime() / 1000); // Convert to seconds
    long remotelyChangedAt = ReadmillApiHelper.parseISO8601ToUnix(remoteReading.getString("touched_at"));
    return locallyChangedAt > remotelyChangedAt;
  }

  /**
   * Syncs all reading sessions between from a remote reading to a local reading
   *
   * @param localReading  LocalReading to sync reading sessions for
   * @param remoteReading Readmill Reading for which to get sessions
   * @throws org.json.JSONException if the remote reading is not in the expected format
   */
  private void syncReadingSessions(LocalReading localReading, JSONObject remoteReading) throws JSONException, SQLException {
    long remoteId = remoteReading.getLong("id");
    Log.i(TAG, "Syncing reading sessions for reading with id:" + remoteId);

    ArrayList<LocalSession> localSessions = (ArrayList<LocalSession>) mSessionDao.queryBuilder()
      .where().eq(LocalSession.READING_ID_FIELD_NAME, localReading.id)
      .query();

    ArrayList<JSONObject> remoteSessions = mReadmillApi.getPeriodsForReadingId(remoteId);

    localSessions = mergeSessions(localReading, remoteSessions, localSessions);

    updateLocalReadingFromSessions(localReading, localSessions);
  }

  /**
   * Syncs highlights from a remote reading to a local reading.
   *
   * @param localReading  local reading
   * @param remoteReading remote reading
   * @return true if the sync was successful, false otherwise
   * @throws org.json.JSONException if the response from Readmill is not properly formatted
   */
  private boolean syncHighlights(LocalReading localReading,
                                 JSONObject remoteReading) throws JSONException, SQLException {
    long remoteId = remoteReading.getLong("id");
    Log.i(TAG, "Syncing all highlights for local reading " + localReading.toString() + " with remote reading: " + remoteId);

    ArrayList<LocalHighlight> localHighlights = (ArrayList<LocalHighlight>) mHighlightDao.queryBuilder()
      .where()
      .eq(LocalHighlight.READMILL_READING_ID_FIELD_NAME, remoteId)
      .query();

    ArrayList<JSONObject> remoteHighlights = mReadmillApi.getHighlightsWithReadingId(remoteId);

    mergeHighlights(localReading, localHighlights, remoteHighlights);
    return true;
  }

  /**
   * Merges a list of local highlights with a list of remote highlights.
   * <p/>
   * Highlights that are in the remote list but not in the local set are added to the device.
   *
   * @param localReading     LocalReading that owns the local highlights
   * @param localHighlights  list of local highlights
   * @param remoteHighlights list of remote highlights
   * @throws org.json.JSONException if the Readmill response is not properly formatted
   */
  private void mergeHighlights(LocalReading localReading,
                               List<LocalHighlight> localHighlights,
                               ArrayList<JSONObject> remoteHighlights) throws JSONException, SQLException {
    if(remoteHighlights == null || localHighlights == null) {
      Log.d(TAG, "Received NULL list - aborting");
      return;
    }

    Log.i(TAG, "Merging " + localHighlights.size() +
            " local highlights with " + remoteHighlights.size() +
            " remote highlights for reading " + localReading.id);

    // Store all local highlights in a cache for fast access
    HashMap<Long, LocalHighlight> localIds = new HashMap<Long, LocalHighlight>(localHighlights.size());
    for(LocalHighlight highlight : localHighlights) {
      localIds.put(highlight.readmillHighlightId, highlight);
    }

    for(JSONObject remoteHighlight : remoteHighlights) {
      long remoteId = remoteHighlight.getLong("id");
      LocalHighlight localHighlight = localIds.get(remoteId);
      if(localHighlight == null) { // Create highlight

        String remoteContent = remoteHighlight.getString("content");
        Log.i(TAG, "Adding highlight: " + remoteId + " " + remoteContent);

        LocalHighlight spawn = ReadmillConverter.createHighlightFromReadmillJSON(remoteHighlight);

        // Attributes only available to ReadTracker
        spawn.syncedAt = new Date();
        spawn.readingId = localReading.id;

        mHighlightDao.create(spawn);
        Log.d(TAG, "   Created Highlight: " + spawn.toString());
      } else { // Update highlight
        ReadmillConverter.mergeLocalHighlightWithJson(localHighlight, remoteHighlight);
        localHighlight.syncedAt = new Date();

        mHighlightDao.update(localHighlight);
        Log.d(TAG, "   Updated Highlight: " + localHighlight.toString());
      }
    }
  }

  /**
   * Gets a list of local readings that matches the remote user id.
   *
   * @param remoteUserId Remote user id to get local reading data for
   * @return the list of matched local readings
   */
  private ArrayList<LocalReading> getAllConnectedLocalReadingForUserId(long remoteUserId) throws SQLException {
    Where<LocalReading, Integer> stmt = mReadingDao.queryBuilder().where().
      eq(LocalReading.READMILL_USER_ID_FIELD_NAME, remoteUserId).
      and().
      gt(LocalReading.READMILL_READING_ID_FIELD_NAME, 0);
    ArrayList<LocalReading> result = (ArrayList<LocalReading>) stmt.query();
    Log.i(TAG, "Found " + result.size() + " local connected readings");
    return result;
  }

  /**
   * Gets a list of local highlights that are connected to Readmill and have been edited.
   *
   * @return all connected and edited highlights
   */
  public ArrayList<LocalHighlight> getEditedConnectedHighlights() throws SQLException {
    Where<LocalHighlight, Integer> stmt = mHighlightDao.queryBuilder()
      .where()
      .gt(LocalHighlight.READMILL_READING_ID_FIELD_NAME, 0)
      .and()
      .isNotNull(LocalHighlight.EDITED_AT_FIELD_NAME);

    ArrayList<LocalHighlight> result = (ArrayList<LocalHighlight>) stmt.query();
    Log.i(TAG, "Found " + result.size() + " connected highlights with edits");
    return result;
  }

  /**
   * Get all readings for a given user from Readmill.
   *
   * @param remoteUserId readmill user id to fetch readings for
   * @return the list of user readings
   * @throws ReadmillException if the request to readmill was not successful
   */
  private ArrayList<JSONObject> getAllRemoteReadingsForUserId(long remoteUserId) throws ReadmillException {
    ArrayList<JSONObject> result = mReadmillApi.getReadingsForUserId(remoteUserId);
    Log.i(TAG, "Found " + result.size() + " remote readings");
    return result;
  }

  /**
   * Loop through all reading sessions and update the correct time spent,
   * as well as the most recent progress.
   *
   * @param localReading LocalReading to update
   * @param sessions     A prefetched list of Reading Sessions for the given LocalReading
   */
  private void updateLocalReadingFromSessions(LocalReading localReading, List<LocalSession> sessions) throws SQLException {
    long totalSpentSeconds = 0;
    LocalSession mostRecentSession = null;

    for(LocalSession session : sessions) {
      totalSpentSeconds += session.durationSeconds;
      if(mostRecentSession == null || session.occurredAt.after(mostRecentSession.occurredAt)) {
        mostRecentSession = session;
      }
    }

    localReading.timeSpentMillis = totalSpentSeconds * 1000;

    // Set the progress of the reading to that of the latest reading
    if(mostRecentSession != null) {
      if(mostRecentSession.endedOnPage == -1) {
        Log.d(TAG, "Updating reading progress to progress of most recent session: " + mostRecentSession.progress + " | page " + (long) Math.floor(mostRecentSession.progress * localReading.totalPages));
        localReading.currentPage = (long) Math.floor(mostRecentSession.progress * localReading.totalPages);
      } else {
        localReading.currentPage = mostRecentSession.endedOnPage;
      }
    }

    localReading.setProgressStops(sessions);

    mReadingDao.update(localReading);
  }

  /**
   * Merges a set of local reading sessions with a set of remote reading
   * sessions (Readmill periods).
   *
   * @param localReading   Parent reading data for the local reading sessions
   * @param remoteSessions local reading sessions
   * @param localSessions  remote reading sessions (Readmill periods)
   * @return the list of updated local sessions
   * @throws org.json.JSONException if any of the remote sessions is not correctly formatted
   */
  private ArrayList<LocalSession> mergeSessions(LocalReading localReading,
                                                ArrayList<JSONObject> remoteSessions,
                                                List<LocalSession> localSessions)
    throws JSONException, SQLException {
    Log.d(TAG, "Merging " + remoteSessions.size() + " remote sessions with " + localSessions.size() + " local sessions");

    ArrayList<LocalSession> createdSessions = new ArrayList<LocalSession>();

    for(JSONObject remoteSession : remoteSessions) {
      // Figure out if we need to create the reading session locally or not
      boolean foundLocal = false;
      // TODO Use a set here instead for faster look ups
      for(LocalSession localSession : localSessions) {
        String remoteSessionIdentifier = remoteSession.getString("identifier");
        if(localSession.sessionIdentifier != null &&
          localSession.sessionIdentifier.equals(remoteSessionIdentifier)) {
          foundLocal = true;
          break;
        }
      }

      // session already available so no need to create a new one
      if(foundLocal) { continue; }

      LocalSession spawn = ReadmillConverter.createReadingSessionFromReadmillPeriod(remoteSession);
      spawn.readingId = localReading.id;
      spawn.syncedWithReadmill = true;

      mSessionDao.create(spawn);
      Log.d(TAG, "Created Session");
      createdSessions.add(spawn);
    }

    createdSessions.addAll(localSessions);
    return createdSessions;
  }

  /**
   * Deletes all highlights that have been marked as deleted by the user.
   */
  private void deleteMarkedHighlights() {
    Log.d(TAG, "deleteMarkedHighlights()");
    try {
      Dao<LocalHighlight, Integer> highlightDao = ApplicationReadTracker.getHighlightDao();
      Where<LocalHighlight, Integer> stmt = highlightDao.queryBuilder().where()
        .gt(LocalHighlight.READMILL_READING_ID_FIELD_NAME, 0)
        .and()
        .eq(LocalHighlight.DELETED_BY_USER_FIELD_NAME, true);

      List<LocalHighlight> highlightsToDelete = stmt.query();

      if(highlightsToDelete.size() < 1) {
        Log.i(TAG, "No highlights to delete. Exiting.");
        return;
      }

      Log.i(TAG, "Found " + highlightsToDelete.size() + " highlights marked for deletion");

      for(LocalHighlight highlight : highlightsToDelete) {
        Log.d(TAG, "Deleting highlight with readmill Id: " + highlight.readmillHighlightId + " url:" + highlight.readmillPermalinkUrl);

        try {
          mReadmillApi.deleteHighlight(highlight.readmillHighlightId);
        } catch(ReadmillException e) {
          Log.w(TAG, "Failed to delete highlight: " + highlight, e);
          if(e.getStatusCode() == 404) {
            Log.d(TAG, "Was 404, still deleting locally");
          } else {
            continue; // skip to next highlight
          }
        }

        Highlights.delete(highlight);
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed get highlights from database", e);
    }
  }

  /**
   * Upload all readings that are yet not connected to Readmill
   */
  private void uploadNewReadings() {
    Log.v(TAG, "uploadNewReadings()");

    try {
      Dao<LocalReading, Integer> readingDao = ApplicationReadTracker.getReadingDao();

      // Fetch readings that have an associated reading (not anonymous readings)
      // that are not yet connected to Readmill
      Where<LocalReading, Integer> stmt = readingDao.queryBuilder().where()
        .le(LocalReading.READMILL_READING_ID_FIELD_NAME, 0)
        .and()
        .gt(LocalHighlight.READMILL_USER_ID_FIELD_NAME, 0);

      List<LocalReading> readingsToPush = stmt.query();

      if(readingsToPush.size() < 1) {
        Log.v(TAG, "No new readings found.");
        return;
      }

      Log.i(TAG, "Pushing " + readingsToPush.size() + " new readings");

      for(LocalReading localReading : readingsToPush) {
        Log.d(TAG, "Pushing reading: " + localReading.getInfo());

        JSONObject jsonBook = null, jsonReading = null;

        try {
          jsonBook = mReadmillApi.createBook(localReading.title, localReading.author);

          final Date startedAt = localReading.hasStartedAt() ? localReading.getStartedAt() : new Date();
          final long id = jsonBook.getLong("id");
          final boolean isPublic = !localReading.readmillPrivate;
          jsonReading = mReadmillApi.createReading(id, isPublic, startedAt);

          // Keep the provided cover if any
          if(localReading.coverURL == null) {
            localReading.coverURL = jsonBook.getString("cover_url");
          }

          // Prevent a closed reading from being updated to reading by the
          // readmill create call.
          boolean wasClosed = localReading.isClosed();
          int previousState = localReading.readmillState;
          String previousClosingRemark = localReading.getClosingRemark();
          boolean wasRecommended = localReading.readmillRecommended;

          // Include data from Readmill
          ReadmillConverter.mergeLocalReadingWithJSON(localReading, jsonReading);

          // Put back the local data if necessary and let the sync task handle
          // updating the Reading at a later point
          if(wasClosed) {
            localReading.readmillClosingRemark = previousClosingRemark;
            localReading.readmillState = previousState;
            localReading.readmillRecommended = wasRecommended;
          }

          // Store locally
          readingDao.createOrUpdate(localReading);

          updateReadmillReadingForSessionsOf(localReading);
          updateReadmillReadingForHighlightsOf(localReading);
        } catch(ReadmillException e) {
          Log.w(TAG, "Failed to connect book to readmill", e);
        } catch(JSONException e) {
          Log.w(TAG, "Unexpected result from Readmill when creating LocalReading. book: " +
                  dumpJSON(jsonBook) + " and reading: " + dumpJSON(jsonReading), e);
        } catch(SQLException e) {
          Log.w(TAG, "SQL Error while trying to save LocalReading", e);
        }
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed to save local reading", e);
    }
  }

  /**
   * Upload all new highlights that are not yet on Readmill
   */
  private void uploadNewHighlights() {
    try {
      Dao<LocalHighlight, Integer> highlightDao = ApplicationReadTracker.getHighlightDao();
      Where<LocalHighlight, Integer> stmt = highlightDao.queryBuilder().where()
        .isNull(LocalHighlight.SYNCED_AT_FIELD_NAME)
        .and()
        .eq(LocalHighlight.DELETED_BY_USER_FIELD_NAME, false)
        .and()
        .gt(LocalHighlight.READMILL_READING_ID_FIELD_NAME, 0);

      List<LocalHighlight> highlightsToPush = stmt.query();

      if(highlightsToPush.size() < 1) {
        Log.i(TAG, "No new highlights found. Exiting.");
        return;
      }

      Log.i(TAG, "Found " + highlightsToPush.size() + " new highlights");

      for(LocalHighlight highlight : highlightsToPush) {
        Log.d(TAG, "Processing highlight with local id: " + highlight.id + " highlighted at: " + highlight.highlightedAt + " with content: " + highlight.content + " at position: " + highlight.position);

        try {
          JSONObject readmillHighlight = mReadmillApi.createHighlight(highlight);
          Log.d(TAG, "Marking highlight with id: " + highlight.id + " as synced");
          ReadmillConverter.mergeLocalHighlightWithJson(highlight, readmillHighlight);
          highlight.syncedAt = new Date();
          highlightDao.update(highlight);
        } catch(ReadmillException e) {
          Log.w(TAG, "Failed to upload highlight: " + highlight, e);
        } catch(JSONException e) {
          Log.w(TAG, "Failed to update highlight due to malformed response from Readmill", e);
        }
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed to persist unsent highlights", e);
    }
  }

  /**
   * Transfers all sessions on the device that have not been marked as already
   * being synced.
   * <p/>
   * Note that this transfers all sessions, not just the ones for the current
   * user.
   */
  private void uploadNewSessions() {
    try {
      Dao<LocalSession, Integer> sessionDao = ApplicationReadTracker.getSessionDao();
      Where<LocalSession, Integer> stmt = sessionDao.queryBuilder().where()
        .eq(LocalSession.SYNCED_WITH_READMILL_FIELD_NAME, false)
        .and()
        .gt(LocalSession.READMILL_READING_ID_FIELD_NAME, 0);

      List<LocalSession> sessionsToProcess = stmt.query();

      if(sessionsToProcess.size() < 1) {
        Log.i(TAG, "No unprocessed sessions to send.");
        return;
      }

      Log.i(TAG, "Sending " + sessionsToProcess.size() + " new sessions to readmill");
      for(LocalSession session : sessionsToProcess) {
        syncWithRemote(session);
        sessionDao.update(session);
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed to get a DAO for queued pings", e);
    }
  }

  /**
   * Sync a session to Readmill.
   * <p/>
   * Potentially modifies the provided ReadingSession with a new state as a
   * result of the sync (marking it as synced or in need of reconnect).
   *
   * @param session ReadingSession to sync
   */
  private void syncWithRemote(LocalSession session) {
    Log.d(TAG, "Processing session with id: " + session.id +
            " occurred at: " + session.occurredAt +
            " session id " + session.sessionIdentifier);

    try {
      mReadmillApi.createPing(
        session.sessionIdentifier,
        session.readmillReadingId,
        session.progress,
        session.durationSeconds,
        session.occurredAt
      );
      Log.d(TAG, "Marking session with id: " + session.id + " as synced");
      session.syncedWithReadmill = true;
    } catch(ReadmillException e) {
      // Keep the local data if the reading has been removed on reading, or
      // if the token has expired, but mark it as needing a reconnect to avoid
      // repeatedly trying to re-send it.
      int status = e.getStatusCode();
      if(status == 404 || status == 401) {
        Log.d(TAG, "Marking session with id: " + session.id + " as needing reconnect");
        session.needsReconnect = true;
      } else if(status == 422) {
        Log.w(TAG, "Server did not accept session. Stop trying to sync it.", e);
        session.syncedWithReadmill = true;
      } else {
        Log.w(TAG, "Failed to upload Readmill Session", e);
        // Do not modify the session at all, causing it to be picked up and
        // retried on the next sync
      }
    }
  }

  /**
   * Update all sessions of a LocalReading with the Readmill Reading id.
   *
   * @param localReading LocalReading to update sessions for
   *
   * TODO This is horribly hackish. Should be - at the very least - a method on the LocalReading
   */
  private void updateReadmillReadingForSessionsOf(LocalReading localReading) {
    Log.d(TAG, "updateReadmillReadingForSessionsOf" + localReading.toString());
    if(localReading.readmillReadingId < 1) {
      return;
    }

    try {
      Dao<LocalSession, Integer> sessionDao = ApplicationReadTracker.getSessionDao();
      Where<LocalSession, Integer> stmt = sessionDao.queryBuilder().where()
        .eq(LocalSession.READING_ID_FIELD_NAME, localReading.id)
        .and()
        .lt(LocalSession.READMILL_READING_ID_FIELD_NAME, 1);

      List<LocalSession> sessionsToProcess = stmt.query();

      if(sessionsToProcess.size() < 1) {
        Log.i(TAG, "No sessions for LocalReading " + localReading.toString() + " needs connecting");
        return;
      }

      for(LocalSession session : sessionsToProcess) {
        session.readmillReadingId = localReading.readmillReadingId;
        sessionDao.update(session);
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed to get a DAO for queued pings", e);
    }
  }

  private void updateReadmillReadingForHighlightsOf(LocalReading localReading) {
    if(localReading == null || localReading.readmillReadingId < 1) {
      return;
    }

    Log.d(TAG, "Searching for highlights without readmill reading id for local reading " + localReading.toString());

    try {
      Dao<LocalHighlight, Integer> highlightDao = ApplicationReadTracker.getHighlightDao();
      Where<LocalHighlight, Integer> stmt = highlightDao.queryBuilder().where()
        .eq(LocalHighlight.READING_ID_FIELD_NAME, localReading.id)
        .and()
        .lt(LocalHighlight.READMILL_READING_ID_FIELD_NAME, 1);

      List<LocalHighlight> highlightsToProcess = stmt.query();

      if(highlightsToProcess.size() < 1) {
        Log.i(TAG, "No highlights without readmill reading id for reading " + localReading.toString() + " found. Exiting.");
        return;
      }

      Log.i(TAG, "Updating " + highlightsToProcess.size() + " highlights");

      for(LocalHighlight highlight : highlightsToProcess) {
        Log.d(TAG, "Processing highlight with local id: " + highlight.id + " highlighted at: " + highlight.highlightedAt + " with content: " + highlight.content + " at position: " + highlight.position);
        highlight.readmillReadingId = localReading.readmillReadingId;
        highlightDao.update(highlight);
      }
    } catch(SQLException e) {
      Log.d(TAG, "Failed to persist unsent highlights", e);
    }
  }
}

