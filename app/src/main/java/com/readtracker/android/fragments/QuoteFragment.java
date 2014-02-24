package com.readtracker.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
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

import com.readtracker.android.R;
import com.readtracker.android.activities.AddQuoteActivity;
import com.readtracker.android.activities.BaseActivity;
import com.readtracker.android.activities.BookActivity;
import com.readtracker.android.activities.BookBaseActivity;
import com.readtracker.android.adapters.QuoteAdapter;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.List;

public class QuoteFragment extends BaseFragment {
  private static final String TAG = QuoteFragment.class.getName();

  private static final int REQ_ADD_QUOTE = 1;

  private ListView mQuoteList;
  private TextView mTextBlankState;
  private Button mAddQuoteButton;

  private View mRootView;

  private Book mBook;
  private List<Quote> mQuotes;

  private QuoteAdapter mQuoteAdapter;

  private static final int MENU_DELETE_HIGHLIGHT = 1;

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new QuoteFragment instance");
    return new QuoteFragment();
  }

  @Subscribe public void onBookLoadedEvent(BookBaseActivity.BookLoadedEvent event) {
    mBook = event.getBook();
    populateFieldsDeferred();
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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.quote_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated()");

    bindViews(view);

    mAddQuoteButton.setEnabled(false);
    populateFieldsDeferred();
  }

  private void populateFieldsDeferred() {
    if(mBook == null || mRootView == null) {
      return;
    }

    final int color = Utils.calculateBookColor(mBook);
    mAddQuoteButton.setBackgroundDrawable(DrawableGenerator.generateButtonBackground(color));

    ColorDrawable divider = new ColorDrawable(color);
    divider.setAlpha(128);
    mQuoteList.setDivider(divider);
    mQuoteList.setDividerHeight(1);

    mQuoteAdapter = new QuoteAdapter(getActivity(), R.layout.highlight_list_item, mBook.getQuotes());
    mQuoteAdapter.setColor(color);
    mQuoteList.setAdapter(mQuoteAdapter);
    mQuoteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        Quote quote = mQuoteAdapter.getItem(position);
        ((BookActivity) getActivity()).exitToAddQuoteScreen(quote);
      }
    });

    mAddQuoteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getActivity(), AddQuoteActivity.class);
        intent.putExtra(AddQuoteActivity.KEY_BOOK_ID, mBook.getId());
        startActivityForResult(intent, REQ_ADD_QUOTE);
      }
    });

    registerForContextMenu(mQuoteList);

    mAddQuoteButton.setEnabled(true);

    refreshHighlightBlankState();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "onActivityCreated()");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == REQ_ADD_QUOTE && resultCode == Activity.RESULT_OK) {
      Log.d(TAG, "Quote created");
      if(data != null && data.hasExtra(AddQuoteActivity.KEY_QUOTE_ID)) {
        loadQuote(data.getExtras().getInt(AddQuoteActivity.KEY_QUOTE_ID));
      }
    } else {
      Log.d(TAG, "Quote not created");
    }
  }

  private void loadQuote(int quoteId) {
    new LoadQuoteTask(quoteId, this).execute();
  }

  private void onQuoteLoaded(Quote quote) {
    mQuoteAdapter.add(quote);
    mQuoteAdapter.notifyDataSetChanged();
  }

  @Override public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, view, menuInfo);
    if(view.getId() == mQuoteList.getId()) {
      final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      Quote quote = mQuoteAdapter.getItem(info.position);
      menu.setHeaderTitle(getActivity().getString(R.string.quote_fragment_item_header, quote.getId()));
      final String itemText = getActivity().getString(R.string.quote_fragment_delete_quote);
      MenuItem item = menu.add(Menu.NONE, MENU_DELETE_HIGHLIGHT, Menu.NONE, itemText);
      item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem menuItem) {
          Quote quote = mQuoteAdapter.getItem(info.position);
          deleteQuote(quote);
          return true;
        }
      });
    }
  }

  private void bindViews(View view) {
    mTextBlankState = (TextView) view.findViewById(R.id.blank_text);
    mQuoteList = (ListView) view.findViewById(R.id.quote_list);
    mAddQuoteButton = (Button) view.findViewById(R.id.add_quote_button);

    mRootView = view;
  }

  /**
   * Check the number of highlights and either show or hide the blank state.
   *
   * TODO this fragment should be a ListFragment which handles this automatically.
   */
  private void refreshHighlightBlankState() {
    if(mQuoteAdapter.getCount() == 0) {
      mTextBlankState.setVisibility(View.VISIBLE);
      mQuoteList.setVisibility(View.GONE);
    } else {
      mTextBlankState.setVisibility(View.GONE);
      mQuoteList.setVisibility(View.VISIBLE);
    }
  }

  private void deleteQuote(Quote quote) {
    new DeleteTask(quote, this).execute();
  }

  private void onQuoteDeleted(Quote quote) {
    if(quote == null) {
      Toast.makeText(getActivity(), R.string.quote_error_failed_to_delete, Toast.LENGTH_SHORT).show();
    } else {
      mQuoteAdapter.remove(quote);
      refreshHighlightBlankState();
    }
  }

  private static class DeleteTask extends AsyncTask<Void, Void, Quote> {
    private final Quote mQuote;
    private final WeakReference<QuoteFragment> mFragment;
    private final DatabaseManager mDatabaseManager;

    public DeleteTask(Quote quote, QuoteFragment fragment) {
      mQuote = quote;
      mFragment = new WeakReference<QuoteFragment>(fragment);
      // TODO this is whack, should probably move to some sort of dependency solution
      mDatabaseManager = ((BaseActivity)fragment.getActivity()).getApp().getDatabaseManager();
    }

    @Override
    protected Quote doInBackground(Void... voids) {
      if(mDatabaseManager.delete(mQuote)) {
        return mQuote;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(Quote deletedQuote) {
      QuoteFragment fragment = mFragment.get();
      if(fragment != null) {
        fragment.onQuoteDeleted(deletedQuote);
      }
    }
  }

  private static class LoadQuoteTask extends AsyncTask<Void, Void, Quote> {
    private final WeakReference<QuoteFragment> mFragment;
    private final DatabaseManager mDatabaseManager;
    private final int mQuoteId;

    public LoadQuoteTask(int quoteId, QuoteFragment fragment) {
      mQuoteId = quoteId;
      mFragment = new WeakReference<QuoteFragment>(fragment);
      mDatabaseManager = ((BaseActivity)fragment.getActivity()).getApp().getDatabaseManager();
    }

    @Override
    protected Quote doInBackground(Void... voids) {
      return mDatabaseManager.get(Quote.class, mQuoteId);
    }

    @Override
    protected void onPostExecute(Quote quote) {
      QuoteFragment fragment = mFragment.get();
      if(fragment != null) {
        fragment.onQuoteLoaded(quote);
      }
    }
  }
}
