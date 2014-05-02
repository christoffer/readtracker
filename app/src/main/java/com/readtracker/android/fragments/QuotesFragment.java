package com.readtracker.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.activities.AddBookActivity;
import com.readtracker.android.activities.AddQuoteActivity;
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
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class QuotesFragment extends BaseFragment {
  private static final String TAG = QuotesFragment.class.getName();

  private static final int REQ_ADD_QUOTE = 1;
  private static final int REQ_EDIT_QUOTE = 2;

  @InjectView(R.id.quote_list) ListView mQuoteList;
  @InjectView(R.id.blank_text) TextView mTextBlankState;
  @InjectView(R.id.add_quote_button) Button mAddQuoteButton;

  private Book mBook;
  private QuoteAdapter mQuoteAdapter;

  private Quote mPendingNewQuote;

  public static Fragment newInstance() {
    Log.v(TAG, "Creating new QuotesFragment instance");
    return new QuotesFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mQuoteAdapter = new QuoteAdapter(getActivity(), new ArrayList<Quote>());
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

    if(mQuoteAdapter != null) mQuoteAdapter.notifyDataSetChanged();

    populateFieldsDeferred();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.quotes_fragment, container, false);
    ButterKnife.inject(this, rootView);
    return rootView;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "onViewCreated()");
    mAddQuoteButton.setEnabled(false);
    populateFieldsDeferred();
  }

  private void populateFieldsDeferred() {
    if(mBook == null || getView() == null) {
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
        Intent editQuoteIntent = new Intent(getActivity(), AddQuoteActivity.class);
        editQuoteIntent.putExtra(AddBookActivity.KEY_QUOTE_ID, quote.getId());
        editQuoteIntent.putExtra(BookActivity.KEY_BOOK_ID, mBook.getId());
        startActivityForResult(editQuoteIntent, REQ_EDIT_QUOTE);
      }
    });

    repopulateAdapter();
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
  }

  private void repopulateAdapter() {
    mQuoteAdapter.clear();
    for(Quote quote : mBook.getQuotes()) {
      mQuoteAdapter.add(quote);
    }
    mQuoteAdapter.sortQuotes();
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
    final boolean addedQuote = requestCode == REQ_ADD_QUOTE && resultCode == Activity.RESULT_OK;
    final boolean editedQuote = requestCode == REQ_EDIT_QUOTE && resultCode != Activity.RESULT_CANCELED;
    if(addedQuote || editedQuote) {
      if(data != null && data.hasExtra(AddQuoteActivity.KEY_QUOTE_ID)) {
        final int quoteId = data.getIntExtra(AddQuoteActivity.KEY_QUOTE_ID, 0);
        Log.d(TAG, "Quote created: " + quoteId);
        if(resultCode == AddQuoteActivity.RESULT_DELETED) {
          new DeleteTask(quoteId, this).execute();
        } else {
          loadQuoteAndUpdateList(quoteId);
        }
      }
    }
  }

  private void loadQuoteAndUpdateList(int quoteId) {
    // Load the quote on the main thread since it's single row, and it quickly
    // gets very complicated to sync this new quote with the book loading event
    Quote quote = getDatabaseManager().get(Quote.class, quoteId);
    if(quote == null) {
      Log.w(TAG, "No such quote: " + quoteId);
      return;
    }

    if(mBook != null) {
      List<Quote> quotesInLoadedBook = mBook.getQuotes();
      int quoteIndex = findQuoteInLoadedBooks(quote);
      if(quoteIndex < 0) {
        Log.d(TAG, "Book does not contain new quote, adding");
        quotesInLoadedBook.add(quote);
      } else {
        Log.d(TAG, "Book already contains quote, updating");
        quotesInLoadedBook.remove(quoteIndex);
        quotesInLoadedBook.add(quoteIndex, quote);
      }

      repopulateAdapter();
    } else {
      Log.d(TAG, "Setting pending quote: " + quote);
      mPendingNewQuote = quote;
    }
  }

  private int findQuoteInLoadedBooks(Quote quote) {
    if(mBook == null || mBook.getQuotes().isEmpty()) {
      return -1;
    }

    List<Quote> quotes = mBook.getQuotes();
    for(int i = 0; i < quotes.size(); i++) {
      if(quotes.get(i).getId() == quote.getId()) {
        return i;
      }
    }

    return -1;
  }

  private void refreshBlankState() {
    final boolean hasItems = mQuoteAdapter.getCount() > 0;
    mQuoteList.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    mTextBlankState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
  }

  private void onQuoteDeleted(Quote quote) {
    if(quote == null) {
      Toast.makeText(getActivity(), R.string.quote_error_failed_to_delete, Toast.LENGTH_SHORT).show();
    } else {
      if(mQuoteAdapter != null) mQuoteAdapter.remove(quote);
      if(mBook != null) mBook.getQuotes().remove(quote);
      refreshBlankState();
    }
  }

  private static class DeleteTask extends AsyncTask<Void, Void, Quote> {
    private final int mQuoteId;
    private final WeakReference<QuotesFragment> mFragment;
    private final DatabaseManager mDatabaseManager;

    public DeleteTask(int quoteId, QuotesFragment fragment) {
      mQuoteId = quoteId;
      mFragment = new WeakReference<QuotesFragment>(fragment);
      mDatabaseManager = fragment.getDatabaseManager();
    }

    @Override
    protected Quote doInBackground(Void... voids) {
      Quote quote = mDatabaseManager.get(Quote.class, mQuoteId);
      return mDatabaseManager.delete(quote) ? quote : null;
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
