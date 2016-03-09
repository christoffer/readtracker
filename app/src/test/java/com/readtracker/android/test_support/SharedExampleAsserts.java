package com.readtracker.android.test_support;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Quote;
import com.readtracker.android.db.Session;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * To keep the amount of data sane, and to enable cross test-comparisons, all the rich
 * examples share the same data.
 * <p/>
 * This class is a way to assert that the data is consistent with how the book object should
 * look after being imported.
 */
public class SharedExampleAsserts {

  /** Asserts that all data in the example used for version 2 import/export is consistent */
  public static void assertExampleBooksVersion2(List<Book> books) {
    assertEquals(2, books.size());

    Book metamorphosis = books.get(0);

    assertEquals("Metamorphosis", metamorphosis.getTitle());
    assertEquals("Franz Kafka", metamorphosis.getAuthor());
    assertEquals(Book.State.Reading, metamorphosis.getState());
    assertEquals(0.00872093066573143f, metamorphosis.getCurrentPosition());
    assertEquals(Long.valueOf(213456789L), metamorphosis.getCurrentPositionTimestampMs());
    assertEquals(Long.valueOf(123456789L), metamorphosis.getFirstPositionTimestampMs());
    assertEquals(123.45f, metamorphosis.getPageCount());
    assertNull(metamorphosis.getCoverImageUrl());

    assertEquals(2, metamorphosis.getSessions().size());

    Session sessionOne = metamorphosis.getSessions().get(0);
    assertEquals(1399535743000L, sessionOne.getTimestampMs());
    assertEquals(0.00872093066573143f, sessionOne.getEndPosition());
    assertEquals(0f, sessionOne.getStartPosition());
    assertEquals(3, sessionOne.getDurationSeconds());

    Session sessionTwo = metamorphosis.getSessions().get(1);
    assertEquals(1400856553800L, sessionTwo.getTimestampMs());
    assertEquals(0.5f, sessionTwo.getEndPosition());
    assertEquals(0.008720931f, sessionTwo.getStartPosition());
    assertEquals(1146, sessionTwo.getDurationSeconds());

    assertEquals(2, metamorphosis.getQuotes().size());

    Quote quoteOne = metamorphosis.getQuotes().get(0);
    assertEquals("unicorn", quoteOne.getContent());
    assertEquals(0.45f, quoteOne.getPosition());
    assertEquals(Long.valueOf(123456799L), quoteOne.getAddTimestampMs());

    Quote quoteTwo = metamorphosis.getQuotes().get(1);
    assertEquals("一角獣", quoteTwo.getContent());
    assertEquals(0f, quoteTwo.getPosition());
    assertEquals(Long.valueOf(4564321L), quoteTwo.getAddTimestampMs());

    Book androidForDummies = books.get(1);

    assertEquals("Android Apps Entwicklung für Dummies", androidForDummies.getTitle());
    assertEquals("Donn Felker", androidForDummies.getAuthor());
    assertEquals(Book.State.Finished, androidForDummies.getState());
    assertEquals(1f, androidForDummies.getCurrentPosition());
    assertEquals(Long.valueOf(1400856553800L), androidForDummies.getCurrentPositionTimestampMs());
    assertEquals(344.0f, androidForDummies.getPageCount());
    assertEquals("http://bks8.books.google.de/books?id=KPjmuog", androidForDummies.getCoverImageUrl());

    assertEquals(0, androidForDummies.getQuotes().size());
    assertEquals(0, androidForDummies.getSessions().size());
  }

  /**
   * Asserts that a version 2 imported book is consistent with what's expected when fields
   * are null or missing.
   */
  public static void assertNullBookVersion2(Book book) {
    assertEquals("", book.getTitle());
    assertEquals("", book.getAuthor());
    assertEquals(Book.State.Unknown, book.getState());
    assertEquals(0f, book.getCurrentPosition());
    assertNull(book.getCurrentPositionTimestampMs());
    assertNull(book.getFirstPositionTimestampMs());
    assertNull(book.getPageCount());
    assertNull(book.getCoverImageUrl());

    assertEquals(1, book.getQuotes().size());
    Quote quote = book.getQuotes().get(0);

    assertNull(quote.getContent());
    assertNull(quote.getPosition());
    assertNull(quote.getAddTimestampMs());

    assertEquals(1, book.getSessions().size());
    Session session = book.getSessions().get(0);

    assertEquals(0, session.getDurationSeconds());
    assertEquals(0f, session.getStartPosition());
    assertEquals(0f, session.getEndPosition());
    assertEquals(0, session.getTimestampMs());
  }
}
