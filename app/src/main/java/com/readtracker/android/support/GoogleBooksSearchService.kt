package com.readtracker.android.support

import android.support.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.readtracker.android.adapters.GoogleBookItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

class GoogleBooksSearchService {
    @Keep
    data class GoogleBooksResponse(
        @SerializedName("totalItems") val totalItems: Int,
        @SerializedName("items") val items: List<GoogleVolumeDto>
    )

    @Keep
    data class GoogleBookDto(
        @SerializedName("id") val id: String = UUID.randomUUID().toString(),
        @SerializedName("title") val title: String?,
        @SerializedName("authors") val author: List<String>?,
        @SerializedName("imageLinks") val coverUrl: GoogleCoverImageDto?,
        @SerializedName("pageCount") val pageCount: Long?
    ) {

        override fun toString(): String {
            return String.format(
                "Id: %s, Title: %s, Author: %s, Cover: %s, Page count: %d",
                id, title, author, coverUrl, pageCount
            )
        }

        fun isValid(): Boolean = title.isNullOrBlank().not() &&
                author != null && author.isNotEmpty() &&
                coverUrl != null &&
                pageCount != null && pageCount > 0

        fun convertToItem(): GoogleBookItem? {
            return if (this.isValid()) {
                return GoogleBookItem(title!!, author!![0], coverUrl!!.thumbnail, pageCount!!)
            } else null
        }
    }

    @Keep
    data class GoogleVolumeDto(
        @SerializedName("id") val id: String = UUID.randomUUID().toString(),
        @SerializedName("volumeInfo") val book: GoogleBookDto
    )

    @Keep
    data class GoogleCoverImageDto(
        @SerializedName("thumbnail") val thumbnail: String,
        @SerializedName("small") val small: String
    )

    interface GoogleBookSearchApi {
        companion object {
            const val BASE_URL = "https://www.googleapis.com/"
        }

        @GET("books/v1/volumes")
        fun getVolumes(@Query("q") queryParam: String): Call<GoogleBooksResponse>
    }
}