package com.readtracker.android.activities;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.widget.AppCompatButton;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.readtracker.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.ColorUtils;
import com.readtracker.android.thirdparty.SafeViewFlipper;
import com.readtracker.databinding.EndSessionDialogBinding;

import org.jetbrains.annotations.NotNull;

/** Screen for input the ending page of a reading session */
public class EndSessionDialog extends DialogFragment implements View.OnClickListener, ProgressPicker.OnPositionChangeListener {
  private static final String TAG = EndSessionDialog.class.getName();

  // Page index for the view flipper
  private static final int SAVE_FLIPPER_PAGE = 0;
  private static final int FINISH_FLIPPER_PAGE = 1;

  public static final String ARG_CURRENT_POSITION = "CURRENT_POSITION";
  public static final String ARG_PAGE_COUNT = "PAGE_COUNT";
  private static final String ARG_COLOR = "COLOR";

  /** Views */
  private AppCompatButton mButtonSaveProgress;
  private AppCompatButton mButtonFinishBook;
  private ProgressPicker mProgressPicker;
  private SafeViewFlipper mFlipperActionButtons;

  private Float mSelectedPosition;
  private Float mPageCount;
  private int mColor;

  private EndSessionDialogBinding binding;

  public static EndSessionDialog newInstance(Book book) {
    assert (book != null);
    EndSessionDialog dialog = new EndSessionDialog();
    Bundle arguments = new Bundle();

    int color = ColorUtils.getColorForBook(book);
    float currentPosition = book.getCurrentPosition();
    Float pageCount = book.getPageCount();

    arguments.putInt(ARG_COLOR, color);
    arguments.putFloat(EndSessionDialog.ARG_CURRENT_POSITION, currentPosition);
    if(pageCount != null) {
      arguments.putFloat(EndSessionDialog.ARG_PAGE_COUNT, pageCount);
    }
    dialog.setArguments(arguments);
    return dialog;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Bundle arguments = getArguments();
    if(arguments == null) {
      Log.d(TAG, "Got null arguments");
      return;
    }

    setStyle(STYLE_NO_TITLE, R.style.ReadTrackerDialogTheme);

    mColor = arguments.getInt(ARG_COLOR);

    if(savedInstanceState == null) {
      mSelectedPosition = arguments.getFloat(ARG_CURRENT_POSITION);
    } else {
      mSelectedPosition = savedInstanceState.getFloat(ARG_CURRENT_POSITION);
    }

    if(arguments.containsKey(ARG_PAGE_COUNT)) {
      mPageCount = arguments.getFloat(ARG_PAGE_COUNT);
    } else {
      mPageCount = null;
    }
  }

  @Override
  public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(TAG, "onCreateView()");
    binding = EndSessionDialogBinding.inflate(inflater, container, false);

    mButtonSaveProgress = binding.saveButton;
    mButtonFinishBook = binding.finishButton;
    mProgressPicker = binding.progressPicker;
    mFlipperActionButtons = binding.actionButtonFlipper;

    mProgressPicker.setPositionAndPageCount(mSelectedPosition, mPageCount);
    mProgressPicker.setOnPositionChangeListener(this);
    mProgressPicker.setColor(mColor);

    toggleFinishButton(mSelectedPosition == 1f);

    ColorUtils.applyButtonColor(mColor, mButtonSaveProgress);

    mButtonSaveProgress.setOnClickListener(this);
    mButtonFinishBook.setOnClickListener(this);

    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onClick(View view) {
    final EndSessionDialogListener listener = (EndSessionDialogListener) getActivity();
    if(listener == null) {
      return;
    }

    if(view == mButtonSaveProgress) {
      listener.onConfirmedSessionEndPosition(mProgressPicker.getPosition());
      dismissAllowingStateLoss();
    } else if(view == mButtonFinishBook) {
      listener.onConfirmedSessionEndPosition(1f);
      dismissAllowingStateLoss();
    }
  }

  @Override
  public void onChangeProgress(int newPage) {
    toggleFinishButton(mProgressPicker.isOnLastPosition());
  }

  private void toggleFinishButton(boolean finishMode) {
    if(finishMode && mFlipperActionButtons.getDisplayedChild() != FINISH_FLIPPER_PAGE) {
      mFlipperActionButtons.setDisplayedChild(FINISH_FLIPPER_PAGE);
    } else if(!finishMode && mFlipperActionButtons.getDisplayedChild() != SAVE_FLIPPER_PAGE) {
      mFlipperActionButtons.setDisplayedChild(SAVE_FLIPPER_PAGE);
    }
  }

  /** Defines an interface for activities that host the EndSessionDialog in order to receive results. */
  public interface EndSessionDialogListener {
    void onConfirmedSessionEndPosition(float position);
  }
}
