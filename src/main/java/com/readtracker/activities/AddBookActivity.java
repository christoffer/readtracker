package com.readtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.SaveLocalReadingListener;
import com.readtracker.support.ReadTrackerUser;
import com.readtracker.tasks.SaveLocalReadingTask;
import com.readtracker.thirdparty.views.Switch;

import java.util.Date;

/**
 * Screen for adding a new book manually
 */
public class AddBookActivity extends ReadTrackerActivity {
  public static final String TAG = AddBookActivity.class.getName();

  private LocalReading mLocalReading;

  private static EditText mEditTitle;
  private static EditText mEditAuthor;
  private static EditText mEditPageCount;

  private static Button mButtonAddBook;

  private static Switch mSwitchPagesPercent;
  private static Switch mSwitchPublicPrivate;

  private static TextView mTextReadmillPrivacyHint;

  private static ViewGroup mLayoutReadmill;

  private boolean mCameFromReadingSession = false;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);

    bindViews();
    bindEvents();

    if(getCurrentUser() == null) {
      // No Readmill connection setup, hide related settings
      findViewById(R.id.layoutReadmill).setVisibility(View.GONE);
      findViewById(R.id.textReadmillPrivacyHint).setVisibility(View.GONE);
    }

    Bundle extras = getIntent().getExtras();
    if(extras != null) {
      LocalReading localReading = extras.getParcelable(IntentKeys.LOCAL_READING);

      if(localReading != null) {
        setupEditMode(localReading);
      } else {
        setupCreateMode(extras);
      }

      mCameFromReadingSession = extras.getBoolean(IntentKeys.FROM_READING_SESSION, false);
      if(mCameFromReadingSession) {
        mEditPageCount.requestFocus();
      } else {

      }
    }
  }

  private void bindViews() {
    mEditTitle = (EditText) findViewById(R.id.editTitle);
    mEditAuthor = (EditText) findViewById(R.id.editAuthor);
    mEditPageCount = (EditText) findViewById(R.id.editPageCount);
    mSwitchPagesPercent = (Switch) findViewById(R.id.togglePagesPercent);
    mSwitchPublicPrivate = (Switch) findViewById(R.id.togglePublicPrivate);
    mButtonAddBook = (Button) findViewById(R.id.buttonAddBook);
    mTextReadmillPrivacyHint = (TextView) findViewById(R.id.textReadmillPrivacyHint);
    mLayoutReadmill = (ViewGroup) findViewById(R.id.layoutReadmill);
  }

  private void bindEvents() {
    mButtonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onAddBookClicked();
      }
    });

    mSwitchPagesPercent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        mEditPageCount.setEnabled(checked);
        String rememberedValue = (String) mEditPageCount.getTag();
        if(checked && rememberedValue != null) {
          mEditPageCount.setText(rememberedValue);
        } else {
          mEditPageCount.setTag(mEditPageCount.getText().toString());
          mEditPageCount.setText("100.00%");
        }
      }
    });

    mSwitchPublicPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        final int textResource = checked ? R.string.readmill_shared : R.string.readmill_private;
        mTextReadmillPrivacyHint.setText(getString(textResource));
      }
    });
  }

  private void setupCreateMode(Bundle extras) {
    mButtonAddBook.setText("Add");
    mSwitchPublicPrivate.setVisibility(View.VISIBLE);

    mEditTitle.setText(extras.getString(IntentKeys.TITLE));
    mEditAuthor.setText(extras.getString(IntentKeys.AUTHOR));
    mCoverURL = extras.getString(IntentKeys.COVER_URL);
    setInitialPageCount(extras.getLong(IntentKeys.PAGE_COUNT, 0));
  }

  private void setupEditMode(LocalReading localReading) {
    mButtonAddBook.setText("Save");

    mEditTitle.setEnabled(false);
    mEditAuthor.setEnabled(false);
    mLayoutReadmill.setVisibility(View.GONE);
    mTextReadmillPrivacyHint.setVisibility(View.GONE);

    mLocalReading = localReading;

    mEditTitle.setText(localReading.title);
    mEditAuthor.setText(localReading.author);

    long pageCount = localReading.totalPages;
    setInitialPageCount(pageCount);
    mCoverURL = localReading.coverURL;
  }

  private void setInitialPageCount(long pageCount) {
    if(pageCount > 0) {
      mEditPageCount.setText(Long.toString(pageCount));
    }
  }

  private void onAddBookClicked() {
    if(!validateFields()) {
      return;
    }

    LocalReading localReading = mLocalReading == null ? new LocalReading() : mLocalReading;

    localReading.title = mEditTitle.getText().toString();
    localReading.author = mEditAuthor.getText().toString();
    localReading.startedAt = (new Date()).getTime();
    localReading.coverURL = mCoverURL;

    ReadTrackerUser user = getApp().getCurrentUser();
    if(user != null && mLocalReading == null) {

      Log.v(TAG, "Creating a reading that should be connected to Readmill. " +
        "Setting user id: " + user.getReadmillId() +
        " and privacy: " + (mSwitchPublicPrivate.isChecked() ? "public" : "private")
      );

      // Creating a reading that will connect to Readmill
      localReading.readmillUserId = getApp().getCurrentUser().getReadmillId();
      localReading.readmillPrivate = !mSwitchPublicPrivate.isChecked();
    }

    if(mSwitchPagesPercent.isChecked()) {
      localReading.totalPages = Integer.parseInt(mEditPageCount.getText().toString());
      localReading.measureInPercent = false;
    } else {
      localReading.totalPages = 1000;
      localReading.measureInPercent = true;
    }

    saveLocalReading(localReading);
  }

  /**
   * Validates the input fields and moves focus to any invalid ones.
   *
   * @return true if all fields were valid, otherwise false
   */
  private boolean validateFields() {
    if(mEditTitle.getText().length() < 1) {
      toast("Please enter the title");
      mEditTitle.requestFocus();
      return false;
    }

    if(mEditAuthor.getText().length() < 1) {
      toast("Please enter the author");
      mEditAuthor.requestFocus();
      return false;
    }

    // Validate a reasonable amount of page numbers
    if(mSwitchPagesPercent.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mEditPageCount.getText().toString());
      } catch(NumberFormatException ignored) {
      }

      if(pageCount < 1) {
        toast(getString(R.string.enter_page_count));
        mEditPageCount.requestFocus();
        return false;
      }
    }

    return true;
  }

  private void exitToReadingSession(LocalReading localReading) {
    Intent readingSessionIntent = new Intent(this, BookActivity.class);
    if(mCameFromReadingSession) {
      Intent data = new Intent();
      data.putExtra(IntentKeys.READING_ID, localReading.id);
      setResult(ActivityCodes.RESULT_OK, data);
    } else {
      readingSessionIntent.putExtra(IntentKeys.READING_ID, localReading.id);
      startActivity(readingSessionIntent);
      setResult(ActivityCodes.RESULT_OK);
    }
    finish();
  }

  private void saveLocalReading(LocalReading localReading) {
    getApp().showProgressDialog(this, "Saving book...");
    SaveLocalReadingTask.save(localReading, new SaveLocalReadingListener() {
      @Override public void onLocalReadingSaved(LocalReading localReading) {
        getApp().clearProgressDialog();
        exitToReadingSession(localReading);
      }
    });
  }
}
