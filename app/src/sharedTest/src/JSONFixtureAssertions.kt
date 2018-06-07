package src

import com.readtracker.android.db.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.util.*

/**
 * To keep the amount of data sane, and to enable cross test-comparisons, all the rich
 * examples share the same data.
 *
 *
 * This class is a way to assert that the data is consistent with how the book object should
 * look after being imported.
 */
object JSONFixtureAssertions {

    /**
     * Asserts that all data in the example used for version 2 import/export is consistent
     */
    fun assertBookListMatchesExpectedResultFromFixtureImport(books: List<Book>) {
        assertEquals(2, books.size)
        books[0].let { metamorphosis ->
            assertEquals("Metamorphosis", metamorphosis.title)
            assertEquals("Franz Kafka", metamorphosis.author)
            assertEquals(Book.State.Reading, metamorphosis.state)
            assertEquals(0.00872093066573143f, metamorphosis.currentPosition)
            assertEquals(java.lang.Long.valueOf(213456789L), metamorphosis.currentPositionTimestampMs)
            assertEquals(java.lang.Long.valueOf(123456789L), metamorphosis.firstPositionTimestampMs)
            assertEquals(123.45f, metamorphosis.pageCount)
            assertNull(metamorphosis.coverImageUrl)

            assertEquals(2, metamorphosis.sessions.size)
            metamorphosis.sessions[0].let { sessionOne ->
                assertEquals(1399535743000L, sessionOne.timestampMs)
                assertEquals(0.00872093066573143f, sessionOne.endPosition)
                assertEquals(0f, sessionOne.startPosition)
                assertEquals(3, sessionOne.durationSeconds)
            }

            metamorphosis.sessions[1].let { sessionTwo ->
                assertEquals(1400856553800L, sessionTwo.timestampMs)
                assertEquals(0.5f, sessionTwo.endPosition)
                assertEquals(0.008720931f, sessionTwo.startPosition)
                assertEquals(1146, sessionTwo.durationSeconds)
            }

            assertEquals(2, metamorphosis.quotes.size)
            metamorphosis.quotes[0].let { quoteOne ->
                assertEquals("unicorn", quoteOne.content)
                assertEquals(0.45f, quoteOne.position)
                assertEquals(java.lang.Long.valueOf(123456799L), quoteOne.addTimestampMs)
            }
            metamorphosis.quotes[1].let { quoteTwo ->
                assertEquals("一角獣", quoteTwo.content)
                assertEquals(0f, quoteTwo.position)
                assertEquals(java.lang.Long.valueOf(4564321L), quoteTwo.addTimestampMs)
            }
        }

        books[1].let { androidForDummies ->
            assertEquals("Android Apps Entwicklung für Dummies", androidForDummies.title)
            assertEquals("Donn Felker", androidForDummies.author)
            assertEquals(Book.State.Finished, androidForDummies.state)
            assertEquals(1f, androidForDummies.currentPosition)
            assertEquals(java.lang.Long.valueOf(1400856553800L), androidForDummies.currentPositionTimestampMs)
            assertEquals(344.0f, androidForDummies.pageCount)
            assertEquals("http://bks8.books.google.de/books?id=KPjmuog", androidForDummies.coverImageUrl)
            assertEquals(0, androidForDummies.quotes.size)
            assertEquals(0, androidForDummies.sessions.size)
        }
    }

    /**
     * Asserts that a version 2 imported book is consistent with what's expected when fields
     * are null or missing.
     */
    fun assertBookMatchesExpectedResultFromNullFieldsFixture(book: Book) {
        assertEquals("", book.title)
        assertEquals("", book.author)
        assertEquals(Book.State.Unknown, book.state)
        assertEquals(0f, book.currentPosition)
        assertNull(book.currentPositionTimestampMs)
        assertNull(book.firstPositionTimestampMs)
        assertNull(book.pageCount)
        assertNull(book.coverImageUrl)

        assertEquals(1, book.quotes.size)
        book.quotes[0].let { quote ->
            assertNull(quote.content)
            assertNull(quote.position)
            assertNull(quote.addTimestampMs)
        }

        assertEquals(1, book.sessions.size)
        book.sessions[0].let { session ->
            assertEquals(0, session.durationSeconds)
            assertEquals(0f, session.startPosition)
            assertEquals(0f, session.endPosition)
            assertEquals(0, session.timestampMs)
        }
    }

    /**
     * Helper method to assemble a list of [Book]s with known data.
     * @return [List] of [Book] entities with data, [Quote]s and [Session]s.
     */
    fun populateBooksForExpectedOutput(databaseManager: DatabaseManager): List<Book> {
        val metamorphosis = Book().apply {
            title = "Metamorphosis"
            author = "Franz Kafka"
            closingRemark = "Dude, poor guy!"
            coverImageUrl = "https://images-of-books/metamorphosis.png"
            currentPosition = 0.5f
            currentPositionTimestampMs = 213456789L
            firstPositionTimestampMs = 123456789L
            pageCount = 123.45f
            state = Book.State.Finished
        }
        databaseManager.save(metamorphosis)

        buildQuote(metamorphosis, "unicorn", 0.45f, 123456799L).let { unicornQuote ->
            metamorphosis.quotes.add(unicornQuote)
            databaseManager.save(unicornQuote)
        }

        buildQuote(metamorphosis, "guppy", 0f, 4564321L).let { guppyQuote ->
            metamorphosis.quotes.add(guppyQuote)
            databaseManager.save(guppyQuote)
        }

        buildSession(metamorphosis, 0.45f, 0.75f, 1234, 4000000L).let { firstSession ->
            metamorphosis.sessions.add(firstSession)
            databaseManager.save(firstSession)
        }

        buildSession(metamorphosis, 0.75f, 1.0f, 600, 5000000L).let { secondSession ->
            metamorphosis.sessions.add(secondSession)
            databaseManager.save(secondSession)
        }

        val androidFurDummies = Book().apply {
            title = "Android Apps Entwicklung für Dummies"
            author = "Donn Felker"
            state = Book.State.Finished
            currentPosition = 0.00872093066573143f
            currentPositionTimestampMs = 1400856553800L
            pageCount = 344f
            coverImageUrl = "http://bks8.books.google.de/books?id=KPjmuog"
        }

        databaseManager.save(androidFurDummies)

        buildSession(androidFurDummies, 0f, 0.00872093066573143f, 3, 1399535743000L).let { session ->
            androidFurDummies.sessions.add(session)
            databaseManager.save(session)
        }

        buildSession(androidFurDummies, 0.008720931f, 0.9800000190734863f, 1146, 1400856553800L).let { session ->
            androidFurDummies.sessions.add(session)
            databaseManager.save(session)
        }

        val emptyBook = Book()
        databaseManager.save(emptyBook)

        return Arrays.asList(metamorphosis, androidFurDummies, emptyBook)
    }
}
