package com.readtracker;

import android.net.Uri;
import com.readtracker.db.LocalHighlight;

import java.util.Date;

/**
 * Shows a local highlight
 */
public class ListItemHighlight {
  private String content;
  private String permalinkUrl;
  private int highlightId;
  private Date highlightedAt;

  public ListItemHighlight(LocalHighlight highlight) {
    content = highlight.content;
    permalinkUrl = highlight.readmillPermalinkUrl;
    highlightId = highlight.id;
    highlightedAt = highlight.highlightedAt;
  }

  public String getContent() {
    return content;
  }

  public int getId() {
    return highlightId;
  }

  public int getLikeCount() {
    return (int) (Math.random() * 10);
  }

  public int getCommentCount() {
    return (int) (Math.random() * 10);
  }

  public Uri getPermalink() {
    try {
      return Uri.parse(permalinkUrl);
    } catch(Exception ignored) {
      return null;
    }
  }

  public Date getHighlightedAt() {
    return highlightedAt;
  }
}
