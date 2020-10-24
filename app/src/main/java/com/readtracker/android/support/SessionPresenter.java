package com.readtracker.android.support;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;

public class SessionPresenter {
  public enum PositionMode {
    PAGES,
    PERCENT
  }

  public static PositionPresenter getPresenterForSession(Session session) {
    final Book book = session.getBook();
    if(book != null && book.hasPageNumbers()) {
      return new PagePresenter(book.getPageCount());
    }
    return new PercentPresenter();
  }

  public interface PositionPresenter {
    String format(float position);

    float parse(String formattedPosition);

    PositionMode getMode();
  }

  public static class PagePresenter implements PositionPresenter {
    final float mPageCount;

    public PagePresenter(float pageCount) {
      mPageCount = pageCount;
    }

    @Override
    public String format(float position) {
      position = Math.max(Math.min(1.0f, position), 0.0f);
      return String.format("%d", Math.round(position * mPageCount));
    }

    @Override
    public float parse(String formattedPosition) {
      try {
        final float pageNumber = Float.parseFloat(formattedPosition);
        return pageNumber <= mPageCount && pageNumber >= 0 ? (pageNumber / mPageCount) : -1f;
      } catch(NumberFormatException ex) {
        return -1f;
      }
    }

    @Override
    public PositionMode getMode() {
      return PositionMode.PAGES;
    }

    public float getMaxBoundary() {
      return mPageCount;
    }
  }

  public static class PercentPresenter implements PositionPresenter {
    @Override
    public String format(float position) {
      position = Math.max(Math.min(1.0f, position), 0.0f);
      return String.format("%.1f", position * 100.0f);
    }

    @Override
    public float parse(String formattedPosition) {
      try {
        final float percentPos = Float.parseFloat(formattedPosition);
        return percentPos >= 0 && percentPos <= 100 ? (percentPos / 100f) : -1f;
      } catch(NumberFormatException ex) {
        return -1f;
      }
    }

    @Override
    public PositionMode getMode() {
      return PositionMode.PERCENT;
    }
  }
}
