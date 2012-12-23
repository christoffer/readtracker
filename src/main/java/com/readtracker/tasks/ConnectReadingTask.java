package com.readtracker.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.readtracker.ApplicationReadTracker;
import com.readtracker.interfaces.ConnectedReadingListener;
import com.readtracker.helpers.ReadmillConverter;
import com.readtracker.db.LocalReading;
import com.readtracker.helpers.ReadmillException;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

import static com.readtracker.helpers.ReadmillApiHelper.dumpJSON;

/**
 * Connects a local reading to a remote reading on Readmill.
 */
public class ConnectReadingTask {
  private static final String TAG = ConnectReadingTask.class.getName();

  public LocalReading localReading;
  public boolean isPublic;

  private ConnectReadingTask(LocalReading localReading, boolean isPublic) {
    this.localReading = localReading;
    this.isPublic = isPublic;
  }

  /**
   * Connects a given local reading to Readmill.
   *
   * @param localReading the local reading to connect
   * @param isPublic     Connect the reading as public or not
   * @param listener     listener for when a connection has been established
   */
  public static void connect(LocalReading localReading, boolean isPublic, ConnectedReadingListener listener) {
    ConnectReadingTask instance = new ConnectReadingTask(localReading, isPublic);
    ConnectReadingAsyncTask task = new ConnectReadingAsyncTask(listener);
    task.execute(instance);
  }

  /**
   * Async task for matching a local reading to a remote reading in the background.
   * The local reading is updated with information from Readmill and then saved
   * (or created).
   */
  private static class ConnectReadingAsyncTask extends AsyncTask<ConnectReadingTask, String, LocalReading> {
    private ConnectedReadingListener mListener;

    ConnectReadingAsyncTask(ConnectedReadingListener listener) {
      mListener = listener;
    }

    @Override
    protected LocalReading doInBackground(ConnectReadingTask... tasks) {
      ConnectReadingTask task = tasks[0];

      LocalReading localReading = task.localReading;
      boolean isPublic = task.isPublic;

      JSONObject jsonBook = null, jsonReading = null;

      try {
        // Create the book on Readmill if one was not provided
        jsonBook = createBookOnReadmill(localReading.title, localReading.author);

        // Start reading it on Readmill
        jsonReading = ApplicationReadTracker.getReadmillApi().
            createReading(jsonBook.getLong("id"), isPublic);

        // Prefer a provided cover over the one that is on Readmill
        if(localReading.coverURL == null) {
          localReading.coverURL = jsonBook.getString("cover_url");
        }

        // Include data from Readmill
        ReadmillConverter.mergeLocalReadingWithJSON(localReading, jsonReading);

        // Store locally
        ApplicationReadTracker.getReadingDao().createOrUpdate(localReading);
        return localReading;
      } catch(ReadmillException e) {
        Log.w(TAG, "Failed to create ReadmillBook", e);
      } catch(JSONException e) {
        Log.w(TAG, "Unexpected result from Readmill when creating LocalReading. book: " +
            dumpJSON(jsonBook) + " and reading: " + dumpJSON(jsonReading), e);
      } catch(SQLException e) {
        Log.w(TAG, "SQL Error while trying to persist LocalReading", e);
      }
      return null;
    }

    /**
     * Sends a request to readmill to create a book with given title and author
     *
     * @param title      Title of the book
     * @param authorName name of the author
     * @return the JSONObject received from Readmill
     * @throws ReadmillException if the request was not successful
     */
    private JSONObject createBookOnReadmill(String title, String authorName) throws ReadmillException {
      return ApplicationReadTracker.getReadmillApi().createBook(title, authorName);
    }

    protected void onPostExecute(LocalReading localReading) {
      mListener.onLocalReadingConnected(localReading);
    }
  }
}
