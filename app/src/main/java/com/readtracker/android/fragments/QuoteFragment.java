package com.readtracker.android.fragments;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.android.IntentKeys;
import com.readtracker.android.R;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.adapters.HighlightItem;
import com.readtracker.android.adapters.QuoteAdapter;
import com.readtracker.android.db.LocalHighlight;
import com.readtracker.android.db.LocalReading;
import com.readtracker.android.interfaces.DeleteLocalHighlightListener;
import com.readtracker.android.interfaces.PersistLocalHighlightListener;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.tasks.DeleteLocalHighlightTask;
import com.readtracker.android.tasks.PersistLocalHighlightTask;

import java.util.ArrayList;
import java.util.List;

public class QuoteFragment extends Fragment {
  private static final String TAG = QuoteFragment.class.getName();

  private static ListView mListHighlights;
  private static TextView mTextBlankState;
  private static Button mButtonAddHighlight;

  private LocalReading mLocalReading;
  private ArrayList<LocalHighlight> mLocalHighlights;
  private QuoteAdapter mQuoteAdapter;

  private boolean mForceReinitialize = false;

  private static final int MENU_DELETE_HIGHLIGHT = 1;

  public static Fragment newInstance(LocalReading localReading, ArrayList<LocalHighlight> localHighlights) {
    Log.d(TAG, "newInstance() called with " + localHighlights.size() + " highlights ");
    QuoteFragment instance = new QuoteFragment();
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
    return inflater.inflate(R.layout.quote_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated()");
    bindViews(view);

    mButtonAddHighlight.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        ((BookActivity) getActivity()).exitToAddQuoteScreen(null);
      }
    });

    mButtonAddHighlight.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(mLocalReading.getColor()));

    List<HighlightItem> highlightItems = itemize(mLocalHighlights);

    final int color = mLocalReading.getColor();
    ColorDrawable divider = new ColorDrawable(color);
    divider.setAlpha(128);
    mListHighlights.setDivider(divider);
    mListHighlights.setDividerHeight(1);

    mQuoteAdapter = new QuoteAdapter(getActivity(), R.layout.highlight_list_item, highlightItems);
    mQuoteAdapter.setColor(mLocalReading.getColor());
    mListHighlights.setAdapter(mQuoteAdapter);
    mListHighlights.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        HighlightItem highlightItem = mQuoteAdapter.getItem(position);
        LocalHighlight clickedHighlight = highlightItem.getLocalHighlight();
        ((BookActivity) getActivity()).exitToAddQuoteScreen(clickedHighlight);
      }
    });
    registerForContextMenu(mListHighlights);

    refreshHighlightBlankState();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "onActivityCreated()");
  }

  @Override public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, view, menuInfo);
    if(view.getId() == mListHighlights.getId()) {
      final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      menu.setHeaderTitle(getActivity().getString(R.string.quote_fragment_item_header, info.position));
      final String itemText = getActivity().getString(R.string.quote_fragment_delete_quote);
      MenuItem item = menu.add(Menu.NONE, MENU_DELETE_HIGHLIGHT, Menu.NONE, itemText);
      item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem menuItem) {
          LocalHighlight localHighlight = mQuoteAdapter.getItem(info.position).getLocalHighlight();
          deleteHighlight(localHighlight);
          return true;
        }
      });
    }
  }

  private void bindViews(View view) {
    mTextBlankState = (TextView) view.findViewById(R.id.blank_text);
    mListHighlights = (ListView) view.findViewById(R.id.quotes_list);
    mButtonAddHighlight = (Button) view.findViewById(R.id.add_quote_button);
  }

  private List<HighlightItem> itemize(List<LocalHighlight> localHighlights) {
    if(localHighlights == null) {
      return new ArrayList<HighlightItem>();
    }

    Log.d(TAG, "Itemizing " + localHighlights.size() + " reading highlights");

    ArrayList<HighlightItem> items = new ArrayList<HighlightItem>(localHighlights.size());
    for(LocalHighlight localHighlight : localHighlights) {
      if(!localHighlight.deletedByUser) {
        items.add(new HighlightItem(localHighlight));
      }
    }
    return items;
  }

  /**
   * Check the number of highlights and either show or hide the blank state.
   *
   * TODO this fragment should be a ListFragment which handles this automatically.
   */
  private void refreshHighlightBlankState() {
    if(mQuoteAdapter.getCount() == 0) {
      mTextBlankState.setVisibility(View.VISIBLE);
      mListHighlights.setVisibility(View.GONE);
    } else {
      mTextBlankState.setVisibility(View.GONE);
      mListHighlights.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Deletes the highlight. If the highlight is not connected to Readmill it is deleted immediately.
   * Otherwise it is flagged as deleted and removed at a later time.
   *
   * @param localHighlight LocalHighlight local highlight to delete.
   */
  private void deleteHighlight(LocalHighlight localHighlight) {
    if(localHighlight.isOfflineOnly()) {
      deleteLocalHighlight(localHighlight);
    } else {
      markHighlightAsDeleted(localHighlight);
    }
  }

  /**
   * Removes a highlight from the device and updates the highlight list
   * @param localHighlight highlight to delete
   */
  private void deleteLocalHighlight(LocalHighlight localHighlight) {
    Log.d(TAG, "deleteLocalHighlight()");
    DeleteLocalHighlightTask.delete(localHighlight, new DeleteLocalHighlightListener() {
      @Override public void onLocalHighlightDeleted(LocalHighlight deletedHighlight) {
        Log.d(TAG, "Deleting highlight with id: " + deletedHighlight.id);
        mQuoteAdapter.remove(deletedHighlight.id);
        refreshHighlightBlankState();
      }

      @Override public void onLocalHighlightDeletedFailed(LocalHighlight deletedHighlight) {
        Toast.makeText(getActivity(), "Failed to delete the highlight", Toast.LENGTH_SHORT).show();
      }
    });
  }

  /**
   * Marks a local highlight as deleted, causing it to be removed on next sync.
   *
   * @param localHighlight highlight to mark as deleted
   */
  private void markHighlightAsDeleted(LocalHighlight localHighlight) {
    Log.d(TAG, "markHighlightAsDeleted()");
    localHighlight.deletedByUser = true;
    PersistLocalHighlightTask.persist(localHighlight, new PersistLocalHighlightListener() {
      @Override public void onLocalHighlightPersisted(int id, boolean created) {
        mQuoteAdapter.remove(id);
        refreshHighlightBlankState();
      }

      @Override public void onLocalHighlightPersistedFailed() {
        Toast.makeText(getActivity(), "Failed to delete the Highlight", Toast.LENGTH_SHORT).show();
      }
    });
  }
}
