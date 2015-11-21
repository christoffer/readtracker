package com.readtracker.android.test_support;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;
import com.readtracker.android.support.Utils;

import java.io.IOException;
import java.io.InputStream;

public class TestUtils {

  /** Return a timestamp within two years from Jan 1st 2012 */
  public static long randomTimestamp() {
    return 1325376000L + (long) (Math.random() * 1000 * 60 * 60 * 24 * 365 * 2);
  }

  /** Returns a string that is most likely unique */
  private static String uniqueString(String string) {
    final long number = (long) (Math.random() * 10000000);
    return String.format("%s-%08d", string, number);
  }

  /** Returns a random string with a range of non-english characters */
  public static String randomString() {
    return utf8ize(uniqueString("random\" \t\nstring'; "));
  }

  /** Adds a variety of non-english UTF8 characters to a string. */
  public static String utf8ize(String string) {
    return String.format("üß空間%sχώρος", string);
  }

  /** Returns a Book with a random title and author. */
  public static Book buildRandomBook() {
    Book book = new Book();
    book.setTitle(randomString());
    book.setAuthor(randomString());

    Book.State state;
    if(Math.random() < 0.5) {
      state = Book.State.Finished;
      if(Math.random() < 0.5) book.setClosingRemark(randomString());
    } else {
      state = Book.State.Reading;
    }
    book.setState(state);

    int numSessions = (int) (Math.random() * 10);
    if(Math.random() < 0.15) {
      // Spawn a seldom, but bigger sample
      numSessions += Math.random() * 100;
    }
    int numQuotes = (int) (Math.random() * 5);
    if(Math.random() < 0.15) {
      // Spawn a seldom, but bigger sample
      numQuotes += Math.random() * 100;
    }

    for(int i = 0; i < numSessions; i++) {
      book.getSessions().add(TestUtils.buildRandomSession(book));
    }

    for(int i = 0; i < numQuotes; i++) {
      book.getQuotes().add(TestUtils.buildRandomQuote(book));
    }

    return book;
  }

  /** Returns a Quote with randomized values. */
  public static Quote buildRandomQuote(Book book) {
    book = book == null ? buildRandomBook() : book;
    float position = (float) Math.random();
    long timestamp = randomTimestamp();
    return buildQuote(book, randomString(), position, timestamp);
  }

  /** Returns a Session with randomized values. */
  public static Session buildRandomSession(Book book) {
    book = book == null ? buildRandomBook() : book;
    long timestamp = randomTimestamp();
    Book.State state;
    state = Math.random() < 0.5 ? Book.State.Reading : Book.State.Finished;
    float positionStart = (float) Math.random();

    float positionEnd;
    if(state == Book.State.Finished) {
      positionEnd = 1.0f;
    } else {
      positionEnd = positionStart + (float) (Math.random() * (1.0f - positionStart));
    }

    int durationSeconds = (int) (Math.random() * 60 * 60 * 4);

    return buildSession(book, positionStart, positionEnd, durationSeconds, timestamp);
  }

  /** Returns a Quote */
  public static Quote buildQuote(Book book, String content, float position, long timestamp) {
    Quote quote = new Quote();
    quote.setBook(book);
    quote.setPosition(position);
    quote.setContent(content);
    quote.setAddTimestampMs(timestamp);
    return quote;
  }

  /** Returns a Session */
  public static Session buildSession(Book book, float startPos, float endPos, int durationSeconds, long timestamp) {
    Session session = new Session();
    session.setBook(book);
    session.setStartPosition(startPos);
    session.setEndPosition(endPos);
    session.setDurationSeconds(durationSeconds);
    session.setTimestampMs(timestamp);
    return session;
  }

  /**
   * Reads the content of a file in the class path and returns its content as a String.
   */
  public static String readFixtureFile(String filename) {
    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    if(inputStream == null) {
      throw new IllegalArgumentException("The resources file [" + filename + "] was not found.");
    }
    try {
      return Utils.readInputStream(inputStream);
    } catch(IOException ex) {
      throw new RuntimeException("The file [" + filename + "] could not be read.", ex);
    }
  }

  public static Book buildBook(String title, String author, float pageCount) {
    Book book = new Book();
    book.setTitle(title);
    book.setAuthor(author);
    book.setPageCount(pageCount);
    return book;
  }
}
