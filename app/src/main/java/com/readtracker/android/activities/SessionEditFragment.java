package com.readtracker.android.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Session;
import com.readtracker.android.fragments.BookFragmentAdapter;
import com.readtracker.android.support.SessionPresenter;
import com.readtracker.android.support.SessionPresenter.PositionPresenter;
import com.readtracker.android.support.StringUtils;
import com.readtracker.android.support.Utils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SessionEditFragment extends DialogFragment {
  private static final String TAG = SessionEditFragment.class.getName();
  private Float mPageCount;

  public interface OnSessionEditListener {
    public void onSessionUpdated(Session session);

    public void onSessionDeleted(long sessionId);
  }

  @InjectView(R.id.startPosText) TextView mStartPosText;
  @InjectView(R.id.startPosEdit) EditText mStartPosEdit;
  @InjectView(R.id.endPosText) TextView mEndPosText;
  @InjectView(R.id.endPosEdit) EditText mEndPosEdit;
  @InjectView(R.id.setDateButton) Button mSetDateButton;
  @InjectView(R.id.hoursTextEdit) EditText mHoursTextEdit;
  @InjectView(R.id.minutesTextEdit) EditText mMinutesTextEdit;
  @InjectView(R.id.secondsTextEdit) EditText mSecondsTextEdit;

  @InjectView(R.id.startPositionSuffix) TextView mStartPositionSuffix;
  @InjectView(R.id.endPositionSuffix) TextView mEndPositionSuffix;

  @InjectView(R.id.saveButton) Button mSaveButton;

  private Session mSession;
  private SimpleDateFormat mDateFormat;

  public static <T extends Fragment & OnSessionEditListener> SessionEditFragment
  create(T listener, int sessionId) {
    SessionEditFragment fragment = new SessionEditFragment();
    Bundle args = new Bundle();
    args.putInt("sessionId", sessionId);
    fragment.setArguments(args);
    fragment.setTargetFragment(listener, 0);

    return fragment;
  }

  private int getSessionId() {
    Bundle args = getArguments();
    if(args != null) {
      return args.getInt("sessionId");
    }
    throw new RuntimeException("Created SessionEditFragment without setting sessionId");
  }

  @Nullable @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View rootView = inflater.inflate(R.layout.session_edit_fragment, container);
    ButterKnife.inject(this, rootView);

    String dateFormat = getString(R.string.session_edit_date_format);
    //noinspection ConstantConditions
    Locale locale = Utils.getLocale(getContext());
    mDateFormat = new SimpleDateFormat(dateFormat, locale);

    mSaveButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if(mSession != null) {
          mSaveButton.setEnabled(false); // prevent double tapping
          final Session serializedSession = getSessionFromFields();
          if(serializedSession != null) {
            mSession.merge(serializedSession);
            BackgroundTasks.saveSession(SessionEditFragment.this, mSession);
          } else {
            mSaveButton.setEnabled(true);
          }
        } else {
          Log.w(TAG, "Unexpectedly was able to click save button with no session set");
        }
      }
    });

    mSetDateButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final FragmentManager fragMgr = getFragmentManager();
        if(fragMgr != null) {
          long timestampMs = (Long) mSetDateButton.getTag();
          DatePickerFragment datePickerFragment = DatePickerFragment.create(SessionEditFragment.this, timestampMs);
          datePickerFragment.show(fragMgr, "pick-session-time");
        }
      }
    });

    BackgroundTasks.loadSession(this, getSessionId());

    return rootView;
  }

  private Session getSessionFromFields() {
    try {
      final float startPos = getBoundaryAdjustedPositionOrFocus(mSession, mStartPosEdit);
      float endPos = getBoundaryAdjustedPositionOrFocus(mSession, mEndPosEdit);

      if (endPos < startPos) {
        endPos = startPos;
      }

      final long timestampMs = (long) mSetDateButton.getTag();

      final long hours = (long) getFloatValueFromFieldOrFocus(mHoursTextEdit);
      final long minutes = (long) getFloatValueFromFieldOrFocus(mMinutesTextEdit);
      final long seconds = (long) getFloatValueFromFieldOrFocus(mSecondsTextEdit);
      final long durationSeconds = seconds + minutes * 60 + hours * 60 * 60;

      Session serializedSession = new Session();
      serializedSession.setStartPosition(startPos);
      serializedSession.setEndPosition(endPos);
      serializedSession.setDurationSeconds(durationSeconds);
      serializedSession.setTimestampMs(timestampMs);

      return serializedSession;
    } catch(NumberFormatException ex) {
      Log.d(TAG, "At least one of the fields has invalid number", ex);
      return null;
    }
  }

  private float getFloatValueFromFieldOrFocus(EditText editText) throws NumberFormatException {
    String stringValue = editText.getText().toString();
    try {
      return Float.parseFloat(stringValue);
    } catch(NumberFormatException ex) {
      editText.requestFocus();
      throw ex;
    }
  }

  private float getBoundaryAdjustedPositionOrFocus(Session session, EditText field) throws NumberFormatException {
    float presentedPosition = getFloatValueFromFieldOrFocus(field);

    PositionPresenter posPresenter = SessionPresenter.getPresenterForSession(session);
    final float adjustedPos = posPresenter.parse(String.valueOf(presentedPosition));
    if(adjustedPos < 0f) {
      // The number was a valid float number, but not within expected range
      String errorMessage;
      if(posPresenter.getMode() == SessionPresenter.PositionMode.PAGES) {
        final int maxPage = (int) ((SessionPresenter.PagePresenter) posPresenter).getMaxBoundary();
        errorMessage = getString(R.string.session_edit_enter_value_between_pages, maxPage);
      } else {
        errorMessage = getString(R.string.session_edit_enter_value_between_percent);
      }
      field.requestFocus();
      Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
      // NOTE(christoffer) Using a number format exception here is a bit lazy, but meh...
      throw new NumberFormatException(String.format("%f isn't within acceptable range", presentedPosition));
    }

    return adjustedPos;
  }

  private void onSessionLoaded(Session session) {
    Log.d(TAG, String.format("Loaded Session: %s", session.getBook().getTitle()));
    mSession = session;

    final PositionPresenter presenter = SessionPresenter.getPresenterForSession(session);

    if(presenter.getMode() == SessionPresenter.PositionMode.PAGES) {
      mStartPosText.setText(R.string.session_edit_started_on_page);
      mStartPosEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
      mEndPosText.setText(R.string.session_edit_ended_on_page);
      mEndPosEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

      mStartPositionSuffix.setVisibility(View.GONE);
      mEndPositionSuffix.setVisibility(View.GONE);
    } else {
      mStartPosText.setText(R.string.session_edit_started_at);
      mStartPosEdit.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
      mEndPosText.setText(R.string.session_edit_ended_at);
      mEndPosEdit.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

      mStartPositionSuffix.setVisibility(View.VISIBLE);
      mEndPositionSuffix.setVisibility(View.VISIBLE);
    }

    mStartPosEdit.setText(presenter.format(session.getStartPosition()));
    mEndPosEdit.setText(presenter.format(session.getEndPosition()));

    final long timestampMs = session.getTimestampMs();
    onSessionTimestampSet(timestampMs);

    final long durationSeconds = session.getDurationSeconds();
    int[] hms = StringUtils.convertMillisecondsToHMS(durationSeconds * 1000L);
    mHoursTextEdit.setText(String.format("%d", hms[0]));
    mMinutesTextEdit.setText(String.format("%d", hms[1]));
    mSecondsTextEdit.setText(String.format("%d", hms[2]));

    mSaveButton.setEnabled(true);
  }

  private OnSessionEditListener getListener() {
    try {
      Fragment targetFragment = getTargetFragment();
      if(targetFragment == null) {
        Log.e(TAG, "Target fragment was never set for SessionEditFragment");
        return null;
      }
      return ((OnSessionEditListener) targetFragment);
    } catch(ClassCastException ex) {
      Log.e(TAG, "Target fragment doesn't implement listener", ex);
    }
    return null;
  }

  private void onSessionDeleted(long sessionId) {
  }

  private void onSessionSaved(Session session) {
    OnSessionEditListener listener = getListener();
    if(listener != null) {
      listener.onSessionUpdated(session);
    }
    Toast.makeText(getContext(), R.string.session_edit_saved, Toast.LENGTH_SHORT).show();
    dismiss();
  }

  public void onSessionUpdateFailed() {
  }

  private static class BackgroundTasks extends AsyncTask<Void, Void, Boolean> {
    final WeakReference<SessionEditFragment> mActivityRef;
    final DatabaseManager mDatabaseMgr;
    private final Integer mSessionId;
    Session mSession;
    private final Action mAction;

    public enum Action {
      SAVE, DELETE, LOAD
    }

    private BackgroundTasks(SessionEditFragment listener, Session session, Integer sessionId, Action action) {
      mSession = session;
      mSessionId = sessionId;
      mActivityRef = new WeakReference<>(listener);
      //noinspection ConstantConditions
      mDatabaseMgr = ((BaseActivity) listener.getActivity()).getApp().getDatabaseManager();
      mAction = action;
    }

    public static void loadSession(SessionEditFragment listener, int sessionId) {
      new BackgroundTasks(listener, null, sessionId, Action.LOAD).execute();
    }

    public static void saveSession(SessionEditFragment listener, Session session) {
      new BackgroundTasks(listener, session, null, Action.SAVE).execute();
    }

    public static void deleteSession(SessionEditFragment listener, Session session) {
      new BackgroundTasks(listener, session, null, Action.DELETE).execute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      switch(mAction) {
        case SAVE:
          return mDatabaseMgr.save(mSession);
        case DELETE:
          return mDatabaseMgr.delete(mSession);
        case LOAD:
          Session session = mDatabaseMgr.get(Session.class, mSessionId);
          if(session == null) {
            Log.w(TAG, String.format("Failed to load session with id: %d", mSessionId));
            return false;
          }
          mSession = session;

          // Also have to load the book so that we can determine if we're dealing with page numbers
          // or relative positions.
          Book book = mDatabaseMgr.get(Book.class, mSession.getBook().getId());
          if(book == null) {
            Log.w(TAG, "Failed to load book for session");
          }
          mSession.setBook(book);

          return true;
      }
      Log.w(TAG, String.format("Failed to handle unknown action: %s", mAction));
      return false;
    }

    @Override protected void onPostExecute(Boolean success) {
      Log.d(TAG, String.format("Completed BackgroundTasks for action: %s with result: %s", mAction, success));
      SessionEditFragment activity = mActivityRef.get();
      if(activity != null) {
        if(success) {
          switch(mAction) {
            case SAVE:
              activity.onSessionSaved(mSession);
              break;
            case DELETE:
              activity.onSessionDeleted(mSession.getId());
              break;
            case LOAD:
              activity.onSessionLoaded(mSession);
              break;
          }
        } else {
          activity.onSessionUpdateFailed();
        }
      }
    }
  }

  private void onSessionTimestampSet(long timestampMs) {
    Log.d(TAG, String.format("onSessionTimestampSet(%d)", timestampMs));
    final Date sessionDate = new Date(timestampMs);
    String formattedDate = mDateFormat.format(sessionDate);
    mSetDateButton.setText(formattedDate);
    mSetDateButton.setTag(timestampMs);
    mSetDateButton.setEnabled(true);
  }

  public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private static final String ARG_TIMESTAMP = "arg_timestamp";
    private Calendar mCalendar = Calendar.getInstance();

    public static DatePickerFragment create(SessionEditFragment targetFragment, long timestampMs) {
      Bundle args = new Bundle();
      args.putLong(ARG_TIMESTAMP, timestampMs);
      DatePickerFragment frag = new DatePickerFragment();
      frag.setArguments(args);
      frag.setTargetFragment(targetFragment, 0 /* request code not used */);
      return frag;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      long timestampMs = getArguments().getLong(ARG_TIMESTAMP);
      mCalendar.setTimeInMillis(timestampMs);

      return new DatePickerDialog(getActivity(), this,
          mCalendar.get(Calendar.YEAR),
          mCalendar.get(Calendar.MONTH),
          mCalendar.get(Calendar.DAY_OF_MONTH)
      );
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
      SessionEditFragment callerFragment = (SessionEditFragment) getTargetFragment();
      if(callerFragment != null) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        long timestampMs = mCalendar.getTimeInMillis();
        callerFragment.onSessionTimestampSet(timestampMs);
      } else {
        Log.d(TAG, "callerFragment is null");
      }
    }
  }

}
