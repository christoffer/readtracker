package src

import com.readtracker.android.db.Book
import com.readtracker.android.db.Quote
import com.readtracker.android.db.Session

/**
 * Build a book based on title author and page count
 * @param title String
 * @param author String
 * @param pageCount String
 * @return [Book] entity
 */
fun buildBook(title: String, author: String, pageCount: Float) =
        Book().apply {
            this@apply.title = title
            this@apply.author = author
            this@apply.pageCount = pageCount
        }


/**
 * @return [Quote] entity
 */
fun buildQuote(book: Book, content: String, position: Float, timestamp: Long) =
        Quote().apply {
            this@apply.book = book
            this@apply.position = position
            this@apply.content = content
            this@apply.addTimestampMs = timestamp
        }


/**
 * @return [Session] entity
 */
fun buildSession(book: Book, startPos: Float, endPos: Float, durationSeconds: Int, timestamp: Long) =
        Session().apply {
            this@apply.book = book
            this@apply.startPosition = startPos
            this@apply.endPosition = endPos
            this@apply.durationSeconds = durationSeconds.toLong()
            this@apply.timestampMs = timestamp
        }

/**
 * Returns a [Book] with a random title and author
 * @return [Book] entity
 */
fun buildRandomBook(): Book {
    val book = Book()
    book.title = randomString()
    book.author = randomString()

    book.state = if (Math.random() < 0.5) {
        if (Math.random() < 0.5) book.closingRemark = randomString()
        Book.State.Finished
    } else {
        Book.State.Reading
    }

    var numSessions = (Math.random() * 10).toInt()
    if (Math.random() < 0.15) {
        // Spawn a seldom, but bigger sample
        numSessions += (Math.random() * 100).toInt()
    }
    var numQuotes = (Math.random() * 5).toInt()
    if (Math.random() < 0.15) {
        // Spawn a seldom, but bigger sample
        numQuotes += (Math.random() * 100).toInt()
    }

    for (i in 0 until numSessions) {
        book.sessions.add(buildRandomSession(book))
    }
    for (i in 0 until numQuotes) {
        book.quotes.add(buildRandomQuote(book))
    }

    return book
}

/**
 * Returns a [Quote] with randomized values
 * @return [Quote] entity
 */
fun buildRandomQuote(book: Book?) =
        buildQuote(
                book = book ?: buildRandomBook(),
                content = randomString(),
                position = Math.random().toFloat(),
                timestamp = randomTimestamp()
        )

/**
 * Returns a [Session] with randomized values
 * @return [Session] entity
 */
fun buildRandomSession(book: Book?): Session {
    val state = if (Math.random() < 0.5) Book.State.Reading else Book.State.Finished
    val positionStart = Math.random().toFloat()

    val positionEnd: Float
    if (state == Book.State.Finished) {
        positionEnd = 1.0f
    } else {
        positionEnd = positionStart + (Math.random() * (1.0f - positionStart)).toFloat()
    }

    return buildSession(
            book = book ?: buildRandomBook(),
            startPos = positionStart,
            endPos = positionEnd,
            durationSeconds = (Math.random() * 60.0 * 60.0 * 4.0).toInt(),
            timestamp = randomTimestamp()
    )
}
