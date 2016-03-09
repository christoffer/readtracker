package com.readtracker.android.db;

import android.test.AndroidTestCase;

import org.junit.Test;

public class BookTest extends AndroidTestCase {

  @Test
  public void bookTest_MergeInformationBookToBook_ReturnsBookCopy() throws Exception {
    Book original = new Book();
    original.setTitle("Metamorphosis");
    original.setAuthor("Franz Kafka");
    original.setCoverImageUrl("https://example.com/image.png");
    original.setPageCount(344.0f);
    original.setState(Book.State.Reading);
    original.setCurrentPosition(0.45f);
    original.setCurrentPositionTimestampMs(1400856553800L);
    original.setFirstPositionTimestampMs(1200856553800L);
    original.setClosingRemark("Finito");

    Book merge = new Book();
    merge.merge(original);

    assertEquals("Metamorphosis", merge.getTitle());
    assertEquals("Franz Kafka", merge.getAuthor());
    assertEquals("https://example.com/image.png", merge.getCoverImageUrl());
    assertEquals(344.0f, merge.getPageCount());
    assertEquals(Book.State.Reading, merge.getState());
    assertEquals(0.45f, merge.getCurrentPosition());
    assertEquals(Long.valueOf(1400856553800L), merge.getCurrentPositionTimestampMs());
    assertEquals(Long.valueOf(1200856553800L), merge.getFirstPositionTimestampMs());
    assertEquals("Finito", merge.getClosingRemark());
  }
}