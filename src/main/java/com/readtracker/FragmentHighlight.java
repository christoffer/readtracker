package com.readtracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.readtracker.db.LocalHighlight;
import com.readtracker.db.LocalReading;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FragmentHighlight extends Fragment {
  private static final String TAG = FragmentHighlight.class.getName();
  private static final int CONTEXT_MENU_DELETE = 0;

  private static ListView mListHighlights;
  private static TextView mTextBlankState;
  private static Button mButtonAddHighlight;

  private LocalReading mLocalReading;
  private ArrayList<LocalHighlight> mLocalHighlights;
  private ListAdapterHighlight mHighlightAdapter;

  private boolean mForceReinitialize = false;

  public static Fragment newInstance(LocalReading localReading, ArrayList<LocalHighlight> localHighlights) {
    Log.d(TAG, "newInstance() called with " + localHighlights.size() + " highlights ");
    FragmentHighlight instance = new FragmentHighlight();
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
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated");
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
      mLocalHighlights = in.getParcelableArrayList(IntentKeys.READING_HIGHLIGHTS);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    Log.d(TAG, "freezing state");
    out.putParcelableArrayList(IntentKeys.READING_HIGHLIGHTS, mLocalHighlights);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_highlights, container, false);
    bindViews(view);

    mButtonAddHighlight.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        ((ActivityBook) getActivity()).exitToCreateHighlightScreen();
      }
    });

    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "onActivityCreated()");

    List<ListItemHighlight> highlightItems = itemize(mLocalHighlights);

    if(highlightItems.size() == 0) {
      mTextBlankState.setVisibility(View.VISIBLE);
      mListHighlights.setVisibility(View.GONE);
    } else {
      final int color = mLocalReading.getColor();
      mListHighlights.setDivider(new ColorDrawable(color));
      mListHighlights.setDividerHeight(1);
    }

    mHighlightAdapter = new ListAdapterHighlight(getActivity(), R.layout.highlight_list_item, highlightItems);
    mListHighlights.setAdapter(mHighlightAdapter);
    mListHighlights.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        ListItemHighlight highlightItem = mHighlightAdapter.getItem(position);
        if(highlightItem.getPermalink() != null) {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, highlightItem.getPermalink());
          startActivity(browserIntent);
        }
      }
    });

    registerForContextMenu(mListHighlights);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
    ListItemHighlight readingItem = mHighlightAdapter.getItem(position);

    MenuItem menuItem = menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Remove from device");
    menuItem.setIcon(android.R.drawable.ic_menu_delete);

    String title = readingItem.getContent();
    if(title.length() > 50) {
      title = title.substring(0, 50) + "...";
    }
    menu.setHeaderTitle(title);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    ListItemHighlight readingItem = mHighlightAdapter.getItem(info.position);
    removeFromDevice(readingItem);
    return true;
  }

  private void removeFromDevice(ListItemHighlight item) {
    (new DeleteHighlightFromDeviceTask()).execute(item);
  }

  private void bindViews(View view) {
    mTextBlankState = (TextView) view.findViewById(R.id.textBlankState);
    mListHighlights = (ListView) view.findViewById(R.id.listHighlights);
    mButtonAddHighlight = (Button) view.findViewById(R.id.buttonAddHighlight);
  }

  private List<ListItemHighlight> itemize(List<LocalHighlight> localHighlights) {
    if(localHighlights == null) {
      return new ArrayList<ListItemHighlight>();
    }

    Log.d(TAG, "Itemizing " + localHighlights.size() + " reading highlights");

    ArrayList<ListItemHighlight> items = new ArrayList<ListItemHighlight>(localHighlights.size());
    for(LocalHighlight localHighlight : localHighlights) {
      items.add(new ListItemHighlight(localHighlight));
    }
    return items;
  }

  private void onItemRemoved(ListItemHighlight deletedItem) {
    mHighlightAdapter.remove(deletedItem);
  }

  private class DeleteHighlightFromDeviceTask extends AsyncTask<ListItemHighlight, Void, ListItemHighlight> {

    @Override
    protected ListItemHighlight doInBackground(ListItemHighlight... items) {
      try {
        List<Integer> ids = new ArrayList<Integer>(1);
        ids.add(items[0].getId());
        ApplicationReadTracker.getHighlightDao().deleteIds(ids);
        return items[0];
      } catch(SQLException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPostExecute(ListItemHighlight deletedItem) {
      onItemRemoved(deletedItem);
    }
  }
}
