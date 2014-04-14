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
import com.readtracker.android.activities.BookBaseActivity;
import com.readtracker.android.adapters.QuoteAdapter;
import com.readtracker.android.db.Book;
import com.readtracker.android.db.DatabaseManager;
import com.readtracker.android.db.Quote;
import com.readtracker.android.support.DrawableGenerator;
import com.readtracker.android.support.Utils;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class QuotesFragment extends BaseFragment {
  private static final String TAG = QuotesFragment.class.getName();

  private static final int REQ_ADD_QUOTE = 1;

  private ListView mQuoteList;
  private TextView mTextBlankState;
  private Button mAddQuoteButton;

  private View mRootView;

  private Book mBook;
  private QuoteAdapter mQuoteAdapter;

  private static final int MENU_DELETE_QUOTE = 1;

  private Quote mPendingNewQuote;

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new QuotesFragment instance");
    return new QuotesFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mQuoteAdapter = new QuoteAdapter(getActivity(), R.layout.quote_list_item, new ArrayList<Quote>());
  }

  // Called when the parent activity has completed loading the book
  @Subscribe
  public void onBookLoadedEvent(BookBaseActivity.BookLoadedEvent event) {
    mBook = event.getBook();

    if(mBook != null) {
      List<Quote> quotesInBook = mBook.getQuotes();

      // If we are running this after the activity result, then
      if(mPendingNewQuote != null && quotesInBook.indexOf(mPendingNewQuote) < 0) {
        Log.d(TAG, "Adding non-existing pending quote");
        quotesInBook.add(mPendingNewQuote);
        mPendingNewQuote = null;
      }
    }

    populateFieldsDeferred();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.quotes_fragment, container, false);
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

    mQuoteAdapter.setColor(color);
    mQuoteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long itemId) {
        Quote quote = mQuoteAdapter.getItem(position);
        Log.v(TAG, "Clicked quote: " + quote);
      }
    });

    mQuoteAdapter.clear();
    for(Quote quote : mBook.getQuotes()) {
      mQuoteAdapter.add(quote);
    }
    mQuoteAdapter.sortQuotes();
    mQuoteList.setAdapter(mQuoteAdapter);

    mAddQuoteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getActivity(), AddQuoteActivity.class);
        intent.putExtra(AddQuoteActivity.KEY_BOOK_ID, mBook.getId());
        startActivityForResult(intent, REQ_ADD_QUOTE);
      }
    });
    mAddQuoteButton.setEnabled(true);

    registerForContextMenu(mQuoteList);

    refreshBlankState();
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
      if(data != null && data.hasExtra(AddQuoteActivity.KEY_QUOTE_ID)) {
        final int quoteId = data.getIntExtra(AddQuoteActivity.KEY_QUOTE_ID, 0);
        Log.d(TAG, "Quote created: " + quoteId);
        addQuoteToBookList(quoteId);
      }
    } else {
      Log.d(TAG, "Quote not created");
    }
  }

  private void addQuoteToBookList(int quoteId) {
    // Load the quote on the main thread since it's single row, and it quickly
    // gets very complicated to sync this new quote with the book loading event
    Quote quote = getDatabaseManager().get(Quote.class, quoteId);
    if(quote == null) {
      Log.w(TAG, "No such quote: " + quoteId);
      return;
    }

    if(mBook != null) {
      List<Quote> quotesInLoadedBook = mBook.getQuotes();
      if(quotesInLoadedBook.indexOf(quote) < 0) {
        Log.d(TAG, "Book does not contain new quote");
        quotesInLoadedBook.add(quote);
      } else {
        Log.d(TAG, "Book already contains quote");
      }
    } else {
      Log.d(TAG, "Setting pending quote: " + quote);
      mPendingNewQuote = quote;
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, view, menuInfo);

    if(view.getId() != mQuoteList.getId()) {
      return;
    }

    final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    Quote quote = mQuoteAdapter.getItem(info.position);

    setMenuTitle(menu, quote);

    final String itemText = getActivity().getString(R.string.quote_fragment_delete_quote);
    MenuItem item = menu.add(Menu.NONE, MENU_DELETE_QUOTE, Menu.NONE, itemText);
    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem menuItem) {
        Quote quote = mQuoteAdapter.getItem(info.position);
        deleteQuote(quote);
        return true;
      }
    });
  }

  private void setMenuTitle(ContextMenu menu, Quote quote) {
    final String defaultTitle = getActivity().getString(R.string.quote_fragment_menu_title_default);
    final String title = Utils.truncateString(quote.getContent(), 20, defaultTitle);
    menu.setHeaderTitle(title);
  }

  private void bindViews(View view) {
    mTextBlankState = (TextView) view.findViewById(R.id.blank_text);
    mQuoteList = (ListView) view.findViewById(R.id.quote_list);
    mAddQuoteButton = (Button) view.findViewById(R.id.add_quote_button);

    mRootView = view;
  }

  private void refreshBlankState() {
    final boolean hasItems = mQuoteAdapter.getCount() > 0;
    mQuoteList.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    mTextBlankState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
  }

  private void deleteQuote(Quote quote) {
    new DeleteTask(quote, this).execute();
  }

  private void onQuoteDeleted(Quote quote) {
    if(quote == null) {
      Toast.makeText(getActivity(), R.string.quote_error_failed_to_delete, Toast.LENGTH_SHORT).show();
    } else {
      mQuoteAdapter.remove(quote);
      mBook.getQuotes().remove(quote);
      refreshBlankState();
    }
  }

  private static class DeleteTask extends AsyncTask<Void, Void, Quote> {
    private final Quote mQuote;
    private final WeakReference<QuotesFragment> mFragment;
    private final DatabaseManager mDatabaseManager;

    public DeleteTask(Quote quote, QuotesFragment fragment) {
      mQuote = quote;
      mFragment = new WeakReference<QuotesFragment>(fragment);
      mDatabaseManager = fragment.getDatabaseManager();
    }

    @Override
    protected Quote doInBackground(Void... voids) {
      return mDatabaseManager.delete(mQuote) ? mQuote : null;
    }

    @Override
    protected void onPostExecute(Quote deletedQuote) {
      QuotesFragment fragment = mFragment.get();
      if(fragment != null) {
        fragment.onQuoteDeleted(deletedQuote);
      }
    }
  }
}
