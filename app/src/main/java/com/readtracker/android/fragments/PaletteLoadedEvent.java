package com.readtracker.android.fragments;

import android.support.v7.graphics.Palette;

import com.readtracker.android.db.Book;

public class PaletteLoadedEvent {
  private final Book mBook;
  private final Palette mPalette;

  public PaletteLoadedEvent(Book book, Palette palette) {
    mBook = book;
    mPalette = palette;
  }

  public Book getBook() {
    return mBook;
  }

  public Palette getPalette() {
    return mPalette;
  }
}
