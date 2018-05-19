package com.readtracker.android.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class QuoteTest {

    /**
     * Create a [Book] entity and set a [Quote] on it. Merge this information
     * into another book entity and assert the information.
     */
    @Test
    fun quoteTest_MergeInformationFromBookToBook_ReturnsBookCopy() {
        val book = Book()

        val original = Quote().apply {
            content = "I have content"
            addTimestampMs = 123456789L
            position = 0.45f
            this@apply.book = book
        }

        val merged = Quote().apply {
            merge(original)
        }

        assertEquals("I have content", merged.content)
        assertEquals(123456789L, merged.addTimestampMs)
        assertEquals(0.45f, merged.position)
        assertSame(book, merged.book)
    }
}