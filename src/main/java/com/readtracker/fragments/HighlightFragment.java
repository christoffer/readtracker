package com.readtracker.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.readtracker.IntentKeys;
import com.readtracker.R;
import com.readtracker.activities.BookActivity;
import com.readtracker.adapters.HighlightAdapter;
import com.readtracker.adapters.HighlightItem;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;
import com.readtracker.support.DrawableGenerator;

import java.util.ArrayList;
import java.util.List;

public class HighlightFragment extends Fragment {
  private static final String TAG = HighlightFragment.class.getName();

  private static ListView mListHighlights;
  private static TextView mTextBlankState;
  private static Button mButtonAddHighlight;

  private LocalReading mLocalReading;
  private ArrayList<LocalHighlight> mLocalHighlights;
  private HighlightAdapter mHighlightAdapter;

  private boolean mForceReinitialize = false;

  public static Fragment newInstance(LocalReading localReading, ArrayList<LocalHighlight> localHighlights) {
    Log.d(TAG, "newInstance() called with " + localHighlights.size() + " highlights ");
    HighlightFragment instance = new HighlightFragment();
    instance.setLocalReading(localReading);
    instance.setReadingHighlights(localHighlights);
    instance.setForceInitialize(true);
    return instance;
  }

  private void setForceInitialize(boolean forceInitialize) {
    mForceReinitialize = forceInitialize;
  }

  public void setLocalReading(LocalReading localReading) {
    mLocalReading = localReading;
  }

  private void setReadingHighlights(ArrayList<LocalHighlight> localHighlights) {
    mLocalHighlights = localHighlights;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    Log.d(TAG, "onAttach()");
  }


  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
  }

  @Override
  public void onCreate(Bundle in) {
    super.onCreate(in);
    if(in != null && !mForceReinitialize) {
      Log.d(TAG, "unfreezing state");
      mLocalReading = in.getParcelable(IntentKeys.LOCAL_READING);
      mLocalHighlights = in.getParcelableArrayList(IntentKeys.READING_HIGHLIGHTS);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelable(IntentKeys.LOCAL_READING, mLocalReading);
    out.putParcelableArrayList(IntentKeys.READING_HIGHLIGHTS, mLocalHighlights);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_highlights, container, false);
    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated()");
    bindViews(view);

    mButtonAddHighlight.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        ((BookActivity) getActivity()).exitToCreateHighlightScreen();
      }
    });

    mButtonAddHighlight.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));

    List<HighlightItem> highlightItems = itemize(mLocalHighlights);

    if(highlightItems.size() == 0) {
      mTextBlankState.setVisibility(View.VISIBLE);
      mListHighlights.setVisibility(View.GONE);
    } else {
      final int color = mLocalReading.getColor();
      ColorDrawable divider = new ColorDrawable(color);
      divider.setAlpha(128);
      mListHighlights.setDivider(divider);
      mListHighlights.setDividerHeight(1);
    }

    mHighlightAdapter = new HighlightAdapter(getActivity(), R.layout.highlight_list_item, highlightItems);
    mHighlightAdapter.setColor(mLocalReading.getColor());
    mListHighlights.setAdapter(mHighlightAdapter);
    mListHighlights.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        HighlightItem highlightItem = mHighlightAdapter.getItem(position);
        if(highlightItem.getPermalink() != null) {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, highlightItem.getPermalink());
          startActivity(browserIntent);
        } else {
          Toast.makeText(getActivity(), "Once this highlight is synced you'll be taken to it's web page", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "onActivityCreated()");
  }

  private void bindViews(View view) {
    mTextBlankState = (TextView) view.findViewById(R.id.textBlankState);
    mListHighlights = (ListView) view.findViewById(R.id.listHighlights);
    mButtonAddHighlight = (Button) view.findViewById(R.id.buttonAddHighlight);
  }

  private List<HighlightItem> itemize(List<LocalHighlight> localHighlights) {
    if(localHighlights == null) {
      return new ArrayList<HighlightItem>();
    }

    Log.d(TAG, "Itemizing " + localHighlights.size() + " reading highlights");

    ArrayList<HighlightItem> items = new ArrayList<HighlightItem>(localHighlights.size());
    for(LocalHighlight localHighlight : localHighlights) {
      items.add(new HighlightItem(localHighlight));
    }
    return items;
  }
}
