package com.readtracker_beta.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import com.readtracker_beta.IntentKeys;
import com.readtracker_beta.R;
import com.readtracker_beta.SettingsKeys;
import com.readtracker_beta.db.LocalReading;
import com.readtracker_beta.interfaces.SaveLocalReadingListener;
import com.readtracker_beta.tasks.SaveLocalReadingTask;

public class BookSettingsActivity extends PreferenceActivity {
  private static final String TAG = BookSettingsActivity.class.getName();

  private LocalReading mLocalReading;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.book_settings);

    if(savedInstanceState == null) {
      mLocalReading = getIntent().getExtras().getParcelable(IntentKeys.LOCAL_READING);
    } else {
      mLocalReading = savedInstanceState.getParcelable(IntentKeys.LOCAL_READING);
    }

    CheckBoxPreference checkboxReadmillPrivacy = (CheckBoxPreference) findPreference(SettingsKeys.READMILL_PRIVACY);
    CheckBoxPreference checkboxReadmillSharing = (CheckBoxPreference) findPreference(SettingsKeys.READMILL_SHARING);
    Preference prefDeleteBook = (Preference) findPreference(SettingsKeys.OTHER_DELETE_BOOK);

    checkboxReadmillPrivacy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object o) {
        toastNotImplemented();
        return false;
      }
    });

    checkboxReadmillSharing.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object o) {
        toastNotImplemented();
        return false;
      }
    });

    prefDeleteBook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        showConfirmDeleteDialog();
        return true;
      }
    });
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
  }

  private void toastNotImplemented() {
    Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
  }

  private void toastDeleted(boolean isConnected) {
    String message = "Book deleted.";

    if(isConnected) {
      message += " Changes to Readmill will be applied on next sync.";
    }

    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private void showConfirmDeleteDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Delete book and all data?");

    if(mLocalReading.isConnected()) {
      builder.setMessage(R.string.delete_reading_connected);
    } else {
      builder.setMessage(R.string.delete_reading_unconnected);
    }

    builder.setCancelable(true);

    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int id) {
        deleteReading(mLocalReading);
      }
    });

    builder.setNegativeButton("Cancel", null);

    builder.show();
  }

  private void deleteReading(LocalReading localReading) {
    Log.i(TAG, "Deleting reading: " + localReading);
    localReading.deletedByUser = true;
    SaveLocalReadingTask.save(localReading, new SaveLocalReadingListener() {
      @Override public void onLocalReadingSaved(LocalReading localReading) {
        toastDeleted(localReading.isConnected());
        setResult(ActivityCodes.RESULT_DELETED_BOOK);
        finish();
      }
    });
  }
}
