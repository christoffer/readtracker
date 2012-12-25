package com.readtracker_beta.adapters;

import android.net.Uri;
import com.readtracker_beta.db.LocalHighlight;

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

  public HighlightItem(LocalHighlight highlight) {
    content = highlight.content;
    permalinkUrl = highlight.readmillPermalinkUrl;
    highlightId = highlight.id;
    highlightedAt = highlight.highlightedAt;
    likeCount = highlight.likeCount;
    commentCount = highlight.commentCount;
  }

  public String getContent() {
    return content;
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
