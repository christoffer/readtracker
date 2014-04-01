package com.readtracker.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.SettingsKeys;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.SaveLocalReadingListener;
import com.readtracker.android.tasks.SaveLocalReadingTask;

public class BookSettingsActivity extends PreferenceActivity {
  private static final String TAG = BookSettingsActivity.class.getName();

  public static final int RESULT_DELETED_BOOK = RESULT_FIRST_USER + 1;

  private LocalReading mLocalReading;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.book_settings);

    if(savedInstanceState == null) {
      if(!getIntent().hasExtra(IntentKeys.LOCAL_READING)) {
        throw new IllegalArgumentException("Missing reading id in the intent");
      }
      mLocalReading = getIntent().getParcelableExtra(IntentKeys.LOCAL_READING);
    } else {
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
    }

    Preference prefDeleteBook = findPreference(SettingsKeys.BOOK_DELETE_BOOK);
    Preference prefEditBookPages = findPreference(SettingsKeys.BOOK_EDIT_PAGES);

    //noinspection ConstantConditions
    prefDeleteBook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        showConfirmDeleteDialog();
        return true;
      }
    });

    //noinspection ConstantConditions
    prefEditBookPages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        exitWithRequestPageEdit();
        return true;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
  }

  private void toastDeleted() {
    String message = getString(R.string.book_settings_book_deleted);
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private void showConfirmDeleteDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.book_settings_delete_book_title));

    if(mLocalReading.isConnected()) {
      builder.setMessage(R.string.delete_reading_connected);
    } else {
      builder.setMessage(R.string.delete_reading_unconnected);
    }

    builder.setCancelable(true);

    builder.setPositiveButton(R.string.general_delete, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int id) {
        deleteReading(mLocalReading);
      }
    });

    builder.setNegativeButton(R.string.general_cancel, null);

    builder.show();
  }

  private void deleteReading(LocalReading localReading) {
    Log.i(TAG, "Deleting reading: " + localReading);
    localReading.deletedByUser = true;
    SaveLocalReadingTask.save(localReading, new SaveLocalReadingListener() {
      @Override public void onLocalReadingSaved(LocalReading localReading) {
        toastDeleted();
        setResult(ActivityCodes.RESULT_DELETED_BOOK);
        finish();
      }
    });
  }

  private void exitWithRequestPageEdit() {
    Intent data = new Intent();
    // We need to pass the reading back here since the original activity
    // might have been destroyed, and thus lost it's reference to the LocalReading.
    data.putExtra(IntentKeys.LOCAL_READING, mLocalReading);
    setResult(ActivityCodes.RESULT_REQUESTED_BOOK_SETTINGS, data);
    finish();
  }
}
