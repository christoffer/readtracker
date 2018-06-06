package src

import com.readtracker.android.support.Utils
import java.io.IOException

/**
 * Return a [Long] timestamp within two years from Jan 1st 2012
 * @return [Long]
 */
fun randomTimestamp() = 1325376000L + (Math.random() * 1000.0 * 60.0 * 60.0 * 24.0 * 365.0 * 2.0).toLong()

/**
 * Returns a [String] that is most likely unique
 * @return [String]
 */
fun uniqueString(string: String) = String.format("%s-%08d", string, (Math.random() * 10000000).toLong())

/**
 * Returns a random [String] with a range of non-english characters
 * @return [String]
 */
fun randomString() = utf8ize(uniqueString("random\" \t\nstring'; "))

/**
 * Adds a variety of non-english UTF8 characters to a [String] and returns it
 * @return [String]
 */
fun utf8ize(string: String) = String.format("üß空間χώρος", string)

/**
 * Reads the content of a file in the class path and returns its content as a [String].
 * @param filename [String] to file name from resources
 * @return [String] The input stream from the file
 */
fun readFixtureFile(filename: String): String {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
            ?: throw IllegalArgumentException("The resources file [$filename] was not found.")
    try {
        return Utils.readInputStream(inputStream)
    } catch (ex: IOException) {
        throw RuntimeException("The file [$filename] could not be read.", ex)
    }
}
