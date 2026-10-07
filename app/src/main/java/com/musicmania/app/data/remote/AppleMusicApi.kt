package com.musicmania.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class AppleTrackDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val trackId: Long? = null,
    val artistId: Long? = null,
    val collectionId: Long? = null,
    val artistName: String = "Unknown artist",
    val collectionName: String? = null,
    val trackName: String? = null,
    val previewUrl: String? = null,
    val artworkUrl100: String? = null,
    val artworkUrl60: String? = null,
    val trackTimeMillis: Long? = null,
    val primaryGenreName: String? = null,
    val trackViewUrl: String? = null,
    val collectionViewUrl: String? = null,
    val explicitness: String? = null,
    @SerialName("trackExplicitness") val trackExplicitness: String? = null,
)

@Serializable
data class AppleSearchResponse(val resultCount: Int = 0, val results: List<AppleTrackDto> = emptyList())

interface AppleMusicApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("country") country: String = "IN",
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 30,
        @Query("explicit") explicit: String = "No",
    ): AppleSearchResponse

    @GET("lookup")
    suspend fun lookupAlbum(
        @Query("id") collectionId: Long,
        @Query("entity") entity: String = "song",
        @Query("country") country: String = "IN",
        @Query("limit") limit: Int = 100,
    ): AppleSearchResponse

    @GET("lookup")
    suspend fun lookupArtistSongs(
        @Query("id") artistId: Long,
        @Query("entity") entity: String = "song",
        @Query("country") country: String = "IN",
        @Query("limit") limit: Int = 100,
    ): AppleSearchResponse
}

object AppleNetwork {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val api: AppleMusicApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AppleMusicApi::class.java)
    }
}
