package com.readtracker.android.adapters;

import android.net.Uri;

import com.readtracker.android.db.LocalHighlight;

import java.util.Date;

/**
 * Shows a local highlight
 */
public class HighlightItem {
  private String content;
  private String permalinkUrl;
  private int highlightId;
  private Date highlightedAt;
  private int likeCount;
  private int commentCount;
  private LocalHighlight localHighlight;

  public HighlightItem(LocalHighlight highlight) {
    content = highlight.content;
    permalinkUrl = highlight.readmillPermalinkUrl;
    highlightId = highlight.id;
    highlightedAt = highlight.highlightedAt;
    likeCount = highlight.likeCount;
    commentCount = highlight.commentCount;
    localHighlight = highlight;
  }

  public String getContent() {
    return content;
  }

  public LocalHighlight getLocalHighlight() {
    return localHighlight;
  }

  public int getId() {
    return highlightId;
  }

  public int getLikeCount() {
    return likeCount;
  }

  public int getCommentCount() {
    return commentCount;
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
