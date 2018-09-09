package com.readtracker.android.adapters

/**
 * Simple display of a book with title, author and cover.
 */
open class BookItem(
    open val title: String,
    open val author: String,
    open val coverUrl: String,
    open val pageCount: Long
)
