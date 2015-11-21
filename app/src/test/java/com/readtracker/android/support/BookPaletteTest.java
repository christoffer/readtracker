package com.readtracker.android.support;

import com.readtracker.android.test_support.JsonBuilder;

import junit.framework.TestCase;

public class BookPaletteTest extends TestCase {
  final String TEST_BOOK_JSON = new JsonBuilder()
      .add("muted", 0xff111111)
      .add("dark_muted", 0xff222222)
      .add("vibrant", 0xff333333)
      .build().toString();

  /** Make sure that we can create a palette to and from a JSON string properly. */
  public void test_JSONSerialization() throws Exception {
    // Test both the serialization and the deserialization at once by turning the deserialized
    // string from a book converted from JSON into a new book.
    BookPalette bookPalette = new BookPalette(new BookPalette(TEST_BOOK_JSON).toJSON());
    assertEquals(0xff111111, bookPalette.getMutedColor());
    assertEquals(0xff222222, bookPalette.getDarkMutedColor());
    assertEquals(0xff333333, bookPalette.getVibrantColor());
  }

  public void test_defaultConstructor() throws Exception {
    BookPalette bookPalette = new BookPalette();
    assertEquals(BookPalette.DEFAULT_MUTED_COLOR, bookPalette.getMutedColor());
    assertEquals(BookPalette.DEFAULT_DARK_MUTED_COLOR, bookPalette.getDarkMutedColor());
    assertEquals(BookPalette.DEFAULT_VIBRANT_COLOR, bookPalette.getVibrantColor());
  }

  public void test_Equals() throws Exception {
    // Defaults should equal
    assertTrue(new BookPalette().equals(new BookPalette()));

    // Should differ when single color differs
    BookPalette a = new BookPalette();
    BookPalette b = new BookPalette();
    a.setMutedColor(0xff001100);
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
    a.setMutedColor(b.getMutedColor());

    a.setDarkMutedColor(0xff001100);
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
    a.setDarkMutedColor(b.getDarkMutedColor());

    a.setVibrantColor(0xff001100);
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));

    BookPalette c = new BookPalette(TEST_BOOK_JSON);
    a.setMutedColor(c.getMutedColor());
    a.setDarkMutedColor(c.getDarkMutedColor());
    a.setVibrantColor(c.getVibrantColor());

    assertTrue(a.equals(c));
    assertTrue(c.equals(a));

    //noinspection EqualsBetweenInconvertibleTypes
    assertFalse(a.equals("string"));
  }
}
