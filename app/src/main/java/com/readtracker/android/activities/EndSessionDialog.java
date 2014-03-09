package com.readtracker.android.activities;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.readtracker.android.R;
import com.readtracker.android.custom_views.ProgressPicker;
import com.readtracker.android.db.Book;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;
import com.readtracker.android.thirdparty.SafeViewFlipper;

/** Screen for input the ending page of a reading session */
public class EndSessionDialog extends DialogFragment implements View.OnClickListener, ProgressPicker.OnPositionChangeListener {
  private static final String TAG = EndSessionDialog.class.getName();

  // Page index for the view flipper
  private static final int SAVE_FLIPPER_PAGE = 0;
  private static final int FINISH_FLIPPER_PAGE = 1;

  public static final String ARG_CURRENT_POSITION = "CURRENT_POSITION";
  public static final String ARG_PAGE_COUNT = "PAGE_COUNT";
  private static final String ARG_COLOR = "COLOR";

  private Button mButtonSaveProgress;
  private Button mButtonFinishBook;
  private ProgressPicker mProgressPicker;
  private SafeViewFlipper mFlipperActionButtons;

  private Float mSelectedPosition;
  private Float mPageCount;
  private int mColor;

  // owning activity must implement EndSessionDialogListener
  private EndSessionDialogListener mListener;

  public static EndSessionDialog newInstance(Book book) {
    assert(book != null);
    EndSessionDialog dialog = new EndSessionDialog();
    Bundle arguments = new Bundle();

    int color = Utils.calculateBookColor(book);
    Float currentPosition = book.getCurrentPosition();
    Float pageCount = book.getPageCount();

    arguments.putInt(ARG_COLOR, color);
    arguments.putFloat(EndSessionDialog.ARG_CURRENT_POSITION, currentPosition == null ? 0f : currentPosition);
    if(pageCount != null) {
      arguments.putFloat(EndSessionDialog.ARG_PAGE_COUNT, pageCount);
    }
    dialog.setArguments(arguments);
    return dialog;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setStyle(STYLE_NO_TITLE, R.style.ReadTrackerDialog);

    mColor = getArguments().getInt(ARG_COLOR);

    if(savedInstanceState == null) {
      mSelectedPosition = getArguments().getFloat(ARG_CURRENT_POSITION);
    } else {
      mSelectedPosition = savedInstanceState.getFloat(ARG_CURRENT_POSITION);
    }

    if(getArguments().containsKey(ARG_PAGE_COUNT)) {
      mPageCount = getArguments().getFloat(ARG_PAGE_COUNT);
    } else {
      mPageCount = null;
    }

    mListener = (EndSessionDialogListener) getActivity();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(TAG, "onCreateView()");
    View root = inflater.inflate(R.layout.end_session_dialog, null);

    bindViews(root);

    mProgressPicker.setPositionAndPageCount(mSelectedPosition, mPageCount);
    mProgressPicker.setOnPositionChangeListener(this);

    toggleFinishButton(mSelectedPosition == 1f);

    mButtonSaveProgress.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mColor));
    mButtonFinishBook.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mColor));

    mButtonSaveProgress.setOnClickListener(this);
    mButtonFinishBook.setOnClickListener(this);

    return root;
  }

  private void bindViews(View root) {
    mButtonSaveProgress = (Button) root.findViewById(R.id.save_button);
    mButtonFinishBook = (Button) root.findViewById(R.id.finish_button);
    mFlipperActionButtons = (SafeViewFlipper) root.findViewById(R.id.action_button_flipper);
    mProgressPicker = (ProgressPicker) root.findViewById(R.id.progress_picker);
  }

  @Override
  public void onClick(View view) {
    if(view == mButtonSaveProgress) {
      mListener.onSessionEndWithNewPosition(mProgressPicker.getPosition());
      dismissAllowingStateLoss();
    } else if(view == mButtonFinishBook) {
      mListener.onSessionEndWithFinish();
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

  /** Interface for listening to dialog results. */
  public static interface EndSessionDialogListener {
    /** Called when the user confirms a new position. */
    void onSessionEndWithNewPosition(float position);

    /** Called when the user wants to finish the book. */
    void onSessionEndWithFinish();
  }
}
