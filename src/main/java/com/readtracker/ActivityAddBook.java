package com.readtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.readtracker.db.LocalReading;
import com.readtracker.interfaces.SaveLocalReadingListener;
import com.readtracker.tasks.ConnectReadingTask;
import com.readtracker.interfaces.ConnectedReadingListener;
import com.readtracker.tasks.SaveLocalReadingTask;

/**
 * Screen for adding a new book manually
 */
public class ActivityAddBook extends ReadTrackerActivity {
  public static final String TAG = ActivityAddBook.class.getName();

  private static LinearLayout mLayoutConnect;

  private static EditText mEditTitle;
  private static EditText mEditAuthor;
  private static EditText mEditPageCount;

  private static ToggleButton mTogglePagesPercent;
  private static ToggleButton mTogglePublicPrivate;

  // Store the cover url from the intent that starts the activity
  private String mCoverURL;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_book);

    bindViews();
    bindEvents();

    if(getCurrentUser() == null) {
      mLayoutConnect.setVisibility(View.GONE);
    }

    Bundle extras = getIntent().getExtras();
    if(extras != null) {
      mEditTitle.setText(extras.getString(IntentKeys.TITLE));
      mEditAuthor.setText(extras.getString(IntentKeys.AUTHOR));

      mCoverURL = extras.getString(IntentKeys.COVER_URL);

      long pageCount = extras.getLong(IntentKeys.PAGE_COUNT, 0);
      if(pageCount > 0) {
        mEditPageCount.setText(Long.toString(pageCount));
      }
    }
  }

  private void bindViews() {
    mLayoutConnect = (LinearLayout) findViewById(R.id.layoutConnect);
    mEditTitle = (EditText) findViewById(R.id.editTitle);
    mEditAuthor = (EditText) findViewById(R.id.editAuthor);
    mEditPageCount = (EditText) findViewById(R.id.editPageCount);
    mTogglePagesPercent = (ToggleButton) findViewById(R.id.togglePagesPercent);
    mTogglePublicPrivate = (ToggleButton) findViewById(R.id.togglePublicPrivate);
  }

  private void bindEvents() {
    Button buttonAddBook = (Button) findViewById(R.id.buttonAddBook);
    buttonAddBook.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        onAddBookClicked();
      }
    });

    mTogglePagesPercent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
  }

  private void onAddBookClicked() {
    if(!validateFields()) {
      return;
    }

    LocalReading localReading = new LocalReading();

    localReading.title = mEditTitle.getText().toString();
    localReading.author = mEditAuthor.getText().toString();
    localReading.coverURL = mCoverURL;

    if(mTogglePagesPercent.isChecked()) {
      localReading.totalPages = Integer.parseInt(mEditPageCount.getText().toString());
      localReading.measureInPercent = false;
    } else {
      localReading.totalPages = 10000;
      localReading.measureInPercent = true;
    }

    boolean isPublic = mTogglePublicPrivate.isChecked();

    saveOrConnectLocalReading(localReading, isPublic);
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
      toast("Please enter the name of the author");
      mEditAuthor.requestFocus();
      return false;
    }

    // Validate a reasonable amount of page numbers
    if(mTogglePagesPercent.isChecked()) {
      int pageCount = 0;

      try {
        pageCount = Integer.parseInt(mEditPageCount.getText().toString());
      } catch(NumberFormatException ignored) {
      }

      if(pageCount < 1) {
        toast("Please enter a reasonable number of pages");
        mEditPageCount.requestFocus();
        return false;
      }
    }

    return true;
  }

  private void exitToReadingSession(LocalReading localReading) {
    Intent readingSessionIntent = new Intent(this, ActivityBook.class);
    readingSessionIntent.putExtra(IntentKeys.READING_ID, localReading.id);
    readingSessionIntent.putExtra(IntentKeys.START_READING_SESSION, true);
    setResult(ActivityCodes.RESULT_OK);
    startActivity(readingSessionIntent);
    finish();
  }

  /**
   * Persists a local reading to the database.
   * The reading is also connected if the user is signed in.
   *
   * @param localReading Local reading to save or connect
   * @param isPublic     Toggle for connecting as public reading if signed in
   */
  private void saveOrConnectLocalReading(LocalReading localReading, boolean isPublic) {
    if(getCurrentUser() == null) {
      getApp().showProgressDialog(this, "Saving book...");
      SaveLocalReadingTask.save(localReading, new SaveLocalReadingListener() {
        @Override public void onLocalReadingSaved(LocalReading localReading) {
          getApp().clearProgressDialog();
          exitToReadingSession(localReading);
        }
      });
    } else {
      getApp().showProgressDialog(this, "Connecting your book to Readmill...");
      ConnectReadingTask.connect(localReading, isPublic, new ConnectedReadingListener() {
        @Override public void onLocalReadingConnected(LocalReading localReading) {
          getApp().clearProgressDialog();
          if(localReading == null) {
            toastLong("Could not connect with Readmill");
          } else {
            exitToReadingSession(localReading);
          }
        }
      });
    }
  }
}
