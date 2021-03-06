package com.readtracker.android.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.SessionPresenter;
import com.readtracker.android.support.SessionPresenter.PositionPresenter;
import com.readtracker.android.support.StringUtils;
import com.readtracker.android.support.Utils;
import com.readtracker.databinding.SessionEditFragmentBinding;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class SessionEditFragment extends DialogFragment {
  private static final String TAG = SessionEditFragment.class.getName();

  public interface OnSessionEditListener {
    void onSessionUpdated(Session session);

    void onSessionDeleted(long sessionId);
  }

  private SessionEditFragmentBinding binding;

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

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    binding = SessionEditFragmentBinding.inflate(inflater, container, false);

    String dateFormat = getString(R.string.session_edit_date_format);
    Locale locale = Utils.getLocale(getContext());
    mDateFormat = new SimpleDateFormat(dateFormat, locale);

    binding.saveButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if(mSession != null) {
          binding.saveButton.setEnabled(false); // prevent double tapping
          final Session serializedSession = getSessionFromFields();
          if(serializedSession != null) {
            mSession.merge(serializedSession);
            BackgroundTasks.saveSession(SessionEditFragment.this, mSession);
          } else {
            binding.saveButton.setEnabled(true);
          }
        } else {
          Log.w(TAG, "Unexpectedly was able to click save button with no session set");
        }
      }
    });

    binding.deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Toast.makeText(getContext(), R.string.session_edit_long_press_to_delete, Toast.LENGTH_LONG).show();
      }
    });

    binding.deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
      @Override public boolean onLongClick(View v) {
        BackgroundTasks.deleteSession(SessionEditFragment.this, mSession);
        return true;
      }
    });

    binding.dateEdit.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final FragmentManager fragMgr = getFragmentManager();
        if(fragMgr != null) {
          long timestampMs = (Long) binding.dateEdit.getTag();
          DatePickerFragment datePickerFragment = DatePickerFragment.create(SessionEditFragment.this, timestampMs);
          datePickerFragment.show(fragMgr, "pick-session-time");
        }
      }
    });

    // NOTE(christoffer) Kind of weird that this can't be set in XML, no?
    // binding.setDateButton.setPaintFlags(binding.setDateButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

    BackgroundTasks.loadSession(this, getSessionId());

    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private Session getSessionFromFields() {
    try {
      final float startPos = getBoundaryAdjustedPositionOrFocus(mSession, binding.startPosEdit);
      float endPos = getBoundaryAdjustedPositionOrFocus(mSession, binding.endPosEdit);

      if(endPos < startPos) {
        endPos = startPos;
      }

      final long timestampMs = (long) binding.dateEdit.getTag();

      final long hours = (long) getFloatValueFromFieldOrFocus(binding.hoursTextEdit);
      final long minutes = (long) getFloatValueFromFieldOrFocus(binding.minutesTextEdit);
      final long seconds = (long) getFloatValueFromFieldOrFocus(binding.secondsTextEdit);
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
      binding.startPosEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
      binding.endPosEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
      binding.endPositionSuffix.setVisibility(View.GONE);
    } else {
      binding.startPosEdit.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
      binding.endPosEdit.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
      binding.endPositionSuffix.setVisibility(View.VISIBLE);
    }

    binding.startPosEdit.setText(presenter.format(session.getStartPosition()));
    binding.endPosEdit.setText(presenter.format(session.getEndPosition()));

    final long timestampMs = session.getTimestampMs();
    onSessionTimestampSet(timestampMs);

    final long durationSeconds = session.getDurationSeconds();
    int[] hms = StringUtils.convertMillisecondsToHMS(durationSeconds * 1000L);
    binding.hoursTextEdit.setText(String.format("%d", hms[0]));
    binding.minutesTextEdit.setText(String.format("%d", hms[1]));
    binding.secondsTextEdit.setText(String.format("%d", hms[2]));

    binding.saveButton.setEnabled(true);
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
    OnSessionEditListener listener = getListener();
    if(listener != null) {
      listener.onSessionDeleted(sessionId);
    }
    Toast.makeText(getContext(), R.string.session_edit_deleted_session, Toast.LENGTH_SHORT).show();
    dismiss();
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
    binding.dateEdit.setText(formattedDate);
    binding.dateEdit.setTag(timestampMs);
    binding.dateEdit.setEnabled(true);
  }

  public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private static final String ARG_TIMESTAMP = "arg_timestamp";
    private final Calendar mCalendar = Calendar.getInstance();

    public static DatePickerFragment create(SessionEditFragment targetFragment, long timestampMs) {
      Bundle args = new Bundle();
      args.putLong(ARG_TIMESTAMP, timestampMs);
      DatePickerFragment frag = new DatePickerFragment();
      frag.setArguments(args);
      frag.setTargetFragment(targetFragment, 0 /* request code not used */);
      return frag;
    }

    @NotNull @Override
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
