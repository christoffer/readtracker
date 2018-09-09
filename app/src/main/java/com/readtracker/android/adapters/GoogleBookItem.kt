package com.readtracker.android.adapters

import com.readtracker.android.support.GoogleBooksSearchService

/**
 * Shows a GoogleBook search result
 */
data class GoogleBookItem(
    override val title: String,
    override val author: String,
    override val coverUrl: String,
    override val pageCount: Long
) : BookItem(title, author, coverUrl, pageCount)
