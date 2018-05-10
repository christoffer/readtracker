package com.readtracker.android.db

import android.support.test.filters.SmallTest
import com.readtracker.android.db.Book.State.Reading
import org.junit.Assert.assertEquals
import org.junit.Test

class BookTest {

    /**
     * Create a book entity with some set data, synchronize the data from one book onto another,
     * and assert that the data is correct.
     */
    @SmallTest
    @Test
    fun bookTest_MergeInformationBookToBook_ReturnsBookCopy() {
        val original = Book().apply {
            title = "Metamorphosis"
            author = "Franz Kafka"
            coverImageUrl = "https://example.com/image.png"
            pageCount = 344.0f
            state = Reading
            currentPosition = 0.45f
            currentPositionTimestampMs = 1400856553800L
            firstPositionTimestampMs = 1200856553800L
            closingRemark = "Finito"
        }

        val merge = Book().apply {
            merge(original)
        }

        assertEquals("Metamorphosis", merge.title)
        assertEquals("Franz Kafka", merge.author)
        assertEquals("https://example.com/image.png", merge.coverImageUrl)
        assertEquals(344.0f, merge.pageCount)
        assertEquals(0.45f, merge.currentPosition)
        assertEquals(Reading, merge.state)
        assertEquals(1400856553800L, merge.currentPositionTimestampMs)
        assertEquals(1200856553800L, merge.firstPositionTimestampMs)
        assertEquals("Finito", merge.closingRemark)
    }
}