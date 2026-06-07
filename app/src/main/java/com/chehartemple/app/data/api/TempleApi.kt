package com.chehartemple.app.data.api

import com.chehartemple.app.data.model.*
import retrofit2.http.*

interface TempleApi {
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): MessageResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: Map<String, String>): MessageResponse

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body request: Map<String, String>): MessageResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): MessageResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): TokenResponse

    @POST("auth/google")
    suspend fun googleLogin(@Body body: Map<String, @JvmSuppressWildcards Any>): AuthResponse

    @GET("events")
    suspend fun getEvents(): List<Event>

    @GET("events/home")
    suspend fun getHomeEvents(): List<Event>

    @GET("events/limited")
    suspend fun getLimitedEvents(): LimitedEvents

    @GET("events/categorized")
    suspend fun getEventsCategorized(): CategorizedEvents

    @GET("news")
    suspend fun getNews(): List<News>

    @GET("gallery")
    suspend fun getGallery(): List<GalleryItem>

    @GET("gallery/paged")
    suspend fun getPagedGallery(
        @Query("page") page: Int,
        @Query("size") size: Int = 3,
        @Query("type") type: String = "ALL"
    ): PagedGallery

    @GET("facebook/videos")
    suspend fun getFacebookVideos(
        @Query("limit") limit: Int = 3,
        @Query("offset") offset: Int = 0
    ): FacebookVideoResponse

    @GET("facebook/reels")
    suspend fun getFacebookReels(
        @Query("limit") limit: Int = 3,
        @Query("offset") offset: Int = 0
    ): FacebookVideoResponse

    @GET("facebook/posts")
    suspend fun getFacebookPosts(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): FacebookPostResponse

    @GET("facebook/photos")
    suspend fun getFacebookPhotos(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): FacebookPostResponse

    @GET("facebook/latest")
    suspend fun getFacebookLatestVideo(): LiveStream

    @GET("instagram/reels")
    suspend fun getInstagramReels(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): InstagramApiResponse

    @GET("instagram/images")
    suspend fun getInstagramImages(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): InstagramApiResponse

    @GET("temple-timings")
    suspend fun getTempleTimings(): List<TempleTiming>

    @GET("live-stream")
    suspend fun getLiveStream(): LiveStream

    @GET("sponsors/active")
    suspend fun getActiveSponsors(): Map<String, Any>

    @POST("sponsors/{id}/click")
    suspend fun recordSponsorClick(@Path("id") id: Long): Unit

    @GET("config/social-media")
    suspend fun getSocialMedia(): SocialMedia

    @GET("config/contact-info")
    suspend fun getContactInfo(): ContactInfo

    @GET("token/validate")
    suspend fun validateToken(): Map<String, String>

    @POST("activity/track")
    suspend fun trackActivity(@Body body: Map<String, String>): Unit
}
